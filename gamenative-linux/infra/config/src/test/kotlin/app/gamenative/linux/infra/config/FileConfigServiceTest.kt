package app.gamenative.linux.infra.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class FileConfigServiceTest {
    @Test
    fun writesAndReadsValues() {
        val tempDir = Files.createTempDirectory("gamenative-config-test")
        val configPath = tempDir.resolve("config.properties")

        val service = FileConfigService(configPath)
        service.putString("user.name", "alice")
        service.putBoolean("network.online", true)
        service.putLong("downloads.max", 42L)

        val reloaded = FileConfigService(configPath)

        assertEquals("alice", reloaded.getString("user.name"))
        assertEquals(true, reloaded.getBoolean("network.online"))
        assertEquals(42L, reloaded.getLong("downloads.max"))
    }

    @Test
    fun writesAndReadsTypedAppConfig() {
        val tempDir = Files.createTempDirectory("gamenative-config-test")
        val configPath = tempDir.resolve("config.properties")

        val service = FileConfigService(configPath)
        service.putAppConfig(
            AppConfig(
                steamUserName = "alice",
                steamUserSteamId64 = 7656119L,
                steamUserAccountId = 12345L,
                networkOnline = false,
                downloadsMaxConcurrent = 5,
                gameInstallRoot = "/games",
                runtimeProfileId = "performance",
                telemetryEnabled = false,
            ),
        )

        val reloaded = FileConfigService(configPath)
        val config = reloaded.getAppConfig()

        assertEquals("alice", config.steamUserName)
        assertEquals(7656119L, config.steamUserSteamId64)
        assertEquals(12345L, config.steamUserAccountId)
        assertEquals(false, config.networkOnline)
        assertEquals(5L, config.downloadsMaxConcurrent)
        assertEquals("/games", config.gameInstallRoot)
        assertEquals("performance", config.runtimeProfileId)
        assertEquals(false, config.telemetryEnabled)
    }

    @Test
    fun importsLegacyKeysIntoMappedSchema() {
        val tempDir = Files.createTempDirectory("gamenative-config-test")
        val configPath = tempDir.resolve("config.properties")

        val service = FileConfigService(configPath)
        service.importLegacyKeyValues(
            mapOf(
                "steamUserName" to "bob",
                "steamUserSteamId64" to "999",
                "downloads.max" to "7",
                "network.online" to "true",
            ),
        )

        val config = FileConfigService(configPath).getAppConfig()
        assertEquals("bob", config.steamUserName)
        assertEquals(999L, config.steamUserSteamId64)
        assertEquals(7L, config.downloadsMaxConcurrent)
        assertEquals(true, config.networkOnline)
    }
}
