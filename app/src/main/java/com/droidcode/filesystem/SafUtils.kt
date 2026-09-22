package com.droidcode.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

object SafUtils {

    @Volatile
    private var appContext: Context? = null

    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences("saf_tree_mappings", Context.MODE_PRIVATE)

    @JvmStatic
    fun registerSafTree(context: Context, localPath: String, treeUri: Uri) {
        val prefs = getPrefs(context)
        prefs.edit().putString(localPath, treeUri.toString()).apply()
    }

    @JvmStatic
    fun getSafTreeUriForPath(context: Context, localPath: String): Uri? {
        val prefs = getPrefs(context)
        val all = prefs.all
        var bestMatchKey = ""
        var bestMatchUri: Uri? = null

        for ((projPath, uriStr) in all) {
            if (uriStr is String && (localPath == projPath || localPath.startsWith(projPath + File.separator))) {
                if (projPath.length > bestMatchKey.length) {
                    bestMatchKey = projPath
                    bestMatchUri = Uri.parse(uriStr)
                }
            }
        }
        return bestMatchUri
    }

    @JvmStatic
    fun getSafProjectPath(context: Context, localPath: String): String? {
        val prefs = getPrefs(context)
        val all = prefs.all
        var bestMatchKey: String? = null

        for ((projPath, _) in all) {
            if (localPath == projPath || localPath.startsWith(projPath + File.separator)) {
                if (bestMatchKey == null || projPath.length > bestMatchKey.length) {
                    bestMatchKey = projPath
                }
            }
        }
        return bestMatchKey
    }

    /**
     * Resolves a SAF tree Uri to an absolute local file path for IDE editing,
     * and registers the mapping so changes sync back to SAF storage device.
     */
    @JvmStatic
    fun resolvePathFromTreeUri(context: Context, uri: Uri): String? {
        init(context)
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val docId = try {
                DocumentsContract.getTreeDocumentId(uri)
            } catch (e: Exception) {
                uri.lastPathSegment
            } ?: uri.path

            val decodedDocId = if (docId != null) Uri.decode(docId) else ""

            val documentTree = DocumentFile.fromTreeUri(context, uri)
            val extractedName = when {
                documentTree != null && !documentTree.name.isNullOrBlank() -> documentTree.name!!
                decodedDocId.contains("primary:") -> decodedDocId.substringAfter("primary:").split("/").lastOrNull { it.isNotEmpty() } ?: "project"
                decodedDocId.contains("/") -> decodedDocId.split("/").lastOrNull { it.isNotEmpty() } ?: "project"
                else -> "project"
            }

            val folderName = extractedName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
            val localProjectDir = File(context.filesDir, "saf_projects/$folderName")
            if (localProjectDir.exists()) {
                localProjectDir.deleteRecursively()
            }
            localProjectDir.mkdirs()

            if (documentTree != null && documentTree.exists()) {
                copyDocumentTreeRecursively(context, documentTree, localProjectDir)
                registerSafTree(context, localProjectDir.absolutePath, uri)
                return localProjectDir.absolutePath
            }

            // Fallback for direct storage paths if reachable
            if (decodedDocId.contains("primary:")) {
                val relativePath = decodedDocId.substringAfter("primary:")
                val externalStorage = Environment.getExternalStorageDirectory()
                val targetFile = File(externalStorage, relativePath)
                if (!targetFile.exists()) {
                    targetFile.mkdirs()
                }
                return targetFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun copyDocumentTreeRecursively(context: Context, sourceTree: DocumentFile, destDir: File) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val children = sourceTree.listFiles()
        for (file in children) {
            val name = file.name ?: continue
            if (file.isDirectory) {
                val subDestDir = File(destDir, name)
                copyDocumentTreeRecursively(context, file, subDestDir)
            } else {
                val targetFile = File(destDir, name)
                try {
                    context.contentResolver.openInputStream(file.uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Synchronizes a local file back to the actual SAF storage device or cloud location.
     */
    @JvmStatic
    fun syncFileToSaf(localFile: File) {
        val context = appContext ?: return
        try {
            val localPath = localFile.absolutePath
            val projPath = getSafProjectPath(context, localPath) ?: return
            val treeUri = getSafTreeUriForPath(context, localPath) ?: return

            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return
            if (!rootDoc.exists()) return

            val relativePath = if (localPath.startsWith(projPath)) {
                localPath.substring(projPath.length).trimStart(File.separatorChar)
            } else return

            if (relativePath.isEmpty()) return

            val segments = relativePath.split(File.separatorChar).filter { it.isNotEmpty() }
            var currentDoc: DocumentFile = rootDoc

            if (localFile.isDirectory) {
                // Ensure directory path exists in SAF
                for (segment in segments) {
                    var childDir = currentDoc.findFile(segment)
                    if (childDir == null || !childDir.isDirectory) {
                        childDir = currentDoc.createDirectory(segment)
                    }
                    if (childDir == null) return
                    currentDoc = childDir
                }
            } else {
                // Navigate directories
                for (i in 0 until segments.size - 1) {
                    val dirName = segments[i]
                    var childDir = currentDoc.findFile(dirName)
                    if (childDir == null || !childDir.isDirectory) {
                        childDir = currentDoc.createDirectory(dirName)
                    }
                    if (childDir == null) return
                    currentDoc = childDir
                }

                val fileName = segments.last()
                var targetDoc = currentDoc.findFile(fileName)
                if (targetDoc == null || !targetDoc.isFile) {
                    val mimeType = getMimeType(fileName)
                    targetDoc = currentDoc.createFile(mimeType, fileName)
                }

                if (targetDoc != null && localFile.exists()) {
                    context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { output ->
                        localFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a file or directory from the SAF tree on the storage device.
     */
    @JvmStatic
    fun deleteFromSaf(localFile: File) {
        val context = appContext ?: return
        try {
            val localPath = localFile.absolutePath
            val projPath = getSafProjectPath(context, localPath) ?: return
            val treeUri = getSafTreeUriForPath(context, localPath) ?: return

            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val relativePath = localPath.substring(projPath.length).trimStart(File.separatorChar)
            if (relativePath.isEmpty()) return

            val segments = relativePath.split(File.separatorChar).filter { it.isNotEmpty() }
            var currentDoc: DocumentFile? = rootDoc

            for (segment in segments) {
                currentDoc = currentDoc?.findFile(segment)
                if (currentDoc == null) break
            }

            currentDoc?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renames a file or directory in the SAF tree on the storage device.
     */
    @JvmStatic
    fun renameInSaf(localFile: File, newName: String) {
        val context = appContext ?: return
        try {
            val localPath = localFile.absolutePath
            val projPath = getSafProjectPath(context, localPath) ?: return
            val treeUri = getSafTreeUriForPath(context, localPath) ?: return

            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val relativePath = localPath.substring(projPath.length).trimStart(File.separatorChar)
            if (relativePath.isEmpty()) return

            val segments = relativePath.split(File.separatorChar).filter { it.isNotEmpty() }
            var currentDoc: DocumentFile? = rootDoc

            for (segment in segments) {
                currentDoc = currentDoc?.findFile(segment)
                if (currentDoc == null) break
            }

            currentDoc?.renameTo(newName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "py" -> "text/x-python"
            "java" -> "text/x-java"
            "kt", "kts" -> "text/x-kotlin"
            "xml" -> "text/xml"
            "md" -> "text/markdown"
            else -> "text/plain"
        }
    }
}
