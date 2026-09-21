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
     * Takes persistable permissions, checks primary storage & raw paths, and recursively
     * imports SAF tree contents for virtual/external locations into local project storage.
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

            // 1. Direct raw storage path (e.g. raw:/storage/emulated/0/...)
            if (decodedDocId.contains("raw:")) {
                val rawPath = decodedDocId.substringAfter("raw:")
                val rawFile = File(rawPath)
                if (rawFile.exists()) {
                    return rawFile.absolutePath
                }
            }

            // 2. Primary internal storage path (e.g. primary:Download/MyFolder)
            if (decodedDocId.contains("primary:")) {
                val relativePath = decodedDocId.substringAfter("primary:")
                val externalStorage = Environment.getExternalStorageDirectory()
                val targetFile = File(externalStorage, relativePath)
                if (!targetFile.exists()) {
                    targetFile.mkdirs()
                }
                return targetFile.absolutePath
            }

            // 3. Absolute path in docId (e.g. /storage/emulated/0/...)
            if (decodedDocId.startsWith("/storage/") || decodedDocId.startsWith("/sdcard/")) {
                val directFile = File(decodedDocId)
                if (directFile.exists()) {
                    return directFile.absolutePath
                }
            }

            // 4. Fallback for external SD cards or virtual providers:
            // Recursively import the DocumentFile tree into local app workspace storage
            val documentTree = DocumentFile.fromTreeUri(context, uri)
            if (documentTree != null && documentTree.exists()) {
                val folderName = documentTree.name ?: "saf_project"
                val localProjectDir = File(context.filesDir, "saf_projects/$folderName")
                if (localProjectDir.exists()) {
                    localProjectDir.deleteRecursively()
                }
                localProjectDir.mkdirs()

                copyDocumentTreeRecursively(context, documentTree, localProjectDir)
                return localProjectDir.absolutePath
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
            } else if (file.isFile) {
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
