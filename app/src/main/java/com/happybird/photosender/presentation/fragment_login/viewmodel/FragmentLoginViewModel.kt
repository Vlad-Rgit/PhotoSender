package com.happybird.photosender.presentation.fragment_login.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.framework.TelegramClientException
import com.happybird.photosender.framework.data.TelegramClient
import com.happybird.photosender.presentation.fragment_login.LoginIntent
import com.happybird.photosender.presentation.fragment_login.SetCodeIntent
import com.happybird.photosender.presentation.fragment_login.SetNumberIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class FragmentLoginViewModel(app: Application): AndroidViewModel(app) {

    @Inject
    lateinit var telegramClient: TelegramClient

    private val _authState = MutableLiveData<TelegramClient.Authentication>()
    val authState: LiveData<TelegramClient.Authentication>
        get() = _authState

    private val _exceptionsState = MutableLiveData<Exception>()
    val exceptionsState: LiveData<Exception> = _exceptionsState

    init {

        (app as PhotoSenderApp).appComponent
            .inject(this)

        //Observer Authentication State
        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.authState.collect {
                _authState.postValue(it)
            }
        }

        //Init Client
        viewModelScope.launch(Dispatchers.IO) {
            telegramClient.init()
        }
    }

    fun sendIntent(intent: LoginIntent) {
        when(intent) {
            is SetNumberIntent -> handleSetNumberIntent(intent)
            is SetCodeIntent -> handleSetCodeIntent(intent)
        }
    }

    private fun handleSetNumberIntent(intent: SetNumberIntent) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(5000L) {
                    telegramClient.setPhoneNumber(intent.number)
                } ?: throw TimeoutException("Timeout")
            }
            catch (ex: Exception) {
                _exceptionsState.postValue(ex)
            }
        }
    }

    private fun handleSetCodeIntent(intent: SetCodeIntent) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(5000L) {
                    telegramClient.setCode(intent.code)
                } ?: throw TimeoutException("Timeout")
            }
            catch (ex: Exception) {
                _exceptionsState.postValue(ex)
            }
        }
    }


}