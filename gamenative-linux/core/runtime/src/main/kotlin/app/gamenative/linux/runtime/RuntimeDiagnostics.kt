package app.gamenative.linux.runtime

import kotlinx.serialization.Serializable

@Serializable
data class SessionDiagnostics(
    val sessionId: String,
    val pid: Long,
    val startedAt: String,
    val command: String,
    val environmentSize: Int,
    val logTail: String,
)

@Serializable
data class RuntimeDiagnosticsSnapshot(
    val schemaVersion: Int = 1,
    val capabilityReport: CapabilityReport,
    val lastSession: SessionDiagnostics? = null,
    val crashBundle: CrashBundle? = null,
    val recovery: RuntimeRecoveryRecord? = null,
    val startupDecision: RuntimeStartupDecision? = null,
    val startupRecommendation: StartupRecommendation? = null,
    val supervisionRecommendation: SupervisionRecommendation? = null,
    val supervisionHold: SupervisionHoldDiagnostics? = null,
    val retryAttempt: RetryAttemptDiagnostics? = null,
    val supervisionEvents: List<RuntimeSupervisionEvent> = emptyList(),
    val launchStateTimeline: List<RuntimeLaunchStateEvent> = emptyList(),
    val incidentSummary: RuntimeIncidentSummary? = null,
)

@Serializable
data class SupervisionRecommendation(
    val action: SupervisionAction,
    val reason: String,
    val backoffSeconds: Int = 0,
)

@Serializable
enum class SupervisionAction {
    PROCEED,
    DELAY_RETRY,
    REQUIRE_MANUAL_INTERVENTION,
}

@Serializable
data class SupervisionHoldDiagnostics(
    val action: SupervisionAction,
    val reason: String,
    val retryNotBefore: String? = null,
    val remainingBackoffSeconds: Int? = null,
    val active: Boolean,
)

@Serializable
data class RetryAttemptDiagnostics(
    val sessionId: String,
    val attempts: Int,
    val lastOutcome: String,
    val updatedAt: String,
)

@Serializable
data class RuntimeIncidentSummary(
    val title: String,
    val summary: String,
    val severity: RuntimeIncidentSeverity,
    val recommendedAction: String,
    val signals: List<String> = emptyList(),
)

@Serializable
enum class RuntimeIncidentSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

@Serializable
data class StartupRecommendation(
    val code: StartupRecommendationCode,
    val action: RuntimeStartupAction,
    val recommendation: String,
    val severity: RecommendationSeverity,
    val tags: List<String> = emptyList(),
)

@Serializable
enum class StartupRecommendationCode {
    STARTUP_CLEAN,
    STARTUP_ATTACH,
    STARTUP_RECOVER,
}

@Serializable
enum class RecommendationSeverity {
    INFO,
    WARNING,
}

@Serializable
data class CrashBundle(
    val sessionId: String,
    val profileId: String? = null,
    val command: String,
    val environmentSummary: String,
    val stdoutTail: String,
    val stderrTail: String,
    val exitCode: Int? = null,
    val abnormalExit: Boolean = false,
    val terminationMode: String = "unknown",
)

interface RuntimeDiagnosticsService {
    fun snapshot(): RuntimeDiagnosticsSnapshot
}
