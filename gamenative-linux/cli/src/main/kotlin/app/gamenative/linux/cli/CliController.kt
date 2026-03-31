package app.gamenative.linux.cli

import app.gamenative.data.SteamApp
import app.gamenative.linux.runtime.DefaultEnvironmentComposer
import app.gamenative.linux.runtime.DefaultRuntimeOrchestrator
import app.gamenative.linux.runtime.FileProfileRepository
import app.gamenative.linux.runtime.LaunchPlan
import app.gamenative.linux.runtime.LocalProcessRunner
import app.gamenative.linux.runtime.RuntimeBackend
import app.gamenative.linux.runtime.RuntimeProfile
import app.gamenative.linux.store.steam.JavaSteamDownloadManager
import app.gamenative.linux.store.steam.SteamDownloadManager
import app.gamenative.linux.store.steam.SteamLibraryService
import app.gamenative.linux.store.steam.SteamSessionManager
import app.gamenative.linux.store.steam.SteamSessionSnapshot
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import kotlinx.coroutines.runBlocking

/**
 * CLI controller — thin orchestration layer that wires together Steam session,
 * library, download, and runtime services for the interactive CLI.
 */
class CliController(
    private val profileRepositoryPath: Path = defaultProfilePath(),
    private val taskStorePath: Path = defaultTaskStorePath(),
    private val stateDir: Path = defaultStateDir(),
    services: CliSteamServices = CliSteamServiceFactory.create(),
) {
    private val sessionManager: SteamSessionManager = services.sessionManager
    private val libraryService: SteamLibraryService = services.libraryService
    private val downloadManager: SteamDownloadManager = services.downloadManager
    private val profileRepository = FileProfileRepository(profileRepositoryPath)
    private val environmentComposer = DefaultEnvironmentComposer()
    private val processRunner = LocalProcessRunner(stateDir = stateDir)
    private val orchestrator = DefaultRuntimeOrchestrator(processRunner)

    private val taskProperties = Properties()
    private var cachedLibrary: List<SteamApp> = emptyList()

    init {
        ensureDefaultProfile()
        runBlocking { (downloadManager as? JavaSteamDownloadManager)?.bootstrapQueue() }
        loadTasks()
    }

    // ── Steam session ─────────────────────────────────────────────────────────

    fun login(username: String, password: String): SteamSessionSnapshot =
        runBlocking { sessionManager.login(username, password) }

    fun logout() = runBlocking { sessionManager.logout() }

    fun currentSession() = sessionManager.currentSession()

    // ── Library ───────────────────────────────────────────────────────────────

    fun library(): List<SteamApp> {
        cachedLibrary = runBlocking { libraryService.refreshOwnedApps() }
        return cachedLibrary
    }

    // ── Download ──────────────────────────────────────────────────────────────

    fun download(appId: Int) {
        runBlocking { downloadManager.enqueueApp(appId) }
        setTaskStatus(appId, "QUEUED")
        persistTasks()
    }

    fun markInstalled(appId: Int) {
        setTaskStatus(appId, "COMPLETED")
        persistTasks()
    }

    fun isInstalled(appId: Int): Boolean =
        taskProperties.getProperty("app.$appId.status") == "COMPLETED"

    // ── Launch ────────────────────────────────────────────────────────────────

    /**
     * Configures the runtime environment and launches the game via the runtime
     * orchestrator. Returns the runtime session ID.
     *
        * The current CLI launch command path emits a runtime marker command while
        * full install/launch manifest resolution is being integrated.
     */
    fun launch(appId: Int, profileId: String? = null): String {
        val profile = profileId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { profileRepository.getProfile(it) }
            ?: profileRepository.listProfiles().firstOrNull()
            ?: RuntimeProfile(
                id = "default",
                name = "Default",
                wineBinary = "wine",
                backend = RuntimeBackend.BOX64,
                env = mapOf("WINEDEBUG" to "-all"),
            )

        val env = environmentComposer.compose(
            profile = profile,
            overrides = mapOf(
                "GN_PROFILE_ID" to profile.id,
                "GN_APP_ID" to appId.toString(),
            ),
        )

        val sessionId = "gn-cli-$appId-${Instant.now().toEpochMilli()}"
        val plan = LaunchPlan(
            sessionId = sessionId,
            // Temporary launch marker command until full manifest launch path is integrated.
            command = listOf("echo", "[GameNative] Launching appId=$appId via ${profile.backend}/${profile.wineBinary}"),
            environment = env,
            workingDirectory = null,
        )

        runBlocking { orchestrator.launch(plan) }
        return sessionId
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun ensureDefaultProfile() {
        if (profileRepository.listProfiles().isEmpty()) {
            profileRepository.saveProfile(
                RuntimeProfile(
                    id = "default",
                    name = "Default",
                    wineBinary = "wine",
                    backend = RuntimeBackend.BOX64,
                    env = mapOf("WINEDEBUG" to "-all"),
                ),
            )
        }
    }

    private fun setTaskStatus(appId: Int, status: String) {
        taskProperties.setProperty("app.$appId.status", status)
        taskProperties.setProperty("app.$appId.updatedAt", Instant.now().toString())
    }

    private fun loadTasks() {
        if (!Files.exists(taskStorePath)) return
        Files.newInputStream(taskStorePath).use { taskProperties.load(it) }
    }

    private fun persistTasks() {
        val parent = taskStorePath.parent
        if (parent != null && !Files.exists(parent)) Files.createDirectories(parent)
        Files.newOutputStream(taskStorePath).use {
            taskProperties.store(it, "GameNative CLI install state")
        }
    }

    companion object {
        private fun defaultProfilePath(): Path {
            val dir = Path.of(System.getProperty("user.home"), ".config", "gamenative")
            if (!Files.exists(dir)) Files.createDirectories(dir)
            return dir.resolve("profiles.json")
        }

        private fun defaultTaskStorePath(): Path {
            val dir = Path.of(System.getProperty("user.home"), ".local", "state", "gamenative")
            if (!Files.exists(dir)) Files.createDirectories(dir)
            return dir.resolve("cli-tasks.properties")
        }

        private fun defaultStateDir(): Path {
            val dir = Path.of(System.getProperty("user.home"), ".local", "state", "gamenative", "sessions")
            if (!Files.exists(dir)) Files.createDirectories(dir)
            return dir
        }
    }
}
