package com.ajrpachon.chatapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.request.crossfade
import com.ajrpachon.chatapp.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChatApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startKoin {
            androidContext(this@ChatApplication)
            modules(appModules)
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel("chat_messages") == null) {
            nm.createNotificationChannel(
                NotificationChannel("chat_messages", "Mensajes", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .crossfade(true)
            .build()
}
