package com.ajrpachon.chatapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.StrictMode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.video.VideoFrameDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import com.ajrpachon.chatapp.data.remote.source.GiphyRemoteSource
import com.ajrpachon.chatapp.di.appModules
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.OkHttpProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class ChatApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
        createNotificationChannel()
        startKoin {
            androidContext(this@ChatApplication)
            modules(appModules)
        }
        initCrashReporting()
        configureAnalyticsCollection()
    }

    // Wires AppLogger.e(...) to Crashlytics app-wide (see AppLogger.init). Wrapped defensively
    // for the same reason as configureAnalyticsCollection() below.
    private fun initCrashReporting() {
        runCatching {
            AppLogger.init(get<CrashReporter>())
        }.onFailure { e ->
            AppLogger.w("ChatApplication", "Crash reporter init failed — AppLogger.e() won't reach Crashlytics", e)
        }
    }

    // Off in debug/local builds — only release builds report real crashes/events.
    // Wrapped defensively: FirebaseApp isn't initialized under Robolectric/unit tests
    // (and may not be on a device without Play Services) — that must never crash startup.
    private fun configureAnalyticsCollection() {
        runCatching {
            val collectionEnabled = !BuildConfig.DEBUG
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(collectionEnabled)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(collectionEnabled)
        }.onFailure { e ->
            AppLogger.w("ChatApplication", "Firebase analytics/crashlytics collection setup failed", e)
        }
    }

    override fun onTerminate() {
        // GiphyRemoteSource owns a process-wide HttpClient reused across every GIF picker
        // opening, so it must NOT be closed when the picker closes. onTerminate() is only
        // called by the emulator (never on real devices), but it's the best-effort hook we
        // have to release the underlying OkHttp connection pool for this shared client.
        // getOrNull() guards against Koin already having been stopped (e.g. in tests).
        GlobalContext.getOrNull()?.get<GiphyRemoteSource>()?.close()
        super.onTerminate()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
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
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = OkHttpProvider.client))
                add(AnimatedImageDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .build()
}
