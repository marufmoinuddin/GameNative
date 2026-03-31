package app.gamenative.linux.cli

import app.gamenative.data.SteamApp
import app.gamenative.linux.store.steam.JavaSteamDownloadManager
import app.gamenative.linux.store.steam.SteamDownloadManager
import app.gamenative.linux.store.steam.SteamLibraryService
import app.gamenative.linux.store.steam.SteamSessionManager
import app.gamenative.linux.store.steam.SteamSessionSnapshot
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

class CliController(
    private val steamHost: CliSteamHost = DefaultCliSteamHost(),
    private val stateDir: Path = defaultStateDir(),
    services: CliSteamServices = CliSteamServiceFactory.create(),
) {
    private val sessionManager: SteamSessionManager = services.sessionManager
    private val libraryService: SteamLibraryService = services.libraryService
    private val downloadManager: SteamDownloadManager = services.downloadManager
    private var cachedLibrary: List<SteamApp> = emptyList()

    init {
        runBlocking { (downloadManager as? JavaSteamDownloadManager)?.bootstrapQueue() }
    }

    fun login(username: String, password: String): SteamSessionSnapshot =
        runBlocking { sessionManager.login(username, password) }

    fun logout() = runBlocking { sessionManager.logout() }

    fun currentSession() = sessionManager.currentSession()

    fun library(): List<SteamApp> {
        cachedLibrary = runBlocking { libraryService.refreshOwnedApps() }
        return cachedLibrary
    }

    fun download(appId: Int): CliActionResult = steamHost.requestInstall(appId)

    fun isInstalled(appId: Int): Boolean = steamHost.isInstalled(appId)

    fun launch(appId: Int): CliActionResult = steamHost.launchApp(appId)

    companion object {
        private fun defaultStateDir(): Path {
            val dir = Path.of(System.getProperty("user.home"), ".local", "state", "gamenative", "sessions")
            if (!Files.exists(dir)) Files.createDirectories(dir)
            return dir
        }
    }
}

data class CliActionResult(
    val success: Boolean,
    val message: String,
)
