package com.happybird.photosender.framework.data.mappers

import com.happybird.photosender.domain.Chat
import com.happybird.photosender.domain.EntityMapper
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatMapper @Inject constructor(private val messageMapper: MessageMapper)
    : EntityMapper<TdApi.Chat, Chat> {

    override fun toDomain(e: TdApi.Chat): Chat {
        return Chat(
                e.id,
                if(e.positions.isEmpty())
                    e.id
                else
                    e.positions.first().order,
                e.title,
                if(e.lastMessage == null)
                    null
                else
                    messageMapper.toDomain(e.lastMessage!!),
                e.photo?.small,
                e.photo?.big
        )
    }

    override fun toEntity(d: Chat): TdApi.Chat {
        TODO("Not yet implemented")
    }

}