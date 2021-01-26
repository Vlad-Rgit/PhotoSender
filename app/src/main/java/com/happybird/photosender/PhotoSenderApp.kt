package com.happybird.photosender

import android.app.Application
import com.happybird.photosender.framework.di.component.AppComponent
import com.happybird.photosender.framework.di.component.DaggerAppComponent

class PhotoSenderApp: Application() {

    private lateinit var _appComponent: AppComponent

    val appComponent
        get() = _appComponent

    override fun onCreate() {
        super.onCreate()
        _appComponent = DaggerAppComponent
            .builder()
            .context(applicationContext)
            .build()
    }
}