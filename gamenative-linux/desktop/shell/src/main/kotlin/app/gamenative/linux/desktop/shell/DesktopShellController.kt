package app.gamenative.linux.desktop.shell

import app.gamenative.data.SteamApp
import app.gamenative.linux.runtime.FileRuntimeRecoveryService
import app.gamenative.linux.runtime.FileProfileRepository
import app.gamenative.linux.runtime.RuntimeBackend
import app.gamenative.linux.runtime.RuntimeProfile
import app.gamenative.linux.runtime.ShellCapabilityDetector
import app.gamenative.linux.store.steam.JavaSteamDownloadManager
import app.gamenative.linux.store.steam.SteamDownloadManager
import app.gamenative.linux.store.steam.SteamLibraryService
import app.gamenative.linux.store.steam.SteamDownloadQueueSnapshot
import app.gamenative.linux.store.steam.SteamSessionManager
import app.gamenative.linux.store.steam.SteamSessionSnapshot
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

class DesktopShellController(
    private val profileRepositoryPath: Path = defaultProfilePath(),
    private val settingsStorePath: Path = defaultSettingsPath(),
    private val taskStorePath: Path = defaultTaskStorePath(),
    private val steamGatewayMode: DesktopSteamGatewayMode = DesktopSteamGatewayMode.fromEnvironment(),
    private val steamFixtureRoot: Path = DesktopSteamServiceFactory.fixtureRootFromEnvironment(),
) {
    private val profileRepository = FileProfileRepository(profileRepositoryPath)
    private val settingsStore = DesktopSettingsStore(settingsStorePath)
    private val taskScheduler = DesktopTaskScheduler(taskStorePath)
    private val capabilityDetector = ShellCapabilityDetector()
    private val steamServices = DesktopSteamServiceFactory.create(
        mode = steamGatewayMode,
        fixtureRoot = steamFixtureRoot,
    )
    private val sessionManager: SteamSessionManager = steamServices.sessionManager
    private val libraryService: SteamLibraryService = steamServices.libraryService
    private val downloadManager: SteamDownloadManager = steamServices.downloadManager
    private var cachedLibrary: List<SteamApp> = emptyList()
    private var selectedAppId: Int? = null

    init {
        runBlocking {
            (downloadManager as? JavaSteamDownloadManager)?.bootstrapQueue()
        }
        taskScheduler.resumePendingTasks()
    }

    fun refreshState(): DesktopShellState {
        val capability = capabilityDetector.detect()
        val monitorSummary = readSessionMonitorSummary()
        return DesktopShellState(
            diagnostics = CapabilitySummary(
                wineAvailable = capability.wineAvailable,
                box64Available = capability.box64Available,
                fexAvailable = capability.fexAvailable,
                vulkanAvailable = capability.vulkanAvailable,
                diagnostics = capability.diagnostics,
            ),
            profiles = profileRepository.listProfiles().sortedBy { it.name.lowercase() },
            session = sessionManager.currentSession(),
            library = cachedLibrary,
            selectedGame = selectedAppId?.let { appId -> toGameSummary(cachedLibrary.firstOrNull { it.id == appId }) },
            downloads = downloadManager.queueSnapshot(),
            tasks = taskScheduler.listTasks(),
            settings = settingsStore.load(),
            sessionMonitor = monitorSummary,
        )
    }

    fun login(username: String, password: String): SteamSessionSnapshot {
        return runBlocking { sessionManager.login(username, password) }
    }

    fun logout() {
        runBlocking { sessionManager.logout() }
    }

    fun refreshLibrary(): List<SteamApp> {
        cachedLibrary = runBlocking { libraryService.refreshOwnedApps() }
        if (selectedAppId == null && cachedLibrary.isNotEmpty()) {
            selectedAppId = cachedLibrary.first().id
        }
        return cachedLibrary
    }

    fun selectGame(appId: Int): GameDetailSummary? {
        if (cachedLibrary.isEmpty()) {
            refreshLibrary()
        }
        selectedAppId = appId
        return toGameSummary(cachedLibrary.firstOrNull { it.id == appId })
    }

    fun enqueueDownload(appId: Int) {
        runBlocking { downloadManager.enqueueApp(appId) }
        taskScheduler.enqueue(type = "download", payload = appId.toString())
    }

    fun pauseDownload(appId: Int) {
        runBlocking { downloadManager.pauseApp(appId) }
        taskScheduler.markLatestByPayload(type = "download", payload = appId.toString(), status = DesktopTaskStatus.PAUSED)
    }

    fun cancelDownload(appId: Int) {
        runBlocking { downloadManager.cancelApp(appId) }
        taskScheduler.markLatestByPayload(type = "download", payload = appId.toString(), status = DesktopTaskStatus.CANCELED)
    }

    fun currentDownloadSnapshot(): SteamDownloadQueueSnapshot = downloadManager.queueSnapshot()

    fun saveProfile(
        id: String,
        name: String,
        wineBinary: String,
        backend: RuntimeBackend,
    ) {
        val profile = RuntimeProfile(
            id = id.trim(),
            name = name.trim(),
            wineBinary = wineBinary.trim(),
            backend = backend,
            env = mapOf("WINEDEBUG" to "-all"),
        )
        require(profile.id.isNotBlank()) { "profile id cannot be blank" }
        require(profile.name.isNotBlank()) { "profile name cannot be blank" }
        require(profile.wineBinary.isNotBlank()) { "wine binary cannot be blank" }
        profileRepository.saveProfile(profile)
    }

    fun saveSettings(downloadRoot: String, diagnosticsPollingSeconds: Int) {
        require(downloadRoot.isNotBlank()) { "download root cannot be blank" }
        require(diagnosticsPollingSeconds > 0) { "polling interval must be greater than zero" }
        settingsStore.save(
            DesktopSettings(
                downloadRoot = downloadRoot.trim(),
                diagnosticsPollingSeconds = diagnosticsPollingSeconds,
            ),
        )
    }

    fun tasksSnapshot(): List<DesktopTaskEntry> = taskScheduler.listTasks()

    private fun toGameSummary(app: SteamApp?): GameDetailSummary? {
        app ?: return null
        return GameDetailSummary(
            appId = app.id,
            name = app.name,
            headerUrl = app.headerUrl,
            isFree = app.isFreeApp,
            releaseState = app.releaseState.name,
        )
    }

    companion object {
        private fun defaultProfilePath(): Path {
            val configPath = Path.of(System.getProperty("user.home"), ".config", "gamenative")
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath)
            }
            return configPath.resolve("profiles.json")
        }

        private fun defaultSettingsPath(): Path {
            val configPath = Path.of(System.getProperty("user.home"), ".config", "gamenative")
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath)
            }
            return configPath.resolve("desktop-settings.properties")
        }

        private fun defaultTaskStorePath(): Path {
            val statePath = Path.of(System.getProperty("user.home"), ".local", "state", "gamenative")
            if (!Files.exists(statePath)) {
                Files.createDirectories(statePath)
            }
            return statePath.resolve("desktop-tasks.properties")
        }

        private fun readSessionMonitorSummary(): SessionMonitorSummary {
            val prototypeStateDir = Path.of("build", "runtime-prototype-state")
            val recovery = FileRuntimeRecoveryService(prototypeStateDir)
            val timeline = recovery.launchStateTimeline("runtime-prototype")
            if (timeline.isEmpty()) {
                return SessionMonitorSummary()
            }

            val summary = timeline.joinToString(" -> ") { entry ->
                if (entry.reason.isBlank()) {
                    entry.state.name
                } else {
                    "${entry.state.name}(${entry.reason})"
                }
            }
            return SessionMonitorSummary(timelineSummary = summary)
        }
    }
}
