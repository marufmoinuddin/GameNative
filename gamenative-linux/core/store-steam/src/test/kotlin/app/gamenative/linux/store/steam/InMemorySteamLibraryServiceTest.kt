package app.gamenative.linux.store.steam

import app.gamenative.data.SteamApp
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemorySteamLibraryServiceTest {
    @Test
    fun refreshPopulatesCacheAndSupportsLookup() = runBlocking {
        val service = InMemorySteamLibraryService(
            appProvider = {
                listOf(
                    SteamApp(id = 10, name = "Game A"),
                    SteamApp(id = 11, name = "Game B"),
                )
            },
        )

        val apps = service.refreshOwnedApps()

        assertEquals(2, apps.size)
        assertEquals("Game A", service.getOwnedApp(10)?.name)
        assertNull(service.getOwnedApp(999))
    }
}
