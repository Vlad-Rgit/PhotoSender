package com.happybird.photosender.framework.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.happybird.photosender.config.Config
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.domain.User
import com.happybird.photosender.framework.addItemToList
import com.happybird.photosender.framework.addItemToSet
import com.happybird.photosender.framework.data.comparators.ChatComparator
import com.happybird.photosender.framework.data.extensions.send
import com.happybird.photosender.framework.data.mappers.ChatMapper
import com.happybird.photosender.framework.data.mappers.UserMapper
import com.happybird.photosender.framework.utils.generateMap
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


@Singleton
class TelegramClient
    @Inject constructor(
            private val context: Context,
            private val chatMapper: ChatMapper,
            private val userMapper: UserMapper
            )
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

    private val client = Client.create(this, null, null)

    private val _authState = MutableStateFlow(Authentication.UNKNOWN)
    val authState: StateFlow<Authentication> = _authState

    private val _chats = MutableStateFlow<Map<Long, Chat>>(hashMapOf())
    val chat: StateFlow<Map<Long, Chat>> = _chats

    private val _users = MutableStateFlow<Map<Int, User>>(hashMapOf())
    val users: StateFlow<Map<Int, User>> = _users


    suspend fun init() {
        val authState = client.send(TdApi.GetAuthorizationState())
        handleUpdateAuthState(authState)
    }

    suspend fun initChats() {
        val offsetOrder = Long.MAX_VALUE
        val offsetChatId: Long = 0
        val limit = 100
        val result  = client.send(TdApi.GetChats(
                TdApi.ChatListMain(),
                offsetOrder,
                offsetChatId,
                limit
        ))
        Log.i(TAG, "Chats got")
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

    @Synchronized
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
            TdApi.UpdateUser.CONSTRUCTOR -> {
                handleUpdateUser((state as TdApi.UpdateUser).user)
            }
            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                handleUpdateChatPositions(state)
            }
        }
    }


    private fun handleUpdateChatPositions(value: TdApi.Object) {

        val updateChat = value as TdApi.UpdateChatPosition

        if(updateChat.position.list.constructor != TdApi.ChatListMain.CONSTRUCTOR) {
            return
        }

        synchronized(_chats) {

            val chat = _chats.value[updateChat.chatId]
                    ?: error("No chat for position update")

            _chats.value = generateMap {
                putAll(_chats.value)
                put(
                        updateChat.chatId,
                        chat.updatePosition(updateChat.position.order)
                )
            }
        }
    }

    @Synchronized
    private fun handleNewChat(chat: TdApi.Chat) {
        val chatDomain = chatMapper.toDomain(chat)
        synchronized(_chats) {
            _chats.value = generateMap {
                putAll(_chats.value)
                put(chatDomain.id, chatDomain)
            }
        }
    }

    @Synchronized
    private fun handleUpdateUser(user: TdApi.User) {
        val userDomain = userMapper.toDomain(user)
        synchronized(_users) {
            val map = _users.value
            _users.value = generateMap {
                putAll(map)
                put(userDomain.id, userDomain)
            }
        }
    }

}