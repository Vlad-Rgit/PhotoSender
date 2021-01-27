package com.happybird.photosender.framework.utils

fun <T> generateList(block: MutableList<T>.() -> Unit): List<T> {
    val list = mutableListOf<T>()
    block(list)
    return list
}

fun <T> generateHashSet(block: HashSet<T>.() -> Unit): Set<T> {
    val hashSet = hashSetOf<T>()
    block(hashSet)
    return hashSet
}

fun <K, T> generateMap(block: HashMap<K, T>.() -> Unit): Map<K, T> {
    val hashMap = hashMapOf<K, T>()
    block(hashMap)
    return hashMap
}