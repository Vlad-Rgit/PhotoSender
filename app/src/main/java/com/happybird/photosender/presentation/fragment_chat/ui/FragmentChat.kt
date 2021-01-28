package com.happybird.photosender.presentation.fragment_chat.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.happybird.photosender.R
import com.happybird.photosender.databinding.FragmentChatBinding
import com.happybird.photosender.domain.PhotoSend
import com.happybird.photosender.presentation.PaddingDivider
import com.happybird.photosender.presentation.fragment_chat.FragmentChatState
import com.happybird.photosender.presentation.fragment_chat.MessagesState
import com.happybird.photosender.presentation.fragment_chat.viewmodel.FragmentChatViewModel
import com.happybird.photosender.presentation.fragment_chat.viewmodel.FragmentChatViewModelFactory
import java.io.ByteArrayOutputStream


class FragmentChat: Fragment() {

    companion object {
        const val EXTRA_CHAT_ID = "com.happybird.photosender" +
                ".presentation.fragment_chat.ui." +
                "EXTRA_CHAT_ID"

        const val CODE_REQUEST_CAMERA_PERMISSION = 1
        const val CODE_TAKE_PHOTO = 2
        const val CODE_REQUEST_PHOTO_EDIT = 3
    }

    private lateinit var viewModel: FragmentChatViewModel
    private lateinit var binding: FragmentChatBinding
    private lateinit var messageAdapter: MessagesAdapter

    private var photoSend: PhotoSend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = requireArguments().getLong(EXTRA_CHAT_ID)

        viewModel = ViewModelProvider(
            this,
            FragmentChatViewModelFactory(
                requireActivity().application,
                chatId
            )
        ).get(FragmentChatViewModel::class.java)

        messageAdapter = MessagesAdapter(requireContext(), viewModel.currentUser)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentChatBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        observeViewModel()
        initButtons()
    }

    private fun startCamera() {
        if(ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        }
        else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.CAMERA),
            CODE_REQUEST_CAMERA_PERMISSION
        )
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(intent, CODE_TAKE_PHOTO)
        }
        catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "Камера не найдена",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK &&
                requestCode == CODE_TAKE_PHOTO &&
                data != null) {
            val bitmap = data.extras!!.get("data") as Bitmap

            val tempUri = getImageUri(requireContext(), bitmap)

            AlertDialog.Builder(requireContext())
                .setMessage(R.string.edit_photo)
                .setPositiveButton(R.string.yes) { d, i ->
                    val editIntent = Intent(Intent.ACTION_EDIT)
                    editIntent.setDataAndType(tempUri, "image/*")
                    editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivityForResult(
                        Intent.createChooser(editIntent, null),
                        CODE_REQUEST_PHOTO_EDIT
                    )
                }
                .setNegativeButton(R.string.no) { d, i ->
                    val path = getRealPathFromURI(tempUri)
                    photoSend = PhotoSend(
                        path,
                        bitmap.width,
                        bitmap.height
                    )
                    binding.img.setImageBitmap(bitmap)
                    binding.img.visibility = View.VISIBLE
                }.show()
        }
    }

    private fun sendImageForEdit() {

    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null)
        return Uri.parse(path)
    }

    fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (requireContext().getContentResolver() != null) {
            val cursor: Cursor? = requireContext().getContentResolver()
                .query(uri, null, null, null, null)

            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == CODE_REQUEST_CAMERA_PERMISSION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        }
    }

    private fun initButtons() {
        binding.run {
            btnSend.setOnClickListener {
                if(photoSend != null) {
                    viewModel.sendImage(photoSend!!)
                    photoSend = null
                    img.visibility = View.GONE
                }
                else {
                    val text = edMessageText.text.toString()
                    if (text.isNotBlank()) {
                        viewModel.sendTextMessage(text)
                    }
                }
            }
            btnPhoto.setOnClickListener {
                startCamera()
            }
        }
    }

    private fun initRecyclerView() {
        binding.rvMessages.run {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter

            val dividerSpaceHeight = requireContext()
                .resources
                .getDimensionPixelSize(
                    R.dimen.divider_space_height
                )

            addItemDecoration(
                PaddingDivider(
                    0,
                    dividerSpaceHeight,
                    0,
                    0
                )
            )
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) {
            render(it)
        }
    }

    private fun render(state: FragmentChatState) {
        renderList(state)
        renderLoading(state)
    }

    private fun renderList(state: FragmentChatState) {
        if(state is MessagesState) {
            messageAdapter.updateItems(state.list)
            val layoutManager = binding.rvMessages.layoutManager
                as LinearLayoutManager
            if(layoutManager.findLastVisibleItemPosition()
                == state.list.size - 2) {
                layoutManager.scrollToPosition(
                    state.list.size - 1
                )
            }
        }
    }

    private fun renderLoading(state: FragmentChatState) {

    }
}