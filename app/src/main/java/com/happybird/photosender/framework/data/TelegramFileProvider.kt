package com.happybird.photosender.framework.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.drinkless.td.libcore.telegram.TdApi
import java.io.FileInputStream
import java.io.FileReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramFileProvider @Inject constructor() {

    suspend fun getFileContent(file: TdApi.LocalFile): ByteArray {
        return withContext(Dispatchers.IO) {
            while (!file.isDownloadingCompleted) {
                delay(200L)
            }

            val downloadedFile = FileInputStream(file.path)
            val bytes = ByteArray(file.downloadedSize)
            downloadedFile.read(bytes, 0, bytes.size)
            bytes
        }
    }

}