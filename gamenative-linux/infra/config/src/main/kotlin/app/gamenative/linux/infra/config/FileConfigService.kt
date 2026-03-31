package app.gamenative.linux.infra.config

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class FileConfigService(
    private val configFile: Path,
) : ConfigService {
    private val props = Properties()

    init {
        load()
    }

    override fun getString(key: String, default: String): String {
        synchronized(props) {
            return props.getProperty(key, default)
        }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return getString(key, default.toString()).toBooleanStrictOrNull() ?: default
    }

    override fun getLong(key: String, default: Long): Long {
        return getString(key, default.toString()).toLongOrNull() ?: default
    }

    override fun putString(key: String, value: String) {
        synchronized(props) {
            props.setProperty(key, value)
            persist()
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    override fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    override fun getAppConfig(): AppConfig {
        synchronized(props) {
            return AppConfig(
                steamUserName = getString(ConfigKeys.STEAM_USER_NAME, ""),
                steamUserSteamId64 = getLong(ConfigKeys.STEAM_USER_STEAM_ID64, 0L),
                steamUserAccountId = getLong(ConfigKeys.STEAM_USER_ACCOUNT_ID, 0L),
                networkOnline = getBoolean(ConfigKeys.NETWORK_ONLINE, true),
                downloadsMaxConcurrent = getLong(ConfigKeys.DOWNLOADS_MAX_CONCURRENT, 3L),
                gameInstallRoot = getString(ConfigKeys.GAME_INSTALL_ROOT, ""),
                runtimeProfileId = getString(ConfigKeys.RUNTIME_PROFILE_ID, "default"),
                telemetryEnabled = getBoolean(ConfigKeys.TELEMETRY_ENABLED, true),
            )
        }
    }

    override fun putAppConfig(config: AppConfig) {
        synchronized(props) {
            props.setProperty(ConfigKeys.STEAM_USER_NAME, config.steamUserName)
            props.setProperty(ConfigKeys.STEAM_USER_STEAM_ID64, config.steamUserSteamId64.toString())
            props.setProperty(ConfigKeys.STEAM_USER_ACCOUNT_ID, config.steamUserAccountId.toString())
            props.setProperty(ConfigKeys.NETWORK_ONLINE, config.networkOnline.toString())
            props.setProperty(ConfigKeys.DOWNLOADS_MAX_CONCURRENT, config.downloadsMaxConcurrent.toString())
            props.setProperty(ConfigKeys.GAME_INSTALL_ROOT, config.gameInstallRoot)
            props.setProperty(ConfigKeys.RUNTIME_PROFILE_ID, config.runtimeProfileId)
            props.setProperty(ConfigKeys.TELEMETRY_ENABLED, config.telemetryEnabled.toString())
            persist()
        }
    }

    override fun importLegacyKeyValues(values: Map<String, String>) {
        synchronized(props) {
            values.forEach { (legacyKey, value) ->
                val mappedKey = LegacyConfigKeyMap.mappings[legacyKey] ?: legacyKey
                props.setProperty(mappedKey, value)
            }
            persist()
        }
    }

    private fun load() {
        if (!Files.exists(configFile)) {
            return
        }

        Files.newInputStream(configFile).use { input: InputStream ->
            synchronized(props) {
                props.load(input)
            }
        }
    }

    private fun persist() {
        val parent = configFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        Files.newOutputStream(configFile).use { output: OutputStream ->
            props.store(output, "GameNative Linux config")
        }
    }
}
