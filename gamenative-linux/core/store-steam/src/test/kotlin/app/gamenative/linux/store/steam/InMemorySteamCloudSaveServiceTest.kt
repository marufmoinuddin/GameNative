package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemorySteamCloudSaveServiceTest {
    @Test
    fun defaultsToUpToDateWhenNoStatusConfigured() = runBlocking {
        val service = InMemorySteamCloudSaveService()

        val result = service.syncApp(730)

        assertEquals(730, result.appId)
        assertEquals(true, result.success)
        assertEquals(CloudSyncStatus.UP_TO_DATE, result.status)
    }

    @Test
    fun returnsConfiguredStatusAndFailureForConflict() = runBlocking {
        val service = InMemorySteamCloudSaveService(
            statusByAppId = mapOf(4000 to CloudSyncStatus.CONFLICT),
            detailsByAppId = mapOf(4000 to "manual resolution required"),
        )

        val result = service.syncApp(4000)

        assertEquals(false, result.success)
        assertEquals(CloudSyncStatus.CONFLICT, result.status)
        assertEquals("manual resolution required", result.details)
    }

    @Test
    fun returnsConfiguredSuccessStatusForUploaded() = runBlocking {
        val service = InMemorySteamCloudSaveService(
            statusByAppId = mapOf(5000 to CloudSyncStatus.UPLOADED),
        )

        val result = service.syncApp(5000)

        assertEquals(true, result.success)
        assertEquals(CloudSyncStatus.UPLOADED, result.status)
    }
}
