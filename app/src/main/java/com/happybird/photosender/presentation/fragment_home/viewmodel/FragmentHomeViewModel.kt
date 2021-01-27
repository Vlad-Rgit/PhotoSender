package com.happybird.photosender.presentation.fragment_home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.framework.data.TelegramClient
import com.happybird.photosender.framework.data.comparators.ChatComparator
import com.happybird.photosender.presentation.fragment_home.FragmentHomeState
import com.happybird.photosender.presentation.fragment_home.ListState
import com.happybird.photosender.presentation.fragment_home.LoadingState
import com.happybird.photosender.presentation.fragment_home.ui.FragmentHome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject



class FragmentHomeViewModel(app: Application): AndroidViewModel(app) {

    @Inject
    lateinit var telegramClient: TelegramClient

    private val _state = MutableLiveData<FragmentHomeState>(LoadingState)
    val state: LiveData<FragmentHomeState> = _state

    init {
        (app as PhotoSenderApp)
                .appComponent
                .inject(this)

        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.initChats()
            telegramClient.chat
                    .map {
                        it.values.sortedByDescending { chat ->
                            chat.position
                        }
                    }
                    .collectLatest {
                        _state.postValue(ListState(it))
                    }
        }
    }
}