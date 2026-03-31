package app.gamenative.linux.infra.config

interface ConfigService {
    fun getString(key: String, default: String = ""): String
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun getLong(key: String, default: Long = 0L): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
    fun getAppConfig(): AppConfig
    fun putAppConfig(config: AppConfig)
    fun importLegacyKeyValues(values: Map<String, String>)
}

/**
 * Typed Linux settings schema mirrored from legacy key-value settings.
 */
data class AppConfig(
    val steamUserName: String = "",
    val steamUserSteamId64: Long = 0L,
    val steamUserAccountId: Long = 0L,
    val networkOnline: Boolean = true,
    val downloadsMaxConcurrent: Long = 3L,
    val gameInstallRoot: String = "",
    val runtimeProfileId: String = "default",
    val telemetryEnabled: Boolean = true,
)

object ConfigKeys {
    const val STEAM_USER_NAME = "steam.user.name"
    const val STEAM_USER_STEAM_ID64 = "steam.user.steam_id64"
    const val STEAM_USER_ACCOUNT_ID = "steam.user.account_id"
    const val NETWORK_ONLINE = "network.online"
    const val DOWNLOADS_MAX_CONCURRENT = "downloads.max.concurrent"
    const val GAME_INSTALL_ROOT = "games.install.root"
    const val RUNTIME_PROFILE_ID = "runtime.profile.id"
    const val TELEMETRY_ENABLED = "telemetry.enabled"
}

/**
 * Legacy Android key names mapped to Linux config keys to support migration.
 */
object LegacyConfigKeyMap {
    val mappings: Map<String, String> = mapOf(
        "steamUserName" to ConfigKeys.STEAM_USER_NAME,
        "steamUserSteamId64" to ConfigKeys.STEAM_USER_STEAM_ID64,
        "steamUserAccountId" to ConfigKeys.STEAM_USER_ACCOUNT_ID,
        "downloads.max" to ConfigKeys.DOWNLOADS_MAX_CONCURRENT,
        "network.online" to ConfigKeys.NETWORK_ONLINE,
        "telemetryEnabled" to ConfigKeys.TELEMETRY_ENABLED,
        "runtimeProfileId" to ConfigKeys.RUNTIME_PROFILE_ID,
    )
}
