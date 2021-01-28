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
class TelegramFileProvider @Inject constructor(
        private val telegramClient: TelegramClient
) {

    suspend fun getFileContent(file: TdApi.File): ByteArray {
        return withContext(Dispatchers.IO) {
            val local = file.local
            if(!local.isDownloadingActive) {
                telegramClient.downloadFile(file.id)
                while (!local.isDownloadingCompleted) {
                    delay(100L)
                }
            }
            val stream = FileInputStream(local.path)
            val bytes = ByteArray(local.downloadedSize)
            stream.read(bytes, 0, bytes.size)
            stream.close()
            bytes
        }
    }

}