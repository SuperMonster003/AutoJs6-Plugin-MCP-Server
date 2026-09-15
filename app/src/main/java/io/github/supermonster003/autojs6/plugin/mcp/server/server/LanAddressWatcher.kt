package io.github.supermonster003.autojs6.plugin.mcp.server.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Tracks the device's routable IPv4 addresses for the LAN allow list (roadmap P2.1): the current
 * set is read from the network interfaces, and a connectivity callback re-reads it whenever a
 * network appears, disappears, or changes its link properties. [onChanged] runs on the
 * connectivity callback thread only when the set actually changed.
 */
class LanAddressWatcher(context: Context, private val onChanged: () -> Unit) {

    private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var callback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    var addresses: Set<String> = emptySet()
        private set

    @Synchronized
    fun start() {
        refresh()
        val manager = connectivity ?: return
        if (callback != null) return
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = notifyIfChanged()
            override fun onLost(network: Network) = notifyIfChanged()
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = notifyIfChanged()
        }
        runCatching { manager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback) }
            .onSuccess { callback = networkCallback }
    }

    @Synchronized
    fun stop() {
        val registered = callback ?: return
        callback = null
        runCatching { connectivity?.unregisterNetworkCallback(registered) }
    }

    /** Re-reads the interfaces; true when the address set changed. */
    fun refresh(): Boolean {
        val next = currentAddresses()
        val changed = next != addresses
        addresses = next
        return changed
    }

    private fun notifyIfChanged() {
        if (refresh()) onChanged()
    }

    companion object {

        /**
         * Non-loopback, non-link-local IPv4 addresses of the local network interfaces that are up.
         * Point-to-point links (VPN tunnels) and interfaces without multicast (cellular data) are
         * not local networks: their addresses are neither listed nor added to the Host allow list.
         */
        fun currentAddresses(): Set<String> = runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { runCatching { it.isUp && !it.isLoopback && !it.isPointToPoint && it.supportsMulticast() }.getOrDefault(false) }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isAnyLocalAddress }
                .mapNotNull { it.hostAddress }
                .toSet()
        }.getOrDefault(emptySet())
    }
}
