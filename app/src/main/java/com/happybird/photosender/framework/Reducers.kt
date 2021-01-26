package com.happybird.photosender.framework

@ExperimentalStdlibApi
fun <T> addItemToList(list: List<T>, item: T): List<T> {
    return buildList<T> {
        addAll(list)
        add(item)
    }
}