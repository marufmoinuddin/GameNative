package app.gamenative.linux.infra.network

import kotlin.test.Test
import kotlin.test.assertEquals

class LinuxNetworkStateServiceTest {
    @Test
    fun returnsOfflineWhenNoActiveInterfaces() {
        val service = LinuxNetworkStateService(
            interfaceCountProvider = { 0 },
            reachabilityProbe = { true },
        )

        assertEquals(NetworkState.OFFLINE, service.currentState())
    }

    @Test
    fun returnsOnlineWhenInterfacesAndProbeSucceed() {
        val service = LinuxNetworkStateService(
            interfaceCountProvider = { 2 },
            reachabilityProbe = { true },
        )

        assertEquals(NetworkState.ONLINE, service.currentState())
    }

    @Test
    fun returnsDegradedWhenInterfacesExistButProbeFails() {
        val service = LinuxNetworkStateService(
            interfaceCountProvider = { 1 },
            reachabilityProbe = { false },
        )

        assertEquals(NetworkState.DEGRADED, service.currentState())
    }

    @Test
    fun returnsUnknownWhenInterfacesCannotBeDetermined() {
        val service = LinuxNetworkStateService(
            interfaceCountProvider = { -1 },
            reachabilityProbe = { true },
        )

        assertEquals(NetworkState.UNKNOWN, service.currentState())
    }
}
