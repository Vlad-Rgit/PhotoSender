package com.happybird.photosender.domain

import org.drinkless.td.libcore.telegram.TdApi

data class User(
        val id: Int,
        val name: String,
        val smallPhoto: TdApi.LocalFile?,
        val bigPhoto: TdApi.LocalFile?
)