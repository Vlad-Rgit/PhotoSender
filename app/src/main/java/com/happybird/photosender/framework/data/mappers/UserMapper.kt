package com.happybird.photosender.framework.data.mappers

import com.happybird.photosender.domain.EntityMapper
import com.happybird.photosender.domain.User
import org.drinkless.td.libcore.telegram.TdApi
import javax.inject.Inject

class UserMapper @Inject constructor()
    : EntityMapper<TdApi.User, User> {

    override fun toDomain(e: TdApi.User): User {
        return User(
                e.id,
                e.username,
                e.profilePhoto?.small?.local,
                e.profilePhoto?.big?.local
        )
    }

    override fun toEntity(d: User): TdApi.User {
        TODO("Not yet implemented")
    }

}