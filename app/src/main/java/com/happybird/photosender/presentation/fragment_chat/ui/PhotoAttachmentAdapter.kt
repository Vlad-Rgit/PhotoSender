package com.happybird.photosender.presentation.fragment_chat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.databinding.ItemPhotoAttachmentBinding
import com.happybird.photosender.domain.PhotoSend
import com.happybird.photosender.framework.utils.SimpleDiffCallback
import com.happybird.photosender.framework.utils.decodeSampledBitmapFromFile


typealias OnPhotoClicked = (PhotoSend) -> Unit


class PhotoAttachmentAdapter:
        RecyclerView.Adapter<PhotoAttachmentAdapter.PhotoAttachmentViewHolder>() {

    class PhotoAttachmentViewHolder(val binding: ItemPhotoAttachmentBinding)
        : RecyclerView.ViewHolder(binding.root) {

        private lateinit var currentPhotoSend: PhotoSend
        private var onRemoveButtonClickedListener: OnPhotoClicked? = null
        private var onPhotoClicked: OnPhotoClicked? = null


        init {
            binding.btnRemove.setOnClickListener {
                onRemoveButtonClickedListener?.invoke(currentPhotoSend)
            }
            binding.img.setOnClickListener {
                onPhotoClicked?.invoke(currentPhotoSend)
            }
        }


        fun bind(photoSend: PhotoSend) {
            currentPhotoSend = photoSend

            val bitmap = decodeSampledBitmapFromFile(
                    photoSend.path,
                    binding.img.maxWidth,
                    binding.img.maxHeight
            )

            binding.img.setImageBitmap(bitmap)
        }

        fun setOnRemoveClickedListener(listener: OnPhotoClicked) {
            this.onRemoveButtonClickedListener = listener
        }

        fun setOnPhotoClicked(listener: OnPhotoClicked) {
            this.onPhotoClicked = listener
        }
    }

    private var items = emptyList<PhotoSend>()
    private var onRemoveButtonClickedListener: OnPhotoClicked? = null
    private var onPhotoClicked: OnPhotoClicked? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoAttachmentViewHolder {
        val binding = ItemPhotoAttachmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )
        return PhotoAttachmentViewHolder(binding).apply {
            setOnRemoveClickedListener {
                onRemoveButtonClickedListener?.invoke(it)
            }
            setOnPhotoClicked {
                onPhotoClicked?.invoke(it)
            }
        }
    }

    override fun onBindViewHolder(holder: PhotoAttachmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setOnRemoveClickedListener(listener: OnPhotoClicked) {
        this.onRemoveButtonClickedListener = listener
    }

    fun setOnPhotoClicked(listener: OnPhotoClicked) {
        this.onPhotoClicked = listener
    }

    fun setItems(newItems: List<PhotoSend>) {
        val diffCallback = SimpleDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

}