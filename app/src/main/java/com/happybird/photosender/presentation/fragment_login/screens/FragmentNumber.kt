package com.happybird.photosender.presentation.fragment_login.screens

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.happybird.photosender.databinding.FragmentLoginBinding
import com.happybird.photosender.databinding.FragmentLoginNumberBinding
import com.happybird.photosender.presentation.fragment_login.SetNumberIntent
import com.happybird.photosender.presentation.fragment_login.viewmodel.FragmentLoginViewModel
import org.drinkless.td.libcore.telegram.TdApi

class FragmentNumber: Fragment() {

    private lateinit var viewModel: FragmentLoginViewModel
    private lateinit var binding: FragmentLoginNumberBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            requireParentFragment(),
            ViewModelProvider.AndroidViewModelFactory(
                requireContext().applicationContext as Application
            )
        ).get(FragmentLoginViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLoginNumberBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    private fun initButtons() {
        binding.btnForward.setOnClickListener {
            sendNumber(binding.edPhoneNumber.text.toString())
        }
    }

    private fun sendNumber(phoneNumber: String) {
        viewModel.sendIntent(SetNumberIntent(phoneNumber))
    }
}