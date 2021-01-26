package com.happybird.photosender.presentation.fragment_login

sealed class LoginIntent

data class SetNumberIntent(val number: String): LoginIntent()
data class SetCodeIntent(val code: String): LoginIntent()