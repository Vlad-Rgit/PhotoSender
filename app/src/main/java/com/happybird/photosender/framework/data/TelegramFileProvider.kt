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

    suspend fun getFilePath(file: TdApi.File): String {
        return withContext(Dispatchers.IO) {
            val downloadedFile = if(file.local.isDownloadingCompleted) {
                file.local
            }
            else {
                telegramClient.downloadFile(file.id).local
            }
            downloadedFile.path
        }
    }

}