package app.gamenative.linux.desktop.shell

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class DesktopSettingsStore(
    private val settingsFile: Path,
) {
    fun load(): DesktopSettings {
        if (!Files.exists(settingsFile)) {
            return DesktopSettings()
        }

        val props = Properties()
        Files.newInputStream(settingsFile).use { props.load(it) }

        return DesktopSettings(
            downloadRoot = props.getProperty("downloadRoot", DesktopSettings().downloadRoot),
            diagnosticsPollingSeconds = props.getProperty("diagnosticsPollingSeconds")?.toIntOrNull()
                ?: DesktopSettings().diagnosticsPollingSeconds,
        )
    }

    fun save(settings: DesktopSettings) {
        val parent = settingsFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val props = Properties()
        props.setProperty("downloadRoot", settings.downloadRoot)
        props.setProperty("diagnosticsPollingSeconds", settings.diagnosticsPollingSeconds.toString())
        Files.newOutputStream(settingsFile).use { props.store(it, "GameNative desktop settings") }
    }
}
