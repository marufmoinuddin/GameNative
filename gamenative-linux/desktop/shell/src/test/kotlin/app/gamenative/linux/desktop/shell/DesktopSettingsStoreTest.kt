package app.gamenative.linux.desktop.shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSettingsStoreTest {
    @Test
    fun savesAndLoadsSettings() {
        val tempDir = Files.createTempDirectory("desktop-settings-store-test")
        val file = tempDir.resolve("desktop-settings.properties")
        val store = DesktopSettingsStore(file)

        store.save(
            DesktopSettings(
                downloadRoot = "/tmp/gamenative",
                diagnosticsPollingSeconds = 12,
            ),
        )

        val loaded = store.load()
        assertEquals("/tmp/gamenative", loaded.downloadRoot)
        assertEquals(12, loaded.diagnosticsPollingSeconds)
    }
}
