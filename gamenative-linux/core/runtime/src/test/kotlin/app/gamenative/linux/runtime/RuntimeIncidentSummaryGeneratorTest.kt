package app.gamenative.linux.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeIncidentSummaryGeneratorTest {
    private val generator = RuntimeIncidentSummaryGenerator()

    @Test
    fun emitsCriticalWhenManualInterventionIsActive() {
        val summary = generator.generate(
            crashBundle = null,
            supervisionRecommendation = SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            ),
            supervisionHold = SupervisionHoldDiagnostics(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
                active = true,
            ),
            retryAttempt = RetryAttemptDiagnostics(
                sessionId = "s1",
                attempts = 3,
                lastOutcome = "abnormal",
                updatedAt = "2026-03-30T12:00:00Z",
            ),
            launchStateTimeline = emptyList(),
        )

        assertEquals(RuntimeIncidentSeverity.CRITICAL, summary.severity)
        assertTrue(summary.title.contains("blocked", ignoreCase = true))
    }

    @Test
    fun emitsWarningWhenLaunchIsDelayed() {
        val summary = generator.generate(
            crashBundle = null,
            supervisionRecommendation = SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-crash-recovery",
                backoffSeconds = 30,
            ),
            supervisionHold = null,
            retryAttempt = null,
            launchStateTimeline = emptyList(),
        )

        assertEquals(RuntimeIncidentSeverity.WARNING, summary.severity)
        assertTrue(summary.summary.contains("retry", ignoreCase = true))
    }

    @Test
    fun emitsInfoWhenRuntimeLooksHealthy() {
        val summary = generator.generate(
            crashBundle = CrashBundle(
                sessionId = "s1",
                command = "echo ok",
                environmentSummary = "",
                stdoutTail = "ok",
                stderrTail = "",
                exitCode = 0,
                abnormalExit = false,
                terminationMode = "graceful",
            ),
            supervisionRecommendation = SupervisionRecommendation(
                action = SupervisionAction.PROCEED,
                reason = "runtime-stable",
            ),
            supervisionHold = null,
            retryAttempt = null,
            launchStateTimeline = listOf(
                RuntimeLaunchStateEvent(
                    sessionId = "s1",
                    state = RuntimeLaunchAttemptState.LAUNCHED,
                    recordedAt = "2026-03-30T12:00:00Z",
                ),
            ),
        )

        assertEquals(RuntimeIncidentSeverity.INFO, summary.severity)
        assertTrue(summary.title.contains("healthy", ignoreCase = true))
    }

    @Test
    fun usesPolicyThresholdsForAbnormalSeverity() {
        val summary = generator.generate(
            crashBundle = CrashBundle(
                sessionId = "s1",
                command = "wine game.exe",
                environmentSummary = "",
                stdoutTail = "",
                stderrTail = "",
                exitCode = 1,
                abnormalExit = true,
                terminationMode = "graceful",
            ),
            supervisionRecommendation = null,
            supervisionHold = null,
            retryAttempt = RetryAttemptDiagnostics(
                sessionId = "s1",
                attempts = 1,
                lastOutcome = "abnormal",
                updatedAt = "2026-03-31T00:00:00Z",
            ),
            launchStateTimeline = emptyList(),
            policy = RuntimeSupervisionPolicy(
                incidentWarningRetryThreshold = 1,
                incidentCriticalRetryThreshold = 3,
            ),
        )

        assertEquals(RuntimeIncidentSeverity.WARNING, summary.severity)
    }

    @Test
    fun emitsSignatureSpecificRemediationHints() {
        val summary = generator.generate(
            crashBundle = CrashBundle(
                sessionId = "s1",
                command = "wine game.exe",
                environmentSummary = "",
                stdoutTail = "",
                stderrTail = "Segmentation fault while initializing renderer",
                exitCode = 139,
                abnormalExit = true,
                terminationMode = "graceful",
            ),
            supervisionRecommendation = null,
            supervisionHold = null,
            retryAttempt = null,
            launchStateTimeline = emptyList(),
        )

        assertTrue(summary.recommendedAction.contains("backtrace", ignoreCase = true))
    }
}
