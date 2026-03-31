package app.gamenative.linux.desktop.shell

import app.gamenative.linux.store.steam.SteamSessionState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopSteamGatewayConfigTest {
    @Test
    fun parsesGatewayModeWithPrototypeFallback() {
        assertEquals(DesktopSteamGatewayMode.PROTOTYPE, DesktopSteamGatewayMode.fromRaw(null))
        assertEquals(DesktopSteamGatewayMode.PROTOTYPE, DesktopSteamGatewayMode.fromRaw(""))
        assertEquals(DesktopSteamGatewayMode.FIXTURE, DesktopSteamGatewayMode.fromRaw("fixture"))
        assertEquals(DesktopSteamGatewayMode.PROTOTYPE, DesktopSteamGatewayMode.fromRaw("invalid"))
    }

    @Test
    fun fixtureModeLoadsLibraryAndQueueFromFixtureFiles() {
        val fixtureRoot = Files.createTempDirectory("desktop-shell-fixtures")
        Files.writeString(
            fixtureRoot.resolve("steam-library.json"),
            """
            [
              {"id": 620, "name": "Portal 2"},
              {"id": 892970, "name": "Valheim"}
            ]
            """.trimIndent(),
        )
        Files.writeString(
            fixtureRoot.resolve("steam-downloads.json"),
            """
            {
              "queuedAppIds": [620, 892970],
              "pausedAppIds": [892970]
            }
            """.trimIndent(),
        )
        Files.writeString(
            fixtureRoot.resolve("steam-auth-users.txt"),
            "alice\nbob\n",
        )

        val services = DesktopSteamServiceFactory.create(
            mode = DesktopSteamGatewayMode.FIXTURE,
            fixtureRoot = fixtureRoot,
        )

        val session = runBlocking { services.sessionManager.login("alice", "secret") }
        assertEquals("alice", session.accountName)

        val library = runBlocking { services.libraryService.refreshOwnedApps() }
        assertEquals(2, library.size)
        assertEquals(620, library.first().id)

        val downloadManager = services.downloadManager
        if (downloadManager is app.gamenative.linux.store.steam.JavaSteamDownloadManager) {
            runBlocking { downloadManager.bootstrapQueue() }
        }
        val queue = downloadManager.queueSnapshot()
        assertEquals(listOf(620, 892970), queue.queuedAppIds)
        assertEquals(listOf(892970), queue.pausedAppIds)

        val denied = runBlocking { services.sessionManager.login("charlie", "secret") }
        assertEquals(SteamSessionState.FAILED, denied.state)
    }
}
