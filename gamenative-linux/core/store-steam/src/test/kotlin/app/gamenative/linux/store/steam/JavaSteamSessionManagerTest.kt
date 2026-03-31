package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaSteamSessionManagerTest {
    @Test
    fun authenticatesWhenGatewayConnectAndLoginSucceed() = runBlocking {
        val gateway = FakeSteamAuthGateway(
            connectResult = GatewayResult(success = true, message = "Connected"),
            loginResult = GatewayResult(success = true, accountName = "alice", message = "OK"),
        )
        val manager = JavaSteamSessionManager(gateway)

        val snapshot = manager.login("alice", "secret")

        assertEquals(SteamSessionState.AUTHENTICATED, snapshot.state)
        assertEquals("alice", snapshot.accountName)
        assertEquals(listOf("connect", "login:alice"), gateway.calls)
    }

    @Test
    fun failsWhenGatewayConnectionFails() = runBlocking {
        val gateway = FakeSteamAuthGateway(
            connectResult = GatewayResult(success = false, message = "Network unavailable"),
            loginResult = GatewayResult(success = true, accountName = "alice"),
        )
        val manager = JavaSteamSessionManager(gateway)

        val snapshot = manager.login("alice", "secret")

        assertEquals(SteamSessionState.FAILED, snapshot.state)
        assertEquals("Network unavailable", snapshot.message)
        assertEquals(listOf("connect"), gateway.calls)
    }

    @Test
    fun failsWhenGatewayLoginFails() = runBlocking {
        val gateway = FakeSteamAuthGateway(
            connectResult = GatewayResult(success = true),
            loginResult = GatewayResult(success = false, message = "Invalid credentials"),
        )
        val manager = JavaSteamSessionManager(gateway)

        val snapshot = manager.login("alice", "bad")

        assertEquals(SteamSessionState.FAILED, snapshot.state)
        assertEquals("Invalid credentials", snapshot.message)
        assertEquals(listOf("connect", "login:alice"), gateway.calls)
    }

    @Test
    fun rejectsBlankCredentialsBeforeGatewayCall() = runBlocking {
        val gateway = FakeSteamAuthGateway(
            connectResult = GatewayResult(success = true),
            loginResult = GatewayResult(success = true, accountName = "alice"),
        )
        val manager = JavaSteamSessionManager(gateway)

        val snapshot = manager.login("", "")

        assertEquals(SteamSessionState.FAILED, snapshot.state)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun logoutDisconnectsGatewayAndClearsSnapshot() = runBlocking {
        val gateway = FakeSteamAuthGateway(
            connectResult = GatewayResult(success = true),
            loginResult = GatewayResult(success = true, accountName = "alice"),
        )
        val manager = JavaSteamSessionManager(gateway)
        manager.login("alice", "secret")

        manager.logout()

        assertEquals(SteamSessionState.DISCONNECTED, manager.currentSession().state)
        assertEquals(listOf("connect", "login:alice", "disconnect"), gateway.calls)
    }
}

private class FakeSteamAuthGateway(
    private val connectResult: GatewayResult,
    private val loginResult: GatewayResult,
) : SteamAuthGateway {
    val calls = mutableListOf<String>()

    override suspend fun connect(): GatewayResult {
        calls += "connect"
        return connectResult
    }

    override suspend fun login(username: String, password: String): GatewayResult {
        calls += "login:$username"
        return loginResult
    }

    override suspend fun disconnect() {
        calls += "disconnect"
    }
}
