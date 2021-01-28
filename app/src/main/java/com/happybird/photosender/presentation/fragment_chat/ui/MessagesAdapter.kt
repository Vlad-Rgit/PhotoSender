package com.happybird.photosender.presentation.fragment_chat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

        fun decodeSampledBitmapFromFile(
            path: String,
            reqWidth: Int,
            reqHeight: Int
        ): Bitmap {
            // First decode with inJustDecodeBounds=true to check dimensions
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, this)

                // Calculate inSampleSize
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false

                BitmapFactory.decodeFile(path, this)
            }
        }

        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            // Raw height and width of image
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        fun bind(message: Message) {
            binding.run {

                binding.imgMessage.setImageBitmap(null)

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

                    binding.progressCircular.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE
                    imgMessage.visibility = View.VISIBLE

                    photoLoadJob = CoroutineScope(Dispatchers.IO).launch {

                        val path = telegramFileProvider.getFilePath(
                            message.photo!!
                        )

                        val bitmap = decodeSampledBitmapFromFile(
                            path,
                            imgMessage.maxWidth,
                            imgMessage.maxHeight
                        )

                        withContext(Dispatchers.Main) {
                            binding.imgMessage.setImageBitmap(bitmap)
                            binding.progressCircular.visibility = View.GONE
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