package app.gamenative.linux.cli

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CliSteamGatewayConfigTest {
    @Test
    fun factoryCreatesJavaSteamBackedServices() {
        val services = CliSteamServiceFactory.create()

        assertTrue(
            services.sessionManager is app.gamenative.linux.store.steam.JavaSteamSessionManager,
            "CLI should use JavaSteamSessionManager",
        )

        assertTrue(
            services.libraryService is app.gamenative.linux.store.steam.JavaSteamLibraryService,
            "CLI should use JavaSteamLibraryService",
        )

        assertTrue(
            services.downloadManager is app.gamenative.linux.store.steam.JavaSteamDownloadManager,
            "CLI should use JavaSteamDownloadManager",
        )
    }

    @Test
    fun queueDownloadGatewayTracksQueueAndPauseState() = runBlocking {
        val gateway = CliQueueDownloadGateway()

        gateway.enqueue(620)
        gateway.enqueue(892970)
        gateway.pause(892970)
        val snapshot = gateway.fetchQueueSnapshot()

        assertTrue(snapshot.queuedAppIds.contains(620))
        assertTrue(snapshot.queuedAppIds.contains(892970))
        assertTrue(snapshot.pausedAppIds.contains(892970))
    }
}
