package app.gamenative.linux.store.steam

class InMemorySteamCloudSaveService(
    private val statusByAppId: Map<Int, CloudSyncStatus> = emptyMap(),
    private val detailsByAppId: Map<Int, String> = emptyMap(),
) : SteamCloudSaveService {
    override suspend fun syncApp(appId: Int): CloudSyncResult {
        val status = statusByAppId[appId] ?: CloudSyncStatus.UP_TO_DATE
        val details = detailsByAppId[appId] ?: defaultDetails(status)
        val success = when (status) {
            CloudSyncStatus.UP_TO_DATE,
            CloudSyncStatus.UPLOADED,
            CloudSyncStatus.DOWNLOADED,
            -> true
            CloudSyncStatus.CONFLICT,
            CloudSyncStatus.FAILED,
            CloudSyncStatus.UNKNOWN,
            -> false
        }

        return CloudSyncResult(
            appId = appId,
            success = success,
            status = status,
            details = details,
        )
    }

    private fun defaultDetails(status: CloudSyncStatus): String {
        return when (status) {
            CloudSyncStatus.UP_TO_DATE -> "Cloud saves already up to date"
            CloudSyncStatus.UPLOADED -> "Local saves uploaded"
            CloudSyncStatus.DOWNLOADED -> "Remote saves downloaded"
            CloudSyncStatus.CONFLICT -> "Save conflict requires user resolution"
            CloudSyncStatus.FAILED -> "Cloud save sync failed"
            CloudSyncStatus.UNKNOWN -> "Cloud save sync status unknown"
        }
    }
}
