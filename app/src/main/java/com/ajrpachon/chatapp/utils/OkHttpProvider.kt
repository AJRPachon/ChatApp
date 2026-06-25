package com.ajrpachon.chatapp.utils

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient with certificate pinning for Supabase and LiveKit.
 *
 * Pins strategy:
 * - Supabase: Let's Encrypt R13 intermediate (more stable than leaf; rotates with LE infra)
 * - LiveKit:  leaf cert + ZeroSSL intermediate as backup
 *
 * When certs are rotated, update these pins and ship a new app version BEFORE the old certs expire.
 */
object OkHttpProvider {

    private val certificatePinner = CertificatePinner.Builder()
        // Supabase — project subdomain *.supabase.co (Let's Encrypt R13 intermediate)
        .add("*.supabase.co", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=")
        // LiveKit — leaf cert + ZeroSSL intermediate backup
        .add("*.livekit.cloud", "sha256/xbdEV2MzC3wuAL+M2CU3niDj279xZE/SH6IyrgqaoBs=")
        .add("*.livekit.cloud", "sha256/rnhtVs65ADYfQGtMuB0jq2kZwwHy6/iqnBiUKcK1m0Y=")
        .build()

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
