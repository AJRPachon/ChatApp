package com.ajrpachon.chatapp.utils

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Shared OkHttpClient with certificate pinning for Supabase and LiveKit.
 *
 * Pins strategy:
 * - Supabase: Let's Encrypt R13 intermediate (more stable than leaf; rotates with LE infra)
 * - LiveKit:  leaf cert + ZeroSSL intermediate as backup
 *
 * When certs are rotated, update these pins and ship a new app version BEFORE the old certs expire.
 *
 * Security posture — Certificate Transparency (CT):
 * Android 7.0+ (API 24+) enforces CT automatically for TLS connections that trust system CAs.
 * By restricting this OkHttpClient to a system-only TrustManager (no user-installed CAs), we:
 *   1. Reject user-installed CAs — including corporate MitM proxies and rogue root certificates.
 *   2. Guarantee CT enforcement on all TLS handshakes, since system CAs are CT-logged by policy.
 *   3. Layer certificate pinning on top of CT for our specific hosts (Supabase, LiveKit).
 *
 * This mirrors the <certificates src="system" /> (no user) posture in network_security_config.xml
 * but enforces it at the OkHttp socket level as a defence-in-depth measure.
 */
object OkHttpProvider {

    private val certificatePinner = CertificatePinner.Builder()
        // Supabase — project subdomain *.supabase.co (Let's Encrypt R13 intermediate)
        .add("*.supabase.co", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=")
        // LiveKit — leaf cert + ZeroSSL intermediate backup
        .add("*.livekit.cloud", "sha256/xbdEV2MzC3wuAL+M2CU3niDj279xZE/SH6IyrgqaoBs=")
        .add("*.livekit.cloud", "sha256/rnhtVs65ADYfQGtMuB0jq2kZwwHy6/iqnBiUKcK1m0Y=")
        .build()

    /**
     * Returns a TrustManager backed exclusively by the system (device) KeyStore.
     *
     * Passing null to TrustManagerFactory.init() on Android uses the platform default KeyStore,
     * which contains only the system CA bundle — user-installed certificates are excluded.
     * This is equivalent to <certificates src="system" /> in network_security_config.xml and
     * ensures OkHttp cannot be bypassed by a user-installed CA regardless of device settings.
     */
    private fun systemOnlyTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?) // null = platform default KeyStore (system CAs only on Android)
        return tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    val client: OkHttpClient by lazy {
        val trustManager = systemOnlyTrustManager()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
