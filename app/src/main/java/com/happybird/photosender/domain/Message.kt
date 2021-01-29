package com.happybird.photosender.domain

import org.drinkless.td.libcore.telegram.TdApi

enum class MessageType {
    Text,
    Photo,
    Album,
    Unknown
}

data class Message(
        val id: Long,
        val chatId: Long,
        val senderId: Long,
        val text: String?,
        val photo: TdApi.File?,
        val date: Int,
        val messageType: MessageType,
        val isSending: Boolean = false,
        val isFailed: Boolean = false,
        val photoHeight: Int? = null,
        val photoWidth: Int? = null
): ListItem<Message> {

    override fun areContentsTheSame(other: Message): Boolean {
        return this == other
    }

    override fun areItemsTheSame(other: Message): Boolean {
        return id == other.id
    }

    fun updateIsSending(isSending: Boolean): Message {
        return Message(
            id,
            chatId,
            senderId,
            text,
            photo,
            date,
            messageType,
            isSending,
            isFailed,
            photoHeight,
            photoWidth
        )
    }


    fun updateIsFailed(isFailed: Boolean): Message {
        return Message(
            id,
            chatId,
            senderId,
            text,
            photo,
            date,
            messageType,
            isSending,
            isFailed,
            photoHeight,
            photoWidth
        )
    }

}