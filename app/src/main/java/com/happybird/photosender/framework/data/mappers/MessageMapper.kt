package com.happybird.photosender.framework.data.mappers

import com.happybird.photosender.domain.EntityMapper
import com.happybird.photosender.domain.Message
import com.happybird.photosender.domain.MessageType
import org.drinkless.td.libcore.telegram.TdApi
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MessageMapper @Inject constructor()
    : EntityMapper<TdApi.Message, Message> {

    override fun toDomain(e: TdApi.Message): Message {

        val messageType = when (e.content.constructor) {
            TdApi.MessageText.CONSTRUCTOR -> MessageType.Text
            TdApi.MessagePhoto.CONSTRUCTOR -> MessageType.Photo
            else -> MessageType.Unknown
        }


        var localFile: TdApi.File? = null
        var text: String? = null
        var photoHeight: Int? = null
        var photoWidth: Int? = null

        if (messageType == MessageType.Text) {
            val content = e.content as TdApi.MessageText
            text = content.text.text
        } else if (messageType == MessageType.Photo) {
            val content = e.content as TdApi.MessagePhoto
            if (content.photo.sizes.isNotEmpty()) {
                val photoSize = content.photo.sizes.last()
                photoHeight = photoSize.height
                photoWidth = photoSize.width
                localFile = photoSize.photo
            }
        }

        val senderId: Long = when (e.sender.constructor) {
            TdApi.MessageSenderUser.CONSTRUCTOR -> {
                (e.sender as TdApi.MessageSenderUser).userId.toLong()
            }
            TdApi.MessageSenderChat.CONSTRUCTOR -> {
                (e.sender as TdApi.MessageSenderChat).chatId
            }
            else -> throw IllegalStateException("Unknown message sender ${e.sender}")
        }

        return Message(
            e.id,
            e.chatId,
            senderId,
            text,
            localFile,
            e.date,
            messageType,
            e.sendingState?.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR,
            e.sendingState?.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR,
            photoHeight,
            photoWidth
        )
    }


    override fun toEntity(d: Message): TdApi.Message {
        TODO("Not yet implemented")
    }


}