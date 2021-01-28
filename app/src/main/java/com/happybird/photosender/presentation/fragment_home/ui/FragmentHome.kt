package com.happybird.photosender.presentation.fragment_home.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happybird.photosender.R
import com.happybird.photosender.databinding.FragmentHomeBinding
import com.happybird.photosender.domain.Chat
import com.happybird.photosender.presentation.fragment_chat.ui.FragmentChat
import com.happybird.photosender.presentation.fragment_home.FragmentHomeState
import com.happybird.photosender.presentation.fragment_home.ListState
import com.happybird.photosender.presentation.fragment_home.LoadingState
import com.happybird.photosender.presentation.fragment_home.viewmodel.FragmentHomeViewModel

class FragmentHome: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: FragmentHomeViewModel
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(
                        requireActivity().application
                )
        ).get(FragmentHomeViewModel::class.java)

        chatAdapter = ChatAdapter(requireContext()).apply {
            setChatClickedListener {
                onChatClicked(it)
            }
        }
    }

    private fun onChatClicked(chat: Chat) {
        findNavController().navigate(
            R.id.action_fragmentLogin_to_fragmentChat,
            bundleOf(
                FragmentChat.EXTRA_CHAT_ID to chat.id
            )
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {

        binding = FragmentHomeBinding.inflate(
                inflater,
                container,
                false
        )

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        initRecyclerView()
    }



    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) {
            render(it)
        }
    }

    private fun initRecyclerView() {
        binding.rvChats.run {
            setHasFixedSize(true)
            val linearManager = LinearLayoutManager(requireContext())
            layoutManager = linearManager
            adapter = chatAdapter

            addItemDecoration(DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            ))
        }
    }

    private fun render(state: FragmentHomeState) {
        renderLoadingState(state)
        renderListState(state)
    }

    private fun renderLoadingState(state: FragmentHomeState) {
        if(state is LoadingState) {
            binding.progressCircular.visibility = View.VISIBLE
            binding.rvChats.visibility = View.GONE
        }
        else {
            binding.progressCircular.visibility = View.GONE
            binding.rvChats.visibility = View.VISIBLE
        }
    }

    private fun renderListState(state: FragmentHomeState) {
        if(state is ListState) {
            Log.i("State", "List received")
            chatAdapter.updateItems(state.list)
        }
    }

}