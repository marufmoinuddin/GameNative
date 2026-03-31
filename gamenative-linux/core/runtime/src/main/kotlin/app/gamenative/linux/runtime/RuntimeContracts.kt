package app.gamenative.linux.runtime

import kotlinx.serialization.Serializable

interface RuntimeOrchestrator {
    suspend fun launch(plan: LaunchPlan): RuntimeSession
    suspend fun stop(sessionId: String): Boolean
}

interface ProcessRunner {
    suspend fun start(
        sessionId: String,
        command: List<String>,
        environment: Map<String, String>,
        workingDirectory: String?,
    ): RuntimeSession
    suspend fun stop(sessionId: String, force: Boolean = false): Boolean
}

interface EnvironmentComposer {
    fun compose(profile: RuntimeProfile, overrides: Map<String, String> = emptyMap()): Map<String, String>
}

interface ProfileRepository {
    fun listProfiles(): List<RuntimeProfile>
    fun getProfile(id: String): RuntimeProfile?
    fun saveProfile(profile: RuntimeProfile)
}

interface CapabilityDetector {
    fun detect(): CapabilityReport
}

@Serializable
data class RuntimeProfile(
    val id: String,
    val name: String,
    val wineBinary: String,
    val backend: RuntimeBackend,
    val env: Map<String, String> = emptyMap(),
    val supervisionPolicy: RuntimeSupervisionPolicy = RuntimeSupervisionPolicy(),
)

@Serializable
data class RuntimeSupervisionPolicy(
    val lookbackMinutes: Int = 15,
    val manualInterventionThreshold: Int = 3,
    val retryBackoffSecondsPerFailure: Int = 10,
    val recoveryRetryBackoffSecondsPerFailure: Int = 15,
    val incidentWarningRetryThreshold: Int = 2,
    val incidentCriticalRetryThreshold: Int = 4,
)

@Serializable
enum class RuntimeBackend {
    BOX64,
    FEX,
}

@Serializable
data class LaunchPlan(
    val sessionId: String,
    val command: List<String>,
    val environment: Map<String, String>,
    val workingDirectory: String? = null,
)

@Serializable
data class RuntimeSession(
    val sessionId: String,
    val pid: Long,
)

@Serializable
data class CapabilityReport(
    val wineAvailable: Boolean,
    val box64Available: Boolean,
    val fexAvailable: Boolean,
    val vulkanAvailable: Boolean,
    val diagnostics: List<String> = emptyList(),
)
