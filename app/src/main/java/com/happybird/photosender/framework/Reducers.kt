package com.happybird.photosender.framework

import com.happybird.photosender.framework.utils.generateHashSet
import com.happybird.photosender.framework.utils.generateList


fun <T> addItemToList(collection: Collection<T>, item: T): List<T> {
    return generateList<T> {
        addAll(collection)
        add(item)
    }
}

fun <T> addItemToSet(collection: Collection<T>, item: T): Set<T> {
    return generateHashSet {
        add(item)
        addAll(collection)
    }
}