package com.happybird.photosender.presentation.fragment_chat

import com.happybird.photosender.domain.Message

sealed class FragmentChatState

object LoadingState: FragmentChatState()

class MessagesState(val list: List<Message>): FragmentChatState()