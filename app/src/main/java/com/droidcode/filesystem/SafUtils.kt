package com.droidcode.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    @JvmStatic
    fun getCleanFileName(name: String): String {
        if (name.endsWith(".txt", ignoreCase = true)) {
            val withoutTxt = name.substring(0, name.length - 4)
            val dotIndex = withoutTxt.lastIndexOf('.')
            if (dotIndex > 0) {
                val prevExt = withoutTxt.substring(dotIndex + 1).lowercase()
                if (prevExt.isNotEmpty() && prevExt.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
                    return withoutTxt
                }
            }
        }
        return name
    }

    private fun copyDocumentTreeRecursively(context: Context, sourceTree: DocumentFile, destDir: File) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val children = sourceTree.listFiles()
        for (file in children) {
            val originalName = file.name ?: continue
            if (file.isDirectory) {
                val subDestDir = File(destDir, originalName)
                copyDocumentTreeRecursively(context, file, subDestDir)
            } else {
                val cleanName = getCleanFileName(originalName)
                if (cleanName != originalName) {
                    try {
                        file.renameTo(cleanName)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                val targetFile = File(destDir, cleanName)
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
                if (targetDoc == null) {
                    // Check if SAF previously appended .txt (e.g. temp.bat.txt for temp.bat)
                    val accidentalTxtDoc = currentDoc.findFile("$fileName.txt")
                    if (accidentalTxtDoc != null && accidentalTxtDoc.isFile && !fileName.endsWith(".txt", ignoreCase = true)) {
                        try {
                            accidentalTxtDoc.renameTo(fileName)
                            targetDoc = currentDoc.findFile(fileName) ?: accidentalTxtDoc
                        } catch (e: Exception) {
                            targetDoc = accidentalTxtDoc
                        }
                    }
                }

                if (targetDoc == null || !targetDoc.isFile) {
                    // Use application/octet-stream to prevent Android's DocumentsProvider from appending .txt
                    targetDoc = currentDoc.createFile("application/octet-stream", fileName)
                    if (targetDoc != null && targetDoc.name != null && targetDoc.name != fileName) {
                        if (targetDoc.name?.endsWith(".txt", ignoreCase = true) == true && !fileName.endsWith(".txt", ignoreCase = true)) {
                            try {
                                targetDoc.renameTo(fileName)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
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

            for (i in 0 until segments.size - 1) {
                currentDoc = currentDoc?.findFile(segments[i])
                if (currentDoc == null) break
            }

            val targetName = segments.last()
            var targetDoc = currentDoc?.findFile(targetName)
            if (targetDoc == null) {
                targetDoc = currentDoc?.findFile("$targetName.txt")
            }

            targetDoc?.delete()
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

            for (i in 0 until segments.size - 1) {
                currentDoc = currentDoc?.findFile(segments[i])
                if (currentDoc == null) break
            }

            val targetName = segments.last()
            var targetDoc = currentDoc?.findFile(targetName)
            if (targetDoc == null) {
                targetDoc = currentDoc?.findFile("$targetName.txt")
            }

            targetDoc?.renameTo(newName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(fileName: String): String {
        return "application/octet-stream"
    }

    data class ExportSummary(
        val successCount: Int,
        val failCount: Int
    )

    /**
     * Recursively exports an internal project directory to an external SAF folder.
     * Guaranteed to run on Dispatchers.IO without blocking the UI thread.
     * Reports progress from 0.0f to 1.0f with current file name.
     * Handles per-file errors: logs, continues, and returns summary (successCount, failCount).
     */
    @JvmStatic
    suspend fun exportProjectToSaf(
        context: Context,
        sourceDir: File,
        destinationTreeUri: Uri,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, destinationTreeUri)
                ?: throw IllegalArgumentException("Failed to access destination storage folder")
            if (!rootDoc.canWrite()) {
                throw IllegalStateException("Destination storage folder is not writable")
            }

            // Create target folder in external SAF directory named after the project
            val targetDirDoc = rootDoc.findFile(sourceDir.name)
                ?: rootDoc.createDirectory(sourceDir.name)
                ?: throw IllegalStateException("Could not create target folder: ${sourceDir.name}")

            val allFiles = mutableListOf<File>()
            fun collectFiles(dir: File) {
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        collectFiles(file)
                    } else {
                        allFiles.add(file)
                    }
                }
            }
            collectFiles(sourceDir)

            val totalFiles = allFiles.size
            var processedFiles = 0
            var successCount = 0
            var failCount = 0

            fun copyRecursive(source: File, targetDoc: DocumentFile) {
                val children = source.listFiles() ?: return
                for (child in children) {
                    if (child.isDirectory) {
                        val subDoc = targetDoc.findFile(child.name)
                            ?: targetDoc.createDirectory(child.name)
                        if (subDoc != null) {
                            copyRecursive(child, subDoc)
                        } else {
                            android.util.Log.e("SafUtils", "Failed to create directory in SAF: ${child.name}")
                        }
                    } else {
                        try {
                            val cleanChildName = getCleanFileName(child.name)
                            var fileDoc = targetDoc.findFile(cleanChildName)
                            if (fileDoc == null) {
                                fileDoc = targetDoc.createFile("application/octet-stream", cleanChildName)
                                if (fileDoc != null && fileDoc.name != null && fileDoc.name != cleanChildName) {
                                    if (fileDoc.name?.endsWith(".txt", ignoreCase = true) == true && !cleanChildName.endsWith(".txt", ignoreCase = true)) {
                                        try {
                                            fileDoc.renameTo(cleanChildName)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            }
                            if (fileDoc != null) {
                                context.contentResolver.openOutputStream(fileDoc.uri, "wt")?.use { out ->
                                    child.inputStream().use { input ->
                                        input.copyTo(out)
                                    }
                                } ?: throw IllegalStateException("Failed to open output stream")
                                successCount++
                            } else {
                                throw IllegalStateException("Failed to create file document")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SafUtils", "Failed to export file: ${child.name}", e)
                            failCount++
                        }
                        processedFiles++
                        val progress = if (totalFiles > 0) processedFiles.toFloat() / totalFiles else 1.0f
                        onProgress(progress, child.name)
                    }
                }
            }

            copyRecursive(sourceDir, targetDirDoc)
            ExportSummary(successCount = successCount, failCount = failCount)
        }
    }
}
