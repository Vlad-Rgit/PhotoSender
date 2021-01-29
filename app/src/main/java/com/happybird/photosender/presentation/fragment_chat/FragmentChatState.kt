package com.happybird.photosender.presentation.fragment_chat

import com.happybird.photosender.domain.Message
import com.happybird.photosender.domain.PhotoSend

sealed class FragmentChatState

object LoadingState: FragmentChatState()

class MessagesState(val list: List<Message>): FragmentChatState()
class AttachmentsState(val list: List<PhotoSend>): FragmentChatState()