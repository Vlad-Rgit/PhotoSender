package com.happybird.photosender.domain

interface ListItem<T> {
    fun areContentsTheSame(other: T): Boolean
    fun areItemsTheSame(other: T): Boolean
}