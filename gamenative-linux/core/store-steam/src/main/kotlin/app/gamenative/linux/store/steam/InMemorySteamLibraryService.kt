package app.gamenative.linux.store.steam

import app.gamenative.data.SteamApp

class InMemorySteamLibraryService(
    private val appProvider: suspend () -> List<SteamApp> = { emptyList() },
) : SteamLibraryService {
    private val cache = linkedMapOf<Int, SteamApp>()

    override suspend fun refreshOwnedApps(): List<SteamApp> {
        val apps = appProvider()
        cache.clear()
        apps.forEach { app ->
            cache[app.id] = app
        }
        return cache.values.toList()
    }

    override suspend fun getOwnedApp(appId: Int): SteamApp? = cache[appId]
}
