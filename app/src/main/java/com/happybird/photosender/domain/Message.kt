package com.happybird.photosender.domain

import org.drinkless.td.libcore.telegram.TdApi

enum class MessageType {
    Text,
    Photo,
    Unknown
}

data class Message(
        val id: Long,
        val chatId: Long,
        val senderId: Long,
        val text: String?,
        val photo: TdApi.LocalFile?,
        val date: Int,
        val messageType: MessageType
)