package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaSteamCloudSaveServiceTest {
    @Test
    fun returnsGatewayStatusAndDetails() = runBlocking {
        val service = JavaSteamCloudSaveService(
            gateway = FakeSteamCloudGateway(
                mapOf(
                    570 to CloudSyncGatewayResult(CloudSyncStatus.DOWNLOADED, "Pulled latest saves"),
                ),
            ),
        )

        val result = service.syncApp(570)

        assertTrue(result.success)
        assertEquals(CloudSyncStatus.DOWNLOADED, result.status)
        assertEquals("Pulled latest saves", result.details)
    }

    @Test
    fun fallsBackToUpToDateWhenGatewayHasNoEntry() = runBlocking {
        val service = JavaSteamCloudSaveService(gateway = FakeSteamCloudGateway(emptyMap()))

        val result = service.syncApp(999)

        assertTrue(result.success)
        assertEquals(CloudSyncStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun conflictIsReportedAsFailure() = runBlocking {
        val service = JavaSteamCloudSaveService(
            gateway = FakeSteamCloudGateway(
                mapOf(10 to CloudSyncGatewayResult(CloudSyncStatus.CONFLICT, "Resolve conflict")),
            ),
        )

        val result = service.syncApp(10)

        assertFalse(result.success)
        assertEquals(CloudSyncStatus.CONFLICT, result.status)
    }
}

private class FakeSteamCloudGateway(
    private val statusByAppId: Map<Int, CloudSyncGatewayResult>,
) : SteamCloudGateway {
    override suspend fun syncStatus(appId: Int): CloudSyncGatewayResult? = statusByAppId[appId]
}
