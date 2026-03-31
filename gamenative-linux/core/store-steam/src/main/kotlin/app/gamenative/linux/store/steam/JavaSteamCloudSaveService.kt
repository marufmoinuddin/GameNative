package app.gamenative.linux.store.steam

interface SteamCloudGateway {
    suspend fun syncStatus(appId: Int): CloudSyncGatewayResult?
}

data class CloudSyncGatewayResult(
    val status: CloudSyncStatus,
    val details: String = "",
)

class JavaSteamCloudSaveService(
    private val gateway: SteamCloudGateway,
) : SteamCloudSaveService {
    override suspend fun syncApp(appId: Int): CloudSyncResult {
        val gatewayResult = gateway.syncStatus(appId)
            ?: CloudSyncGatewayResult(status = CloudSyncStatus.UP_TO_DATE)

        return CloudSyncResult(
            appId = appId,
            success = isSuccess(gatewayResult.status),
            status = gatewayResult.status,
            details = gatewayResult.details.ifBlank { defaultDetails(gatewayResult.status) },
        )
    }

    private fun isSuccess(status: CloudSyncStatus): Boolean {
        return when (status) {
            CloudSyncStatus.UP_TO_DATE,
            CloudSyncStatus.UPLOADED,
            CloudSyncStatus.DOWNLOADED,
            -> true
            CloudSyncStatus.CONFLICT,
            CloudSyncStatus.FAILED,
            CloudSyncStatus.UNKNOWN,
            -> false
        }
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
