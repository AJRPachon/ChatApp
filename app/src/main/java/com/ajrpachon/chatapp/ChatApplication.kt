package com.ajrpachon.chatapp

import android.app.Application
import com.ajrpachon.chatapp.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ChatApplication)
            modules(appModules)
        }
    }
}
