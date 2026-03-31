package app.gamenative.linux.cli

import app.gamenative.data.SteamApp
import app.gamenative.linux.store.steam.InMemorySteamDownloadManager
import app.gamenative.linux.store.steam.InMemorySteamLibraryService
import app.gamenative.linux.store.steam.InMemorySteamSessionManager
import java.nio.file.Files
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

    private fun makeController(): Pair<CliController, java.nio.file.Path> {
        val tempDir = Files.createTempDirectory("cli-controller-test")
        val controller = CliController(
            profileRepositoryPath = tempDir.resolve("profiles.json"),
            taskStorePath = tempDir.resolve("cli-tasks.properties"),
            stateDir = tempDir.resolve("sessions"),
            services = testServices(),
        )
        return controller to tempDir
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
    fun downloadThenMarkInstalledTracksState() {
        val (controller, _) = makeController()
        controller.login("alice", "secret")
        val games = controller.library()
        val appId = games.first().id

        assertFalse(controller.isInstalled(appId), "Game should not be installed initially")
        controller.download(appId)
        assertFalse(controller.isInstalled(appId), "Game should still be QUEUED, not COMPLETED")
        controller.markInstalled(appId)
        assertTrue(controller.isInstalled(appId), "Game should be COMPLETED after markInstalled")
    }

    @Test
    fun installStatePersistedAcrossReloads() {
        val tempDir = Files.createTempDirectory("cli-controller-persist-test")
        val controller1 = CliController(
            profileRepositoryPath = tempDir.resolve("profiles.json"),
            taskStorePath = tempDir.resolve("cli-tasks.properties"),
            stateDir = tempDir.resolve("sessions"),
            services = testServices(),
        )
        controller1.login("alice", "secret")
        val appId = controller1.library().first().id
        controller1.download(appId)
        controller1.markInstalled(appId)

        // Re-create controller from same paths to simulate restart
        val controller2 = CliController(
            profileRepositoryPath = tempDir.resolve("profiles.json"),
            taskStorePath = tempDir.resolve("cli-tasks.properties"),
            stateDir = tempDir.resolve("sessions"),
            services = testServices(),
        )
        assertTrue(
            controller2.isInstalled(appId),
            "Install state should survive a controller restart",
        )
    }

    @Test
    fun launchReturnNonBlankSessionId() {
        val (controller, _) = makeController()
        controller.login("alice", "secret")
        val appId = controller.library().first().id
        controller.download(appId)
        controller.markInstalled(appId)

        val sessionId = controller.launch(appId)
        assertTrue(sessionId.isNotBlank(), "Session ID must not be blank after launch")
    }
}
