package com.example.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object SafUtils {

    /**
     * Attempts to resolve a SAF tree Uri (e.g. from OpenDocumentTree) to a usable absolute file path,
     * taking persistable permissions and handling primary storage and fallback workspaces.
     */
    fun resolvePathFromTreeUri(context: Context, uri: Uri): String? {
        try {
            // Take persistable URI permission so the app maintains access
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
            }

            if (docId != null && docId.contains("primary:")) {
                val relativePath = docId.substringAfter("primary:")
                val externalStorage = Environment.getExternalStorageDirectory()
                val file = File(externalStorage, relativePath)
                if (!file.exists()) {
                    file.mkdirs()
                }
                return file.absolutePath
            }

            // Fallback for non-primary storage or custom tree URIs
            val folderName = docId?.replace(":", "_")?.replace("/", "_") ?: "saf_project"
            val localCopyDir = File(context.filesDir, "saf_projects/$folderName")
            if (!localCopyDir.exists()) {
                localCopyDir.mkdirs()
            }
            return localCopyDir.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
