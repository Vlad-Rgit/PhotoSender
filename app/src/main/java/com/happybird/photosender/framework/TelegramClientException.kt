package com.happybird.photosender.framework

import java.lang.RuntimeException

class TelegramClientException(val code: Int, message: String)
    : RuntimeException(message)