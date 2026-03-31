package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaSteamDownloadManagerTest {
    @Test
    fun bootstrapMirrorsGatewayQueue() = runBlocking {
        val gateway = FakeSteamDownloadGateway(
            initialQueue = SteamDownloadQueueSnapshot(
                queuedAppIds = listOf(570, 730),
                pausedAppIds = listOf(730),
            ),
        )
        val manager = JavaSteamDownloadManager(gateway)

        val snapshot = manager.bootstrapQueue()

        assertEquals(listOf(570, 730), snapshot.queuedAppIds)
        assertEquals(listOf(730), snapshot.pausedAppIds)
        assertEquals(listOf("fetchQueueSnapshot"), gateway.calls)
    }

    @Test
    fun enqueuePauseCancelTrackMirrorWhenGatewaySucceeds() = runBlocking {
        val gateway = FakeSteamDownloadGateway(
            initialQueue = SteamDownloadQueueSnapshot(emptyList(), emptyList()),
            enqueueSuccess = true,
            pauseSuccess = true,
            cancelSuccess = true,
        )
        val manager = JavaSteamDownloadManager(gateway)

        manager.enqueueApp(10)
        manager.pauseApp(10)
        manager.cancelApp(10)

        val snapshot = manager.queueSnapshot()
        assertTrue(snapshot.queuedAppIds.isEmpty())
        assertTrue(snapshot.pausedAppIds.isEmpty())
        assertEquals(listOf("enqueue:10", "pause:10", "cancel:10"), gateway.calls)
    }

    @Test
    fun mirrorDoesNotChangeWhenGatewayActionFails() = runBlocking {
        val gateway = FakeSteamDownloadGateway(
            initialQueue = SteamDownloadQueueSnapshot(emptyList(), emptyList()),
            enqueueSuccess = false,
        )
        val manager = JavaSteamDownloadManager(gateway)

        manager.enqueueApp(20)

        assertTrue(manager.queueSnapshot().queuedAppIds.isEmpty())
        assertEquals(listOf("enqueue:20"), gateway.calls)
    }
}

private class FakeSteamDownloadGateway(
    private val initialQueue: SteamDownloadQueueSnapshot,
    private val enqueueSuccess: Boolean = true,
    private val pauseSuccess: Boolean = true,
    private val cancelSuccess: Boolean = true,
) : SteamDownloadGateway {
    val calls = mutableListOf<String>()

    override suspend fun fetchQueueSnapshot(): SteamDownloadQueueSnapshot {
        calls += "fetchQueueSnapshot"
        return initialQueue
    }

    override suspend fun enqueue(appId: Int): Boolean {
        calls += "enqueue:$appId"
        return enqueueSuccess
    }

    override suspend fun pause(appId: Int): Boolean {
        calls += "pause:$appId"
        return pauseSuccess
    }

    override suspend fun cancel(appId: Int): Boolean {
        calls += "cancel:$appId"
        return cancelSuccess
    }
}
