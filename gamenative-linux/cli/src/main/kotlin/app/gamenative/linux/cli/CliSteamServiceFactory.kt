package app.gamenative.linux.cli

import app.gamenative.linux.store.steam.JavaSteamDownloadManager
import app.gamenative.linux.store.steam.JavaSteamLibraryService
import app.gamenative.linux.store.steam.JavaSteamSessionManager
import app.gamenative.linux.store.steam.SteamDownloadManager
import app.gamenative.linux.store.steam.SteamLibraryService
import app.gamenative.linux.store.steam.SteamSessionManager

data class CliSteamServices(
    val sessionManager: SteamSessionManager,
    val libraryService: SteamLibraryService,
    val downloadManager: SteamDownloadManager,
)

object CliSteamServiceFactory {
    fun create(): CliSteamServices {
        val realGateway = CliRealSteamGateway()
        return CliSteamServices(
            sessionManager = JavaSteamSessionManager(realGateway),
            libraryService = JavaSteamLibraryService(realGateway),
            downloadManager = JavaSteamDownloadManager(CliQueueDownloadGateway()),
        )
    }
}
