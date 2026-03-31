package app.gamenative.linux.store.steam

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FixtureSteamLibraryServiceTest {
    @Test
    fun refreshLoadsAppsFromJsonFixture() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-library-fixture", ".json")
        Files.writeString(
            fixtureFile,
            """
            [
              {"id": 570, "name": "Dota 2"},
              {"appId": 730, "name": "Counter-Strike 2"}
            ]
            """.trimIndent(),
        )

        val service = FixtureSteamLibraryService(fixtureFile)
        val apps = service.refreshOwnedApps()

        assertEquals(2, apps.size)
        assertEquals("Dota 2", service.getOwnedApp(570)?.name)
        assertEquals("Counter-Strike 2", service.getOwnedApp(730)?.name)
    }

    @Test
    fun refreshReturnsEmptyForMissingFixture() = runBlocking {
        val fixtureFile = Files.createTempDirectory("steam-library-fixture").resolve("missing.json")
        val service = FixtureSteamLibraryService(fixtureFile)

        val apps = service.refreshOwnedApps()

        assertEquals(0, apps.size)
        assertNull(service.getOwnedApp(570))
    }

    @Test
    fun refreshReturnsEmptyForMalformedFixture() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-library-fixture", ".json")
        Files.writeString(fixtureFile, "{bad-json")

        val service = FixtureSteamLibraryService(fixtureFile)
        val apps = service.refreshOwnedApps()

        assertEquals(0, apps.size)
    }
}
