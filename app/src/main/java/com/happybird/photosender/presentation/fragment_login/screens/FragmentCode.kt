package com.happybird.photosender.presentation.fragment_login.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.happybird.photosender.databinding.FragmentLoginCodeBinding
import com.happybird.photosender.framework.TelegramClientException
import com.happybird.photosender.presentation.fragment_login.SetCodeIntent
import com.happybird.photosender.presentation.fragment_login.viewmodel.FragmentLoginViewModel
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.TimeoutException

class FragmentCode: Fragment() {

    private lateinit var binding: FragmentLoginCodeBinding
    private lateinit var viewModel: FragmentLoginViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
                requireParentFragment(),
                ViewModelProvider.AndroidViewModelFactory(
                        requireActivity().application
                )
        ).get(FragmentLoginViewModel::class.java)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginCodeBinding.inflate(
                inflater,
                container,
                false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        observeViewModel()
    }

    private fun initButtons() {
        binding.btnForward.setOnClickListener {
            binding.btnForward.visibility = View.INVISIBLE
            binding.progressCircular.visibility = View.VISIBLE
            sendCode(binding.edCode.text.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.exceptionsState.observe(viewLifecycleOwner) {
            binding.btnForward.visibility = View.VISIBLE
            binding.progressCircular.visibility = View.GONE
            when(it) {
                is TimeoutException -> showMessage("Ошибка соединения")
                is TelegramClientException -> showMessage("${it.code}: ${it.message}")
            }
        }
    }

    private fun showMessage(text: String) {
        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sendCode(code: String) {
        viewModel.sendIntent(SetCodeIntent(code))
    }
}