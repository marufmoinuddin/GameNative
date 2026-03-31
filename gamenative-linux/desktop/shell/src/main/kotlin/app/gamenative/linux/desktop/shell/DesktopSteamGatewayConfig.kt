package app.gamenative.linux.desktop.shell

import app.gamenative.linux.store.steam.FixtureSteamDownloadGateway
import app.gamenative.linux.store.steam.FixtureSteamLibraryGateway
import app.gamenative.linux.store.steam.JavaSteamDownloadManager
import app.gamenative.linux.store.steam.JavaSteamLibraryService
import app.gamenative.linux.store.steam.JavaSteamSessionManager
import app.gamenative.linux.store.steam.SteamDownloadManager
import app.gamenative.linux.store.steam.SteamLibraryService
import app.gamenative.linux.store.steam.SteamSessionManager
import java.nio.file.Path
import kotlinx.serialization.json.Json

enum class DesktopSteamGatewayMode {
    PROTOTYPE,
    FIXTURE,
    ;

    companion object {
        const val MODE_ENV = "GAMENATIVE_STEAM_GATEWAY_MODE"

        fun fromRaw(value: String?): DesktopSteamGatewayMode {
            return when (value?.trim()?.uppercase()) {
                "FIXTURE" -> FIXTURE
                else -> PROTOTYPE
            }
        }

        fun fromEnvironment(env: Map<String, String> = System.getenv()): DesktopSteamGatewayMode {
            return fromRaw(env[MODE_ENV])
        }
    }
}

data class DesktopSteamServices(
    val sessionManager: SteamSessionManager,
    val libraryService: SteamLibraryService,
    val downloadManager: SteamDownloadManager,
)

object DesktopSteamServiceFactory {
    const val FIXTURE_ROOT_ENV = "GAMENATIVE_STEAM_FIXTURE_ROOT"

    fun fixtureRootFromEnvironment(env: Map<String, String> = System.getenv()): Path {
        return env[FIXTURE_ROOT_ENV]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(Path::of)
            ?: defaultFixtureRoot()
    }

    fun create(
        mode: DesktopSteamGatewayMode,
        fixtureRoot: Path,
    ): DesktopSteamServices {
        return when (mode) {
            DesktopSteamGatewayMode.PROTOTYPE -> DesktopSteamServices(
                sessionManager = JavaSteamSessionManager(DesktopShellPrototypeAuthGateway()),
                libraryService = JavaSteamLibraryService(DesktopShellPrototypeLibraryGateway()),
                downloadManager = JavaSteamDownloadManager(DesktopShellPrototypeDownloadGateway()),
            )

            DesktopSteamGatewayMode.FIXTURE -> {
                val json = Json { ignoreUnknownKeys = true }
                DesktopSteamServices(
                    sessionManager = JavaSteamSessionManager(
                        DesktopShellFixtureAuthGateway(
                            usersFile = fixtureRoot.resolve("steam-auth-users.txt"),
                        ),
                    ),
                    libraryService = JavaSteamLibraryService(
                        gateway = FixtureSteamLibraryGateway(
                            fixtureFile = fixtureRoot.resolve("steam-library.json"),
                            json = json,
                        ),
                    ),
                    downloadManager = JavaSteamDownloadManager(
                        gateway = FixtureSteamDownloadGateway(
                            fixtureFile = fixtureRoot.resolve("steam-downloads.json"),
                            json = json,
                        ),
                    ),
                )
            }
        }
    }

    private fun defaultFixtureRoot(): Path {
        return Path.of(System.getProperty("user.home"), ".local", "share", "gamenative", "fixtures")
    }
}
