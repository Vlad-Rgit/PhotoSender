package com.happybird.photosender.presentation.fragment_chat.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.PhotoSenderApp
import com.happybird.photosender.R
import com.happybird.photosender.databinding.FragmentChatBinding
import com.happybird.photosender.domain.PhotoSend
import com.happybird.photosender.framework.data.TelegramFileProvider
import com.happybird.photosender.framework.utils.allocateImageFile
import com.happybird.photosender.presentation.PaddingDivider
import com.happybird.photosender.presentation.fragment_chat.FragmentChatState
import com.happybird.photosender.presentation.fragment_chat.MessagesState
import com.happybird.photosender.presentation.fragment_chat.viewmodel.FragmentChatViewModel
import com.happybird.photosender.presentation.fragment_chat.viewmodel.FragmentChatViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


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
    private lateinit var telegramFileProvider: TelegramFileProvider


    private var photoSend: PhotoSend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = requireArguments().getLong(EXTRA_CHAT_ID)

        val photoApp = requireActivity().application as PhotoSenderApp

        telegramFileProvider = photoApp.appComponent.getTelegramFileProvider()

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
        initToolbar()
        initRecyclerView()
        observeViewModel()
        initButtons()
    }

    private fun initToolbar() {
        val chat = viewModel.getChatInfo()

        binding.run {
            tvTitle.text = chat.title
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            if(chat.smallPhoto == null) {
                imgChat.setImageResource(R.drawable.no_photo)
            }
            else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val path = telegramFileProvider.getFilePath(chat.smallPhoto)
                    val bitmap = BitmapFactory.decodeFile(path)
                    withContext(Dispatchers.Main) {
                        imgChat.setImageBitmap(bitmap)
                    }
                }
            }
        }
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
        val path = allocateImageFile(requireContext())
        photoSend = PhotoSend(path)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                requireContext(),
                requireContext()
                    .applicationContext
                    .packageName + ".provider",
                File(path)
            ))
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
            requestCode == CODE_TAKE_PHOTO) {
            val toSend = photoSend!!

            val bitmap = BitmapFactory.decodeFile(toSend.path)
            toSend.width = bitmap.width
            toSend.height = bitmap.height

            AlertDialog.Builder(requireContext())
                .setMessage(R.string.edit_photo)
                .setPositiveButton(R.string.yes) { d, i ->
                    val editIntent = Intent(Intent.ACTION_EDIT)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext()
                            .applicationContext
                            .packageName + ".provider",
                        File(toSend.path)
                    )
                    editIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    editIntent.setDataAndType(uri, "image/*")
                    editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    editIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivityForResult(
                        editIntent,
                        CODE_REQUEST_PHOTO_EDIT
                    )
                }
                .setNegativeButton(R.string.no) { d, i ->
                    prepareImageToSend(bitmap)
                }.show()
        }
        else if(requestCode == CODE_REQUEST_PHOTO_EDIT) {
            prepareImageToSend(BitmapFactory
                .decodeFile(photoSend!!.path))
        }
    }

    private fun prepareImageToSend(bitmap: Bitmap) {
        binding.img.setImageBitmap(bitmap)
        binding.img.visibility = View.VISIBLE
    }

    private fun sendImageForEdit() {

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
                        edMessageText.setText("")
                    }
                }
            }
            btnPhoto.setOnClickListener {
                startCamera()
            }
        }
    }

    private var isLoading = false

    private fun initRecyclerView() {
        binding.rvMessages.run {
            val linearLayout = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            layoutManager = linearLayout
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
            isLoading = false
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