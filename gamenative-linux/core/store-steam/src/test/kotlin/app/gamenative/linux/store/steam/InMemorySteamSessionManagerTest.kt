package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemorySteamSessionManagerTest {
    @Test
    fun authenticatesWhenCredentialsProvided() = runBlocking {
        val manager = InMemorySteamSessionManager()

        val snapshot = manager.login("alice", "secret")

        assertEquals(SteamSessionState.AUTHENTICATED, snapshot.state)
        assertEquals("alice", snapshot.accountName)
    }

    @Test
    fun failsWhenCredentialsMissing() = runBlocking {
        val manager = InMemorySteamSessionManager()

        val snapshot = manager.login("", "")

        assertEquals(SteamSessionState.FAILED, snapshot.state)
    }

    @Test
    fun clearsSessionOnLogout() = runBlocking {
        val manager = InMemorySteamSessionManager()
        manager.login("alice", "secret")

        manager.logout()

        assertEquals(SteamSessionState.DISCONNECTED, manager.currentSession().state)
    }
}
