package com.happybird.photosender.presentation.fragment_home.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.databinding.ItemChatBinding
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.framework.data.TelegramFileProvider
import com.happybird.photosender.framework.utils.SimpleDiffCallback
import kotlinx.coroutines.*

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

            fun bind(chat: Chat) {
                currentChat = chat
                binding.run {
                    tvTitle.text = chat.title
                    fileLoadingJob?.cancel()
                    chat.smallPhoto?.let {
                        fileLoadingJob = CoroutineScope(Dispatchers.IO).launch {
                            val bytes = telegramFileProvider
                                    .getFileContent(chat.smallPhoto)
                            val bitmap = BitmapFactory.decodeByteArray(
                                    bytes, 0, bytes.size

                            )

                            withContext(Dispatchers.Main) {
                                imgChat.setImageBitmap()
                            }
                        }
                    }
                }
            }
    }

    private var items = emptyList<Chat>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val binding = ItemChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )

        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<Chat>) {
        val diffCallback = SimpleDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}