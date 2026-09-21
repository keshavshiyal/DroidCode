package com.example.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

object SafUtils {

    /**
     * Resolves a SAF tree Uri (e.g. from OpenDocumentTree) to an absolute file path.
     * Takes persistable permissions and recursively imports all files and subdirectories
     * into app workspace storage so that full read/write/create capabilities work natively.
     */
    fun resolvePathFromTreeUri(context: Context, uri: Uri): String? {
        try {
            // Take persistable URI permission so the app maintains access across restarts
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not grantable or already granted
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
                return localProjectDir.absolutePath
            }

            // Fallback to direct path if DocumentFile wasn't resolvable
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
}
