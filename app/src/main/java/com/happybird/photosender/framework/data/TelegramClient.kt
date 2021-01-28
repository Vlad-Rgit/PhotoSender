package com.happybird.photosender.framework.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.happybird.photosender.config.Config
import com.happybird.photosender.domain.*
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.domain.Message
import com.happybird.photosender.domain.User
import com.happybird.photosender.framework.TelegramClientException
import com.happybird.photosender.framework.data.extensions.send
import com.happybird.photosender.framework.data.mappers.ChatMapper
import com.happybird.photosender.framework.data.mappers.MessageMapper
import com.happybird.photosender.framework.data.mappers.UserMapper
import com.happybird.photosender.framework.utils.deletePhotoFromCache
import com.happybird.photosender.framework.utils.generateList
import com.happybird.photosender.framework.utils.generateMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.*
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TelegramClient
    @Inject constructor(
        private val context: Context,
        private val chatMapper: ChatMapper,
        private val userMapper: UserMapper,
        private val messageMapper: MessageMapper
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

    private var inboxChatId: Long? = null

    private val _inbox = MutableStateFlow<List<Message>>(emptyList())
    val inbox: StateFlow<List<Message>> = _inbox

    lateinit var currentUser: TdApi.User

    private val _photosCache = hashMapOf<Long, String>()

    suspend fun init() {
        val authState = client.send(TdApi.GetAuthorizationState())
        handleUpdateAuthState(authState)
    }

    fun getChatInfo(chatId: Long): Chat {
        return synchronized(_chats) {
            _chats.value[chatId] ?: error("No Chat Found with id $chatId")
        }
    }

    suspend fun initChats() {
        val offsetOrder = Long.MAX_VALUE
        val offsetChatId: Long = 0
        val limit = 100
        val result  = client.send(
            TdApi.GetChats(
                TdApi.ChatListMain(),
                offsetOrder,
                offsetChatId,
                limit
            )
        )
        Log.i(TAG, "Chats got")
    }

    suspend fun sendTextMessage(message: String) {
        withContext(Dispatchers.IO) {
            val chatId = inboxChatId!!
            val row = arrayOf(
                InlineKeyboardButton(
                    "https://telegram.org?1",
                    InlineKeyboardButtonTypeUrl()
                ),
                InlineKeyboardButton("https://telegram.org?2", InlineKeyboardButtonTypeUrl()),
                InlineKeyboardButton("https://telegram.org?3", InlineKeyboardButtonTypeUrl())
            )

            val replyMarkup: ReplyMarkup = ReplyMarkupInlineKeyboard(arrayOf(row, row, row))

            val content: InputMessageContent =
                InputMessageText(
                    FormattedText(message, null),
                    false,
                    true
                )

            val message = client.send(SendMessage(
                chatId,
                0,
                0,
                null,
                replyMarkup,
                content)) as TdApi.Message

            if(inboxChatId == chatId) {
                addMessageToInbox(messageMapper
                    .toDomain(message)
                )
            }
        }
    }

    fun registerInboxChatId(chatId: Long) {
        inboxChatId = chatId
    }

    fun clearInbox() {
        inboxChatId = null
        _inbox.value = emptyList()
    }

    suspend fun sendImageMessage(photoSend: PhotoSend) {
        withContext(Dispatchers.IO) {

            val chatId = inboxChatId!!

            val row = arrayOf(
                InlineKeyboardButton(
                    "https://telegram.org?1",
                    InlineKeyboardButtonTypeUrl()
                ),
                InlineKeyboardButton("https://telegram.org?2", InlineKeyboardButtonTypeUrl()),
                InlineKeyboardButton("https://telegram.org?3", InlineKeyboardButtonTypeUrl())
            )

            val replyMarkup: ReplyMarkup = ReplyMarkupInlineKeyboard(arrayOf(row, row, row))


            try {

               val message = client.send(
                    TdApi.SendMessage(
                        chatId,
                        0,
                        0,
                        null,
                        replyMarkup,
                        InputMessagePhoto(
                            TdApi.InputFileLocal(
                                photoSend.path
                            ),
                            null,
                            null,
                            photoSend.width,
                            photoSend.height,
                            FormattedText(
                                photoSend.caption,
                                null
                            ),
                            0
                        )
                    )
                ) as TdApi.Message

                synchronized(_photosCache) {
                    _photosCache.put(message.id, photoSend.path)
                }
            }
            catch (ex: java.lang.Exception) {
                Log.e(TAG, ex.message, ex)
            }
        }
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
        try {
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
        catch (ex: TelegramClientException) {
            Log.e(TAG, ex.message, ex)
        }
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
            try {
                client.send(TdApi.CheckDatabaseEncryptionKey())
            }
            catch (ex: TelegramClientException) {
                Log.e(TAG, ex.message, ex)
            }
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
                Log.d(
                    TAG, "onResult: AuthorizationStateWaitPhoneNumber " +
                            "-> state = WAIT_FOR_NUMBER"
                )
                setAuth(Authentication.WAIT_FOR_NUMBER)
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                Log.d(
                    TAG, "onResult: AuthorizationStateWaitCode " +
                            "-> state = WAIT_FOR_CODE"
                )
                setAuth(Authentication.WAIT_FOR_CODE)
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                Log.d(TAG, "onResult: AuthorizationStateWaitPassword")
                setAuth(Authentication.WAIT_FOR_PASSWORD)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(
                    TAG, "onResult: AuthorizationStateReady " +
                            "-> state = AUTHENTICATED"
                )
                setAuth(Authentication.AUTHENTICATED)
                CoroutineScope(Dispatchers.IO).launch {
                    setCurrentUser()
                }
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


    suspend fun downloadFile(fileId: Int): TdApi.File {
        return withContext(Dispatchers.IO) {
            client.send(
                TdApi.DownloadFile(
                    fileId,
                    1,
                    0,
                    0,
                    true
                )
            ) as TdApi.File
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
            TdApi.UpdateFile.CONSTRUCTOR -> {
                handleUpdateFile(state)
            }
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                handleUpdateLastMessage(state)
            }
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                handleUpdateNewMessage(state)
            }
            TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
                handleUpdateSuccessSent(state)
            }
            TdApi.UpdateMessageSendFailed.CONSTRUCTOR -> {
                handleUpdateSuccessFailed(state)
            }
        }
    }

    private fun handleUpdateSuccessSent(value: Object) {
        val update = value as TdApi.UpdateMessageSendSucceeded
        val message = messageMapper.toDomain(update.message)

        synchronized(_photosCache) {
            if (message.messageType == MessageType.Photo &&
                _photosCache.containsKey(message.id)
            ) {
                try {
                    deletePhotoFromCache(_photosCache[message.id]!!)
                    _photosCache.remove(message.id)
                } catch (ex: java.lang.Exception) {
                    Log.e(TAG, ex.message, ex)
                }
            }
        }
    }

    private fun handleUpdateSuccessFailed(value: Object) {
        val update = value as TdApi.UpdateMessageSendSucceeded
        val chatId = inboxChatId
        if(chatId == update.message.chatId) {
            val index = _inbox.value.indexOfFirst {
                it.id == update.oldMessageId
            }

            if(index != -1) {
                synchronized(_inbox) {
                    val messages = _inbox.value
                        .toMutableList()
                    messages[index] = messages[index]
                        .updateIsFailed(true)
                        .updateIsSending(false)
                    _inbox.value = messages
                }
            }
        }
    }

    private fun handleUpdateNewMessage(value: TdApi.Object) {
        val chatId = inboxChatId
        val update = value as TdApi.UpdateNewMessage
        if(chatId != null &&
            update.message.chatId == chatId) {
            val message = messageMapper.toDomain(update.message)
            addMessageToInbox(message)
        }
    }

    private fun addMessageToInbox(message: Message) {
        synchronized(_inbox) {
            _inbox.value = generateList {
                add(message)
                addAll(_inbox.value)
            }
        }
    }

    suspend fun setCurrentUser() {
        return withContext(Dispatchers.IO) {
            currentUser = client.send(TdApi.GetMe())
                as TdApi.User
        }
    }

    suspend fun updateMessages(amountFromLast: Int) {
        return withContext(Dispatchers.IO) {
            val chatId = inboxChatId!!
            try {

                var counter = 0
                var fromMessageIdLocal = 0L

                while (counter < amountFromLast) {
                    val result = client.send(
                        TdApi.GetChatHistory(
                            chatId,
                            fromMessageIdLocal,
                            0,
                            amountFromLast,
                            false
                        )
                    ) as TdApi.Messages

                    if(result.messages.isEmpty()) {
                        break
                    }

                    fromMessageIdLocal = result.messages.last().id
                    counter += result.messages.size

                    synchronized(_inbox) {
                        _inbox.value = generateList {
                            addAll(_inbox.value)
                            addAll(
                                messageMapper.toDomainList(
                                    result.messages.asIterable()
                                )
                            )
                        }
                    }
                }
            }
            catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                emptyList<Message>()
            }
        }
    }


    private fun handleUpdateFile(value: TdApi.Object) {
        Log.d(TAG, "Update File Download: $value")
    }

    private fun handleUpdateLastMessage(value: TdApi.Object) {
        val update = value as TdApi.UpdateChatLastMessage

        synchronized(_chats) {
            val chat = _chats.value[update.chatId]
                    ?: error("No chat at ${update.chatId} in handle update last message")
            val message = if(update.lastMessage == null)
                null
            else
                messageMapper.toDomain(update.lastMessage!!)
            _chats.value = generateMap {
                putAll(_chats.value)
                put(update.chatId, chat.updateLastMessage(message))
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


    private fun handleNewChat(chat: TdApi.Chat) {
        if(chat.type.constructor == TdApi.ChatTypePrivate.CONSTRUCTOR) {
            val chatDomain = chatMapper.toDomain(chat)
            synchronized(_chats) {
                _chats.value = generateMap {
                    putAll(_chats.value)
                    put(chatDomain.id, chatDomain)
                }
            }
        }
    }


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