package com.happybird.photosender.presentation.fragment_login.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.happybird.photosender.R
import com.happybird.photosender.databinding.FragmentLoginBinding
import com.happybird.photosender.framework.data.TelegramClient
import com.happybird.photosender.presentation.fragment_home.ui.FragmentHome
import com.happybird.photosender.presentation.fragment_login.screens.FragmentCode
import com.happybird.photosender.presentation.fragment_login.screens.FragmentNumber
import com.happybird.photosender.presentation.fragment_login.viewmodel.FragmentLoginViewModel

class FragmentLogin: Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: FragmentLoginViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(
                requireActivity().application
            )
        ).get(FragmentLoginViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }



    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) {
            Log.d("LoginFragment", it.toString())
            handleAuthState(it)
        }
    }

    private fun handleAuthState(auth: TelegramClient.Authentication) {
        when(auth) {
            TelegramClient.Authentication.WAIT_FOR_NUMBER -> {
                addFragment(FragmentNumber())
            }
            TelegramClient.Authentication.WAIT_FOR_CODE -> {
                addFragment(FragmentCode())
            }
            TelegramClient.Authentication.AUTHENTICATED -> {
                addFragment(FragmentHome())
            }
        }
    }

    private fun addFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.nav_host_login_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}