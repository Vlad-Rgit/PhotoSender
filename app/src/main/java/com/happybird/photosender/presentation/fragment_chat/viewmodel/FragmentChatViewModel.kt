package com.happybird.photosender.presentation.fragment_chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.domain.PhotoSend
import com.happybird.photosender.domain.User
import com.happybird.photosender.framework.data.TelegramClient
import com.happybird.photosender.framework.data.mappers.UserMapper
import com.happybird.photosender.presentation.fragment_chat.FragmentChatState
import com.happybird.photosender.presentation.fragment_chat.MessagesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class FragmentChatViewModel(application: Application, chatId: Long)
    : AndroidViewModel(application) {

    @Inject
    lateinit var telegramClient: TelegramClient

    @Inject
    lateinit var userMapper: UserMapper


    private val _state = MutableLiveData<FragmentChatState>()
    val state: LiveData<FragmentChatState> = _state

    val currentUser: User

    init {
        val photoApp = application as PhotoSenderApp
        photoApp.appComponent.inject(this)

        currentUser = userMapper.toDomain(telegramClient.currentUser)
        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.registerInboxChatId(chatId)
            telegramClient.updateMessages(100)
            telegramClient.inbox
                .map {
                    it.reversed()
                        .distinctBy {
                            it.id
                        }
                }
                .collectLatest {
                    _state.postValue(MessagesState(it))
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        telegramClient.clearInbox()
    }

    fun sendImage(photoSend: PhotoSend) {
        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.sendImageMessage(photoSend)
        }
    }

    fun sendTextMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.sendTextMessage(text)
        }
    }

}