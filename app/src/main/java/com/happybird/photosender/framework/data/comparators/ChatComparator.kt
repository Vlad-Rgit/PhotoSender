package com.happybird.photosender.framework.data.comparators

import com.happybird.photosender.domain.Chat
import java.util.*

class ChatComparator: Comparator<Chat> {
    override fun compare(o1: Chat, o2: Chat): Int {
        if (o1.position != o2.position) {
            return if (o2.position < o1.position) -1 else 1
        }
        return if (o1.id != o2.id) {
            if (o2.id < o1.id) -1 else 1
        } else 0
    }


}