package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemorySteamDownloadManagerTest {
    @Test
    fun enqueuePauseCancelUpdatesQueueSnapshot() = runBlocking {
        val manager = InMemorySteamDownloadManager()

        manager.enqueueApp(100)
        manager.enqueueApp(101)
        manager.pauseApp(101)

        val afterPause = manager.queueSnapshot()
        assertEquals(listOf(100, 101), afterPause.queuedAppIds)
        assertEquals(listOf(101), afterPause.pausedAppIds)

        manager.cancelApp(101)

        val afterCancel = manager.queueSnapshot()
        assertEquals(listOf(100), afterCancel.queuedAppIds)
        assertTrue(afterCancel.pausedAppIds.isEmpty())
    }
}
