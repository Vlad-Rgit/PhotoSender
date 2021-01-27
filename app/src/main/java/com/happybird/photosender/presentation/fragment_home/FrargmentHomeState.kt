package com.happybird.photosender.presentation.fragment_home

import com.happybird.photosender.domain.Chat
import org.drinkless.td.libcore.telegram.TdApi

sealed class FragmentHomeState

object LoadingState: FragmentHomeState()
data class ListState(val list: List<Chat>): FragmentHomeState()