package com.happybird.photosender.framework.data.extensions

import com.happybird.photosender.framework.TelegramClientException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine




suspend fun Client.send(function: TdApi.Function) = suspendCoroutine<TdApi.Object> {
    continuation ->

    send(
        function,
        {
            when(it.constructor) {
                TdApi.Ok.CONSTRUCTOR -> continuation.resume(it)
                TdApi.Error.CONSTRUCTOR -> {
                    val error = it as TdApi.Error
                    continuation.resumeWithException(
                            TelegramClientException(error.code, error.message)
                    )
                }
                else -> continuation.resume(it)
            }
        },
        { continuation.resumeWithException(it) } //Unhandled Exception
    )
}

