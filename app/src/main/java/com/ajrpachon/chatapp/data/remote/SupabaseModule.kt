package com.ajrpachon.chatapp.data.remote

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.remote.session.AndroidSessionManager
import com.ajrpachon.chatapp.utils.OkHttpProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * The Supabase client. It lives here, with the rest of the code that uses the SDK, so nothing
 * outside `data/remote` imports `io.github.jan.supabase`; every consumer goes through a
 * `*RemoteSource`.
 */
val supabaseModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            httpEngine = OkHttp.create { preconfigured = OkHttpProvider.client }
            install(Auth) {
                sessionManager = AndroidSessionManager(androidContext())
                scheme = "com.ajrpachon.chatapp"
                host = "auth-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
}
