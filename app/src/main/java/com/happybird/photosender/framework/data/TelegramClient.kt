package com.happybird.photosender.framework.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.happybird.photosender.config.Config
import com.happybird.photosender.framework.addItemToList
import com.happybird.photosender.framework.data.extensions.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalStdlibApi
@Singleton
class TelegramClient
    @Inject constructor(private val context: Context)
    : Client.ResultHandler {

    companion object {
        private const val TAG = "TelegramClient"
    }

    enum class Authentication {
        UNAUTHENTICATED,
        WAIT_FOR_NUMBER,
        WAIT_FOR_CODE,
        WAIT_FOR_PASSWORD,
        AUTHENTICATED,
        WAIT_FOR_PARAMETERS,
        UNKNOWN
    }

    private var isParametersSender = false

    private val client = Client.create(this, null,null)

    private val _authState = MutableStateFlow(Authentication.UNKNOWN)
    val authState: StateFlow<Authentication> = _authState

    private val _chats = MutableStateFlow(emptyList<TdApi.Chat>())
    val chat: StateFlow<List<TdApi.Chat>> = _chats


    suspend fun init() {
        val authState = client.send(TdApi.GetAuthorizationState())
        handleUpdateAuthState(authState)
    }

    suspend fun initChats() {
        client.send(TdApi.GetChats())
    }

    override fun onResult(value: TdApi.Object?) {
        value?.let {
            when(value.constructor) {
                TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                    handleUpdateAuthState((value as TdApi.UpdateAuthorizationState).authorizationState)
                }
                else -> handleUpdateState(it)
            }
        }
    }

    suspend fun sendParameters() {
        val params = TdApi.TdlibParameters().apply {
            apiId = Config.apiId
            apiHash = Config.appHash
            useMessageDatabase = true
            useSecretChats = true
            systemLanguageCode = Locale.getDefault().language
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = "1.0"
            enableStorageOptimizer = true
            databaseDirectory = context.filesDir.absolutePath
        }

        client.send(TdApi.SetTdlibParameters(params))
        isParametersSender = true
    }

    suspend fun setPhoneNumber(phoneNumber: String) {

        if(!isParametersSender) {
            sendParameters()
        }

        val settings = TdApi.PhoneNumberAuthenticationSettings(
            false,
            false,
            false
        )
        client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings))
        Log.d(TAG, "Number sender $phoneNumber")
    }


    suspend fun setCode(code: String) {
        client.send(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun setPassword(password: String) {
        client.send(TdApi.CheckAuthenticationPassword(password))
    }

    private fun setAuth(auth: Authentication) {
        _authState.value = auth
    }

    private fun setStubEncryptionKey() {
        CoroutineScope(Dispatchers.IO).launch {
            client.send(TdApi.CheckDatabaseEncryptionKey())
        }
    }

    private fun handleUpdateAuthState(value: TdApi.Object) {
        when (value.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                setAuth(Authentication.WAIT_FOR_PARAMETERS)
                CoroutineScope(Dispatchers.IO).launch {
                    sendParameters()
                }
            }
            TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitEncryptionKey")
                setStubEncryptionKey()
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPhoneNumber " +
                        "-> state = WAIT_FOR_NUMBER")
                setAuth(Authentication.WAIT_FOR_NUMBER)
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitCode " +
                        "-> state = WAIT_FOR_CODE")
                setAuth(Authentication.WAIT_FOR_CODE)
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPassword")
                setAuth(Authentication.WAIT_FOR_PASSWORD)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateReady " +
                        "-> state = AUTHENTICATED")
                setAuth(Authentication.AUTHENTICATED)
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateLoggingOut")
                setAuth(Authentication.UNAUTHENTICATED)
            }
            TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateClosing")
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateClosed")
            }
            else -> Log.d(TAG, "Unhandled authorizationState with data: $value.")
        }
    }


    private fun handleUpdateState(state: TdApi.Object) {
        when(state.constructor) {
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                handleNewChat((state as TdApi.UpdateNewChat).chat)
            }
        }
    }

    private fun handleNewChat(chat: TdApi.Chat) {
        _chats.value = addItemToList(_chats.value, chat)
    }

}