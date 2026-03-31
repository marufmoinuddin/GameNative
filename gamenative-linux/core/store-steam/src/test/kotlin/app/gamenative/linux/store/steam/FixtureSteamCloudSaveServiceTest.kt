package app.gamenative.linux.store.steam

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FixtureSteamCloudSaveServiceTest {
    @Test
    fun syncUsesFixtureStatusAndDetails() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-cloud-fixture", ".json")
        Files.writeString(
            fixtureFile,
            """
            {
              "apps": [
                {"appId": 570, "status": "UP_TO_DATE", "details": "Already synced"},
                {"id": 730, "status": "CONFLICT", "details": "Conflict detected"}
              ]
            }
            """.trimIndent(),
        )

        val service = FixtureSteamCloudSaveService(fixtureFile)

        val upToDate = service.syncApp(570)
        assertTrue(upToDate.success)
        assertEquals(CloudSyncStatus.UP_TO_DATE, upToDate.status)
        assertEquals("Already synced", upToDate.details)

        val conflict = service.syncApp(730)
        assertFalse(conflict.success)
        assertEquals(CloudSyncStatus.CONFLICT, conflict.status)
        assertEquals("Conflict detected", conflict.details)
    }

    @Test
    fun unknownAppFallsBackToUpToDateDefault() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-cloud-fixture", ".json")
        Files.writeString(fixtureFile, "[]")

        val service = FixtureSteamCloudSaveService(fixtureFile)
        val result = service.syncApp(999)

        assertTrue(result.success)
        assertEquals(CloudSyncStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun malformedFixtureFallsBackToDefaults() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-cloud-fixture", ".json")
        Files.writeString(fixtureFile, "{bad-json")

        val service = FixtureSteamCloudSaveService(fixtureFile)
        val result = service.syncApp(570)

        assertTrue(result.success)
        assertEquals(CloudSyncStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun reloadAppliesUpdatedFixtureEntries() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-cloud-fixture", ".json")
        Files.writeString(
            fixtureFile,
            """
            [
              {"appId": 10, "status": "UPLOADED", "details": "Uploaded"}
            ]
            """.trimIndent(),
        )

        val service = FixtureSteamCloudSaveService(fixtureFile)
        Files.writeString(
            fixtureFile,
            """
            [
              {"appId": 10, "status": "FAILED", "details": "Network error"},
              {"appId": 20, "status": "DOWNLOADED", "details": "Downloaded"}
            ]
            """.trimIndent(),
        )

        val snapshot = service.reloadFixture()
        assertEquals(2, snapshot.entries)

        val updated = service.syncApp(10)
        assertFalse(updated.success)
        assertEquals(CloudSyncStatus.FAILED, updated.status)
        assertEquals("Network error", updated.details)
    }
}