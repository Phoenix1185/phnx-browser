package com.phoenix.phnx.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

enum class NetworkState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
}

fun interface NetworkStateListener {
    fun onStateChanged(state: NetworkState)
}

class NetworkMonitor(context: Context) {
    private val connectivityManager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var listener: NetworkStateListener? = null

    @Volatile
    private var state = NetworkState.DISCONNECTED

    fun observeConnection(listener: NetworkStateListener): NetworkState {
        this.listener = listener
        updateState(currentState(), notifyWhenUnchanged = true)
        if (callback == null) {
            callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateState(NetworkState.CONNECTING)
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    updateState(stateFor(capabilities))
                }

                override fun onLost(network: Network) {
                    updateState(currentState())
                }

                override fun onUnavailable() {
                    updateState(NetworkState.DISCONNECTED)
                }
            }
            runCatching { connectivityManager.registerDefaultNetworkCallback(callback!!) }
                .onFailure {
                    callback = null
                    updateState(NetworkState.DISCONNECTED)
                }
        }
        return state
    }

    fun detectNetworkChanges(): NetworkState {
        val current = currentState()
        updateState(current)
        return current
    }

    fun reportConnectionState(): NetworkState = state

    fun stop() {
        callback?.let { runCatching { connectivityManager.unregisterNetworkCallback(it) } }
        callback = null
        listener = null
    }

    private fun currentState(): NetworkState {
        val capabilities = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return NetworkState.DISCONNECTED
        return stateFor(capabilities)
    }

    private fun stateFor(capabilities: NetworkCapabilities): NetworkState =
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkState.CONNECTED
        } else {
            NetworkState.CONNECTING
        }

    private fun updateState(next: NetworkState, notifyWhenUnchanged: Boolean = false) {
        val changed = state != next
        state = next
        if (changed || notifyWhenUnchanged) listener?.onStateChanged(next)
    }
}
