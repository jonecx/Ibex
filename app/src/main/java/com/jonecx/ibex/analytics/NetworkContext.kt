package com.jonecx.ibex.analytics

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Current network transport for telemetry enrichment. Neither PostHog nor the manual
 * Axiom ingest captures this, and it is the most useful QoE slice (cellular vs wifi).
 */
object NetworkContext {

    /** [wire] is the value stamped onto telemetry rows and super properties. */
    enum class NetworkType(val wire: String) {
        WIFI("wifi"),
        CELLULAR("cellular"),
        ETHERNET("ethernet"),
        OTHER("other"),
        NONE("none"),
        UNKNOWN("unknown"),
    }

    private var connectivityManager: ConnectivityManager? = null

    fun init(context: Context, onChange: ((NetworkType) -> Unit)? = null) {
        connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        if (onChange == null) return
        connectivityManager?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    onChange(type())
                }

                override fun onLost(network: Network) {
                    onChange(type())
                }
            },
        )
    }

    fun type(): NetworkType {
        val manager = connectivityManager ?: return NetworkType.UNKNOWN
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }
}
