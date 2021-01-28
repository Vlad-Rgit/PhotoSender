package com.happybird.photosender.domain

data class PhotoSend(
    val path: String,
    val width: Int,
    val height: Int,
    val caption: String? = null
)