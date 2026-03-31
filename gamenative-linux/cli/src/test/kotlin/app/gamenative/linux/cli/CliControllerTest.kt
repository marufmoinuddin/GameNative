package app.gamenative.linux.cli

import app.gamenative.data.SteamApp
import app.gamenative.linux.store.steam.InMemorySteamDownloadManager
import app.gamenative.linux.store.steam.InMemorySteamLibraryService
import app.gamenative.linux.store.steam.InMemorySteamSessionManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliControllerTest {
    private fun testServices(): CliSteamServices {
        return CliSteamServices(
            sessionManager = InMemorySteamSessionManager(),
            libraryService = InMemorySteamLibraryService(
                appProvider = {
                    listOf(
                        SteamApp(id = 620, name = "Portal 2"),
                        SteamApp(id = 892970, name = "Valheim"),
                        SteamApp(id = 1086940, name = "Baldur's Gate 3"),
                    )
                },
            ),
            downloadManager = InMemorySteamDownloadManager(),
        )
    }

    private class FakeSteamHost : CliSteamHost {
        val installed = linkedSetOf<Int>()
        var nextInstallSuccess = true
        var nextLaunchSuccess = true

        override fun isInstalled(appId: Int): Boolean = installed.contains(appId)

        override fun requestInstall(appId: Int): CliActionResult {
            return if (nextInstallSuccess) {
                CliActionResult(true, "Install request sent to Steam for appId $appId")
            } else {
                CliActionResult(false, "Failed to reach Steam client")
            }
        }

        override fun launchApp(appId: Int): CliActionResult {
            return if (!installed.contains(appId)) {
                CliActionResult(false, "Game is not installed yet.")
            } else if (nextLaunchSuccess) {
                CliActionResult(true, "Launch request sent to Steam for appId $appId")
            } else {
                CliActionResult(false, "Launch failed")
            }
        }
    }

    private fun makeController(host: FakeSteamHost = FakeSteamHost()): Pair<CliController, FakeSteamHost> {
        val controller = CliController(
            steamHost = host,
            services = testServices(),
        )
        return controller to host
    }

    @Test
    fun loginWithBlankCredentialsReturnsFailed() {
        val (controller, _) = makeController()
        val snapshot = controller.login("", "")
        assertEquals(
            app.gamenative.linux.store.steam.SteamSessionState.FAILED,
            snapshot.state,
        )
    }

    @Test
    fun loginWithValidCredentialsReturnsAuthenticated() {
        val (controller, _) = makeController()
        val snapshot = controller.login("alice", "secret")
        assertEquals(
            app.gamenative.linux.store.steam.SteamSessionState.AUTHENTICATED,
            snapshot.state,
        )
        assertEquals("alice", snapshot.accountName)
    }

    @Test
    fun libraryReturnsNonEmptyList() {
        val (controller, _) = makeController()
        controller.login("alice", "secret")
        val games = controller.library()
        assertTrue(games.isNotEmpty(), "Library should have at least one game")
    }

    @Test
    fun downloadRequestDoesNotFakeInstallState() {
        val (controller, host) = makeController()
        controller.login("alice", "secret")
        val games = controller.library()
        val appId = games.first().id

        assertFalse(controller.isInstalled(appId), "Game should not be installed initially")
        val downloadResult = controller.download(appId)
        assertTrue(downloadResult.success)
        assertFalse(controller.isInstalled(appId), "Install should not be faked by CLI")
        host.installed += appId
        assertTrue(controller.isInstalled(appId), "Install state should reflect Steam manifest state")
    }

    @Test
    fun launchFailsWhenNotInstalled() {
        val (controller, _) = makeController()
        controller.login("alice", "secret")
        val appId = controller.library().first().id

        val launchResult = controller.launch(appId)
        assertFalse(launchResult.success)
    }

    @Test
    fun launchUsesSteamHostWhenInstalled() {
        val (controller, host) = makeController()
        controller.login("alice", "secret")
        val appId = controller.library().first().id
        host.installed += appId

        val launchResult = controller.launch(appId)
        assertTrue(launchResult.success)
        assertTrue(launchResult.message.contains("appId $appId"))
    }
}
