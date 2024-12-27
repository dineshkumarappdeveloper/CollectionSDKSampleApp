package com.blackstraw.shelfauditsdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Utility functions for handling bitmap files in local storage.
 * The functions are used to save and delete bitmap files from local storage.
 */
fun saveBitmapToLocalStorage(bitmap: Bitmap, filePath: String) {
    val file = File(filePath)
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

/**
 * Function to delete a bitmap file from local storage.
 *
 * @param filePath The file path of the bitmap file.
 */
fun deleteBitmapFromLocalStorage(filePath: String) {
    val file = File(filePath)
    if (file.exists()) {
        file.delete()
    }
}

/**
 * Function to check if a bitmap file exists in local storage.
 *
 * @param filePath The file path of the bitmap file.
 * @return True if the file exists, false otherwise.
 */
fun checkBitmapExistsInLocalStorage(filePath: String): Boolean {
    val file = File(filePath)
    return file.exists()
}

/**
 * Function to get the bitmap file size in local storage.
 *
 * @param filePath The file path of the bitmap file.
 * @return The size of the bitmap file in bytes.
 */
fun getBitmapFileSize(filePath: String): Long {
    val file = File(filePath)
    return file.length()
}

/**
 * Function to save an MP4 file to local storage.
 *
 * @param inputFilePath The file path of the input MP4 file.
 * @param outputFilePath The file path where the MP4 file should be saved.
 */
fun saveMp4ToLocalStorage(inputFilePath: String, outputFilePath: String) {
    val inputFile = File(inputFilePath)
    val outputFile = File(outputFilePath)
    inputFile.inputStream().use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * Function to load a file from the given path into a MappedByteBuffer.
 * The function will check if the file exists at the given path.
 * If the file exists, it will map the file into memory using a FileChannel.
 * If the file does not exist or an I/O error occurs, it will log an error message and return null.
 *
 * @param filePath The file path of the file to be loaded.
 * @return A MappedByteBuffer containing the file data, or null if the file is not found or an error occurs.
 * @throws IOException If an I/O error occurs while reading the file.
 */
@Throws(IOException::class)
fun loadMappedFileFromPath( context: Context, filePath: String, assetFileName: String): MappedByteBuffer? {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("File not found: Getting file from assets folder", filePath)
            val fileDescriptor = context.assets.openFd(assetFileName)
            val fileChannel = FileInputStream(fileDescriptor.fileDescriptor).channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)

        }

        FileInputStream(file).use { inputStream ->
            val fileChannel = inputStream.channel
            val size = fileChannel.size()
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size)
        }
    } catch (ex: FileNotFoundException) {
        Log.e("File not found: ",": ${ex.message}")
        return null
    } catch (ex: IOException) {
        Log.e("I/O error", ": ${ex.message}")
        return null
    }
}