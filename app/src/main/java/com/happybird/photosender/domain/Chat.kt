package com.happybird.photosender.domain

import org.drinkless.td.libcore.telegram.TdApi

data class Chat(
        val id: Long,
        val position: Long,
        val title: String,
        val lastMessage: Message?,
        val smallPhoto: TdApi.File?,
        val bigPhoto: TdApi.File?
): ListItem<Chat> {

    override fun areContentsTheSame(other: Chat): Boolean {
        return this == other
    }

    override fun areItemsTheSame(other: Chat): Boolean {
        return this.id == other.id
    }

    fun updatePosition(position: Long): Chat {
        return Chat(
                id,
                position,
                title,
                lastMessage,
                smallPhoto,
                bigPhoto
        )
    }

    fun updateLastMessage(lastMessage: Message?): Chat {
        return Chat(
                id,
                position,
                title,
                lastMessage,
                smallPhoto,
                bigPhoto
        )
    }


}