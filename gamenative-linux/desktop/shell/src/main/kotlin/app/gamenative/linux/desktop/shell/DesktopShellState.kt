package app.gamenative.linux.desktop.shell

import app.gamenative.data.SteamApp
import app.gamenative.linux.store.steam.SteamDownloadQueueSnapshot
import app.gamenative.linux.store.steam.SteamSessionSnapshot
import app.gamenative.linux.runtime.RuntimeProfile

data class DesktopShellState(
    val diagnostics: CapabilitySummary = CapabilitySummary(),
    val profiles: List<RuntimeProfile> = emptyList(),
    val session: SteamSessionSnapshot? = null,
    val library: List<SteamApp> = emptyList(),
    val selectedGame: GameDetailSummary? = null,
    val downloads: SteamDownloadQueueSnapshot = SteamDownloadQueueSnapshot(emptyList(), emptyList()),
    val tasks: List<DesktopTaskEntry> = emptyList(),
    val settings: DesktopSettings = DesktopSettings(),
    val sessionMonitor: SessionMonitorSummary = SessionMonitorSummary(),
)

data class CapabilitySummary(
    val wineAvailable: Boolean = false,
    val box64Available: Boolean = false,
    val fexAvailable: Boolean = false,
    val vulkanAvailable: Boolean = false,
    val diagnostics: List<String> = emptyList(),
)

data class SessionMonitorSummary(
    val timelineSummary: String = "No runtime launch timeline available yet.",
)

data class GameDetailSummary(
    val appId: Int,
    val name: String,
    val headerUrl: String,
    val isFree: Boolean,
    val releaseState: String,
)

data class DesktopSettings(
    val downloadRoot: String = "~/.local/share/gamenative/games",
    val diagnosticsPollingSeconds: Int = 10,
)
