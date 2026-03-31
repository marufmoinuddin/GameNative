package app.gamenative.linux.infra.network

import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class LinuxNetworkStateService(
    private val interfaceCountProvider: () -> Int = ::defaultInterfaceCount,
    private val reachabilityProbe: () -> Boolean = ::defaultReachabilityProbe,
) : NetworkStateService {
    override fun currentState(): NetworkState {
        val activeInterfaceCount = interfaceCountProvider()

        if (activeInterfaceCount < 0) {
            return NetworkState.UNKNOWN
        }
        if (activeInterfaceCount == 0) {
            return NetworkState.OFFLINE
        }

        return if (reachabilityProbe()) {
            NetworkState.ONLINE
        } else {
            NetworkState.DEGRADED
        }
    }

    companion object {
        private fun defaultInterfaceCount(): Int {
            return try {
                NetworkInterface.getNetworkInterfaces()
                    .toList()
                    .count { it.isUp && !it.isLoopback }
            } catch (_: Exception) {
                -1
            }
        }

        private fun defaultReachabilityProbe(): Boolean {
            return try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 1500)
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
