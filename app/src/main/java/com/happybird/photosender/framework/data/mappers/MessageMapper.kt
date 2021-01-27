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
    : EntityMapper<TdApi.Message, Message>{

    override fun toDomain(e: TdApi.Message): Message {

        val messageType = when(e.content.constructor) {
            TdApi.MessageText.CONSTRUCTOR -> MessageType.Text
            TdApi.MessagePhoto.CONSTRUCTOR -> MessageType.Photo
            else -> MessageType.Unknown
        }

        var localFile: TdApi.LocalFile? = null
        var text: String? = null

        if(messageType == MessageType.Text) {
            val content = e.content as TdApi.MessageText
            text = content.text.text
        }
        else if(messageType == MessageType.Photo) {
            val content = e.content as TdApi.MessagePhoto
            localFile = content.photo.sizes[3].photo.local
        }



        val senderId: Long = when(e.sender.constructor) {
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
                    messageType
            )
    }

    override fun toEntity(d: Message): TdApi.Message {
        TODO("Not yet implemented")
    }


}