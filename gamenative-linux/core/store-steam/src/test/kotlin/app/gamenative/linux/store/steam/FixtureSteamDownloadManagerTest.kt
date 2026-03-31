package app.gamenative.linux.store.steam

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureSteamDownloadManagerTest {
    @Test
    fun initializesQueueFromFixture() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-download-fixture", ".json")
        Files.writeString(
            fixtureFile,
            """
            {
              "queuedAppIds": [570, 730, 440],
              "pausedAppIds": [730, 999]
            }
            """.trimIndent(),
        )

        val manager = FixtureSteamDownloadManager(fixtureFile)
        val snapshot = manager.queueSnapshot()

        assertEquals(listOf(570, 730, 440), snapshot.queuedAppIds)
        assertEquals(listOf(730), snapshot.pausedAppIds)
    }

    @Test
    fun initializesEmptyQueueWhenFixtureMissing() {
        val fixtureFile = Files.createTempDirectory("steam-download-fixture").resolve("missing.json")
        val manager = FixtureSteamDownloadManager(fixtureFile)

        val snapshot = manager.queueSnapshot()
        assertTrue(snapshot.queuedAppIds.isEmpty())
        assertTrue(snapshot.pausedAppIds.isEmpty())
    }

    @Test
    fun replayResetsQueueFromFixture() = runBlocking {
        val fixtureFile = Files.createTempFile("steam-download-fixture", ".json")
        Files.writeString(
            fixtureFile,
            """
            {
              "queuedAppIds": [10, 20],
              "pausedAppIds": [20]
            }
            """.trimIndent(),
        )
        val manager = FixtureSteamDownloadManager(fixtureFile)
        manager.enqueueApp(30)

        Files.writeString(
            fixtureFile,
            """
            {
              "queuedAppIds": [99],
              "pausedAppIds": []
            }
            """.trimIndent(),
        )

        val replayed = manager.replayFixtureQueue()
        assertEquals(listOf(99), replayed.queuedAppIds)
        assertTrue(replayed.pausedAppIds.isEmpty())
    }

    @Test
    fun malformedFixtureFallsBackToEmptyQueue() {
        val fixtureFile = Files.createTempFile("steam-download-fixture", ".json")
        Files.writeString(fixtureFile, "{bad-json")

        val manager = FixtureSteamDownloadManager(fixtureFile)
        val snapshot = manager.queueSnapshot()

        assertTrue(snapshot.queuedAppIds.isEmpty())
        assertTrue(snapshot.pausedAppIds.isEmpty())
    }
}