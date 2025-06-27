package com.example.realtimecalltranslation.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageStorageHelper {

    private const val PROFILE_PICS_DIR = "profile_pics"
    private const val TAG = "ImageStorageHelper"

    fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return null

            val directory = File(context.filesDir, PROFILE_PICS_DIR)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: ${directory.absolutePath}")
                    return null
                }
            }

            // Create a unique filename, e.g., using timestamp or the original segment if available
            val fileName = "profile_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "image"}"
            val outputFile = File(directory, fileName)

            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)

            Log.d(TAG, "Image saved successfully to: ${outputFile.absolutePath}")
            return Uri.fromFile(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to internal storage", e)
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    fun deleteImageFromInternalStorage(fileUri: String?): Boolean {
        if (fileUri == null) return false
        return try {
            val localUri = Uri.parse(fileUri)
            if (localUri.scheme == "file") { // Ensure it's a file URI
                val file = File(localUri.path ?: return false)
                if (file.exists()) {
                    if (file.delete()) {
                        Log.d(TAG, "Image deleted successfully: $fileUri")
                        true
                    } else {
                        Log.e(TAG, "Failed to delete image: $fileUri")
                        false
                    }
                } else {
                    Log.w(TAG, "Image not found for deletion: $fileUri")
                    false // File not found
                }
            } else {
                Log.w(TAG, "Not a file URI, cannot delete: $fileUri")
                false // Not a local file URI
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: $fileUri", e)
            false
        }
    }
}
