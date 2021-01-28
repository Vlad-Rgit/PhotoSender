package com.happybird.photosender.presentation.fragment_edit_photo

import android.R
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.happybird.photosender.databinding.FragmentEditPhotoBinding
import ja.burhanrashid52.photoeditor.PhotoEditor


class FragmentEditPhoto: Fragment() {

    companion object {
        const val EXTRA_PHOTO_URI = "com.happybird.photosender" +
                ".presentation.fragment_edit_photo" +
                ".EXTRA_PHOTO_URI"
    }

    private lateinit var binding: FragmentEditPhotoBinding
    private lateinit var photoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoUri = requireArguments().get(EXTRA_PHOTO_URI) as Uri
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditPhotoBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPhotoEditor()
    }

    private fun initPhotoEditor() {
        binding.photoEditor.source.setImageURI(photoUri)
        val photoEditor = PhotoEditor.Builder(requireContext(), binding.photoEditor)
            .setPinchTextScalable(true)
            .build()
    }
}