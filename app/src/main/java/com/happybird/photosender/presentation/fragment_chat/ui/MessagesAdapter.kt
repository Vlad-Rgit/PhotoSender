package com.happybird.photosender.presentation.fragment_chat.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.R
import com.happybird.photosender.databinding.ItemMessageBinding
import com.happybird.photosender.domain.Message
import com.happybird.photosender.domain.MessageType
import com.happybird.photosender.domain.User
import com.happybird.photosender.framework.data.TelegramFileProvider
import com.happybird.photosender.framework.utils.SimpleDiffCallback
import kotlinx.coroutines.*

class MessagesAdapter(
    private val context: Context,
    private val currentUser: User
)
    : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    class MessageViewHolder(
        private val binding: ItemMessageBinding,
        private val currentUser: User,
        private val telegramFileProvider: TelegramFileProvider
    )
        : RecyclerView.ViewHolder(binding.root) {

        private var photoLoadJob: Job? = null

        fun bind(message: Message) {
            binding.run {

                photoLoadJob?.cancel()

                val layoutParams = messageHost.layoutParams
                    as FrameLayout.LayoutParams

                if(message.senderId == currentUser.id.toLong()) {
                    layoutParams.gravity = Gravity.END
                }
                else {
                    layoutParams.gravity = Gravity.START
                }

                messageHost.layoutParams = layoutParams

                if(message.messageType == MessageType.Text) {
                    tvMessage.visibility = View.VISIBLE
                    imgMessage.visibility = View.GONE
                    tvMessage.text = message.text
                }
                else if(message.messageType == MessageType.Photo) {
                    tvMessage.visibility = View.GONE
                    imgMessage.visibility = View.VISIBLE

                    photoLoadJob = CoroutineScope(Dispatchers.IO).launch {
                        val bytes = telegramFileProvider.getFileContent(
                            message.photo!!
                        )
                        val bitmap = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.size
                        )

                        withContext(Dispatchers.Main) {
                            imgMessage.setImageBitmap(bitmap)
                        }
                    }
                }
                else {
                    tvMessage.visibility = View.VISIBLE
                    imgMessage.visibility = View.GONE
                    tvMessage.setText(R.string.unsupported_message_type)
                }
            }
        }

    }



    private var items: List<Message> = emptyList()
    private val telegramFileProvider: TelegramFileProvider

    init {
        val photoApp = context.applicationContext as PhotoSenderApp
        telegramFileProvider = photoApp.appComponent.getTelegramFileProvider()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MessageViewHolder(binding, currentUser, telegramFileProvider)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<Message>) {
        val diffCallback = SimpleDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}