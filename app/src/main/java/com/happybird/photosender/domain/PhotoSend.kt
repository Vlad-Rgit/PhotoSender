package com.happybird.photosender.domain

data class PhotoSend(
    val path: String,
    var width: Int = 0,
    var height: Int = 0,
    val caption: String? = null
)