package com.happybird.photosender.presentation.fragment_chat.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class FragmentChatViewModelFactory(
    private val application: Application,
    private val chatId: Long
)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(FragmentChatViewModel::class.java.isAssignableFrom(modelClass)) {
            return FragmentChatViewModel(application, chatId) as T
        }
        else {
            throw IllegalArgumentException("$modelClass is not assignable from " +
                    "FragmentChatViewModel")
        }
    }

}