package com.happybird.photosender.framework.di.component

import android.content.Context
import com.happybird.photosender.framework.data.TelegramFileProvider
import com.happybird.photosender.presentation.fragment_home.viewmodel.FragmentHomeViewModel
import com.happybird.photosender.presentation.fragment_login.viewmodel.FragmentLoginViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Component
@Singleton
interface AppComponent {

    fun inject(item: FragmentLoginViewModel)
    fun inject(item: FragmentHomeViewModel)

    fun getTelegramFileProvider(): TelegramFileProvider

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }
}