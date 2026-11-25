package com.ajrpachon.chatapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(private val context: Context) {

    /**
     * True while at least one network has validated internet access.
     *
     * Tracks the set of validated networks from the callbacks themselves instead of re-querying
     * `activeNetwork` in `onLost`: during `onLost` the system can still report the network being
     * lost as the active one, so turning on airplane mode (Wi-Fi and mobile data both dropping)
     * used to leave the app stuck "online" and the offline banner never showed.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val validated = mutableSetOf<Network>()

        fun NetworkCapabilities.hasValidatedInternet(): Boolean =
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        cm.activeNetwork?.let { network ->
            if (cm.getNetworkCapabilities(network)?.hasValidatedInternet() == true) validated += network
        }
        trySend(validated.isNotEmpty())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                synchronized(validated) {
                    if (caps.hasValidatedInternet()) validated += network else validated -= network
                    trySend(validated.isNotEmpty())
                }
            }

            override fun onLost(network: Network) {
                synchronized(validated) {
                    validated -= network
                    trySend(validated.isNotEmpty())
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
