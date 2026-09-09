package com.phoenix.phnx.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.CopyOnWriteArraySet

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
    private val listeners = CopyOnWriteArraySet<NetworkStateListener>()

    @Volatile
    private var state = NetworkState.DISCONNECTED

    fun observeConnection(listener: NetworkStateListener): NetworkState {
        listeners += listener
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

    fun stop(listener: NetworkStateListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) stop()
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
        listeners.clear()
    }

    private fun currentState(): NetworkState {
        return runCatching {
            val capabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return@runCatching NetworkState.DISCONNECTED
            stateFor(capabilities)
        }.getOrDefault(NetworkState.DISCONNECTED)
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
        if (changed || notifyWhenUnchanged) {
            listeners.forEach { listener ->
                runCatching { listener.onStateChanged(next) }
            }
        }
    }
}
