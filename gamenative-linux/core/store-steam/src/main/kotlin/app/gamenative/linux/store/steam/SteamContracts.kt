package app.gamenative.linux.store.steam

import app.gamenative.data.SteamApp

enum class SteamSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    FAILED,
}

data class SteamSessionSnapshot(
    val state: SteamSessionState,
    val accountName: String? = null,
    val message: String? = null,
)

interface SteamSessionManager {
    suspend fun login(username: String, password: String): SteamSessionSnapshot
    suspend fun logout()
    fun currentSession(): SteamSessionSnapshot
}

interface SteamLibraryService {
    suspend fun refreshOwnedApps(): List<SteamApp>
    suspend fun getOwnedApp(appId: Int): SteamApp?
}

interface SteamDownloadManager {
    suspend fun enqueueApp(appId: Int)
    suspend fun pauseApp(appId: Int)
    suspend fun cancelApp(appId: Int)
    fun queueSnapshot(): SteamDownloadQueueSnapshot
}

interface SteamCloudSaveService {
    suspend fun syncApp(appId: Int): CloudSyncResult
}

data class CloudSyncResult(
    val appId: Int,
    val success: Boolean,
    val status: CloudSyncStatus = CloudSyncStatus.UNKNOWN,
    val details: String = "",
)

enum class CloudSyncStatus {
    UP_TO_DATE,
    UPLOADED,
    DOWNLOADED,
    CONFLICT,
    FAILED,
    UNKNOWN,
}

data class SteamDownloadQueueSnapshot(
    val queuedAppIds: List<Int>,
    val pausedAppIds: List<Int>,
)
