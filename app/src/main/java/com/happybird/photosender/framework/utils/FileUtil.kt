package com.happybird.photosender.framework.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun allocateImageFile(context: Context): String {
    val fileName = "cache_" + System.currentTimeMillis() + ".jpg"
    File(context.cacheDir, fileName).createNewFile()
    return context.cacheDir.path + "/" + fileName
}

fun deletePhotoFromCache(path: String) {
    try {
        val file = File(path)
        file.delete()
    }
    catch (ex: java.lang.Exception) {
        Log.e("DeletePhotoFromCache", ex.message, ex)
    }
}