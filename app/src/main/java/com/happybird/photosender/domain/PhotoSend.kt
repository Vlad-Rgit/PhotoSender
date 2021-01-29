package com.happybird.photosender.domain

data class PhotoSend(
    val path: String,
    var width: Int = 0,
    var height: Int = 0,
    val caption: String? = null
): ListItem<PhotoSend> {

    override fun areContentsTheSame(other: PhotoSend): Boolean {
        return path == other.path
    }

    override fun areItemsTheSame(other: PhotoSend): Boolean {
        return path == other.path
    }

}