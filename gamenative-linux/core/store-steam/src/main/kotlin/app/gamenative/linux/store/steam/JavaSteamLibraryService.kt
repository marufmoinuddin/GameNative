package app.gamenative.linux.store.steam

import app.gamenative.data.SteamApp

interface SteamLibraryGateway {
    suspend fun fetchOwnedApps(): List<SteamLibraryRecord>
    suspend fun fetchOwnedApp(appId: Int): SteamLibraryRecord?
}

data class SteamLibraryRecord(
    val appId: Int,
    val name: String,
)

class JavaSteamLibraryService(
    private val gateway: SteamLibraryGateway,
) : SteamLibraryService {
    private val cache = linkedMapOf<Int, SteamApp>()

    override suspend fun refreshOwnedApps(): List<SteamApp> {
        val apps = gateway.fetchOwnedApps().map { it.toSteamApp() }
        cache.clear()
        apps.forEach { app ->
            cache[app.id] = app
        }
        return cache.values.toList()
    }

    override suspend fun getOwnedApp(appId: Int): SteamApp? {
        cache[appId]?.let { return it }

        val fetched = gateway.fetchOwnedApp(appId)?.toSteamApp() ?: return null
        cache[fetched.id] = fetched
        return fetched
    }

    private fun SteamLibraryRecord.toSteamApp(): SteamApp {
        return SteamApp(id = appId, name = name)
    }
}
