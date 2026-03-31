package app.gamenative.linux.store.steam

import java.nio.file.Path
import kotlinx.serialization.json.Json
import app.gamenative.data.SteamApp

class FixtureSteamLibraryService(
    private val fixtureFile: Path,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) : SteamLibraryService {
    private val delegate = JavaSteamLibraryService(
        gateway = FixtureSteamLibraryGateway(
            fixtureFile = fixtureFile,
            json = json,
        ),
    )

    override suspend fun refreshOwnedApps(): List<SteamApp> {
        return delegate.refreshOwnedApps()
    }

    override suspend fun getOwnedApp(appId: Int): SteamApp? = delegate.getOwnedApp(appId)
}
