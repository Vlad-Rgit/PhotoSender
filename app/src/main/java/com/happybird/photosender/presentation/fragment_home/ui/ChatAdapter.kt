package com.happybird.photosender.presentation.fragment_home.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.R
import com.happybird.photosender.databinding.ItemChatBinding
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.domain.MessageType
import com.happybird.photosender.framework.data.TelegramFileProvider
import com.happybird.photosender.framework.utils.SimpleDiffCallback
import kotlinx.coroutines.*

typealias ChatClickedListener = (Chat) -> Unit

class ChatAdapter(context: Context): RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {


    private val telegramFileProvider: TelegramFileProvider

    init {
        val app = context.applicationContext as PhotoSenderApp
        telegramFileProvider = app.appComponent.getTelegramFileProvider()
    }

    class ChatViewHolder(
            private val binding: ItemChatBinding,
            private val telegramFileProvider: TelegramFileProvider
    )
        : RecyclerView.ViewHolder(binding.root) {

        private var fileLoadingJob: Job? = null
        private lateinit var currentChat: Chat
        private var chatClickedListener: ChatClickedListener? = null

        init {
            binding.root.setOnClickListener {
                chatClickedListener?.invoke(currentChat)
            }
        }

        fun setChatClickedListener(chatClickedListener: ChatClickedListener) {
            this.chatClickedListener = chatClickedListener
        }

        fun bind(chat: Chat) {
            currentChat = chat
            binding.run {
                val context = root.context
                fileLoadingJob?.cancel()
                if (chat.smallPhoto == null) {
                    imgChat.setImageResource(R.drawable.no_photo)
                } else {
                    fileLoadingJob = CoroutineScope(Dispatchers.IO).launch {

                        val bytes = telegramFileProvider
                            .getFileContent(chat.smallPhoto)

                        val bitmap = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.size
                        )

                        withContext(Dispatchers.Main) {
                            imgChat.setImageBitmap(bitmap)
                        }
                    }
                }
                tvTitle.text = chat.title
                tvLastMessage.text = if (chat.lastMessage == null)
                    ""
                else
                    when (chat.lastMessage.messageType) {
                        MessageType.Text -> chat.lastMessage.text
                        MessageType.Photo -> context.getString(R.string.photo_message)
                        MessageType.Unknown -> context.getString(
                            R.string.unsupported_message_type
                        )
                    }
            }
        }
    }

    private var items = emptyList<Chat>()
    private var chatClickedListener: ChatClickedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val binding = ItemChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )

        return ChatViewHolder(binding, telegramFileProvider).apply {
            setChatClickedListener {
                chatClickedListener?.invoke(it)
            }
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setChatClickedListener(chatClickedListener: ChatClickedListener) {
        this.chatClickedListener = chatClickedListener
    }

    fun updateItems(newItems: List<Chat>) {
        val diffCallback = SimpleDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}