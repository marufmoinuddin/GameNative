package app.gamenative.linux.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeSupervisionAdvisorTest {
    @Test
    fun recommendsProceedWhenNoRecentFailures() {
        val advisor = RuntimeSupervisionAdvisor(nowProvider = { Instant.parse("2026-03-30T12:00:00Z") })

        val recommendation = advisor.recommendation(
            startupDecision = RuntimeStartupDecision(
                action = RuntimeStartupAction.CLEAN_START,
                reason = "no-active-session-marker",
                decidedAt = "2026-03-30T12:00:00Z",
            ),
            recentTerminations = emptyList(),
        )

        assertEquals(SupervisionAction.PROCEED, recommendation.action)
        assertEquals("runtime-stable", recommendation.reason)
    }

    @Test
    fun recommendsDelayedRetryOnRecentRecoveryFailure() {
        val advisor = RuntimeSupervisionAdvisor(nowProvider = { Instant.parse("2026-03-30T12:00:00Z") })

        val recommendation = advisor.recommendation(
            startupDecision = RuntimeStartupDecision(
                action = RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION,
                reason = "previous-session-interrupted",
                decidedAt = "2026-03-30T12:00:00Z",
            ),
            recentTerminations = listOf(
                RuntimeTerminationRecord(
                    sessionId = "s1",
                    exitCode = 1,
                    abnormalExit = true,
                    terminationMode = "graceful-timeout-force",
                    endedAt = "2026-03-30T11:58:00Z",
                ),
            ),
        )

        assertEquals(SupervisionAction.DELAY_RETRY, recommendation.action)
        assertEquals(15, recommendation.backoffSeconds)
    }

    @Test
    fun requiresManualInterventionAfterThreeRecentFailures() {
        val advisor = RuntimeSupervisionAdvisor(nowProvider = { Instant.parse("2026-03-30T12:00:00Z") })

        val recommendation = advisor.recommendation(
            startupDecision = RuntimeStartupDecision(
                action = RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION,
                reason = "previous-session-interrupted",
                decidedAt = "2026-03-30T12:00:00Z",
            ),
            recentTerminations = listOf(
                failure("s1", "2026-03-30T11:59:00Z"),
                failure("s2", "2026-03-30T11:58:00Z"),
                failure("s3", "2026-03-30T11:57:00Z"),
            ),
        )

        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, recommendation.action)
        assertEquals("multiple-abnormal-terminations-detected", recommendation.reason)
    }

    @Test
    fun respectsCustomPolicyThresholdAndBackoff() {
        val advisor = RuntimeSupervisionAdvisor(nowProvider = { Instant.parse("2026-03-30T12:00:00Z") })

        val recommendation = advisor.recommendation(
            startupDecision = RuntimeStartupDecision(
                action = RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION,
                reason = "previous-session-interrupted",
                decidedAt = "2026-03-30T12:00:00Z",
            ),
            recentTerminations = listOf(
                failure("s1", "2026-03-30T11:59:00Z"),
                failure("s2", "2026-03-30T11:58:00Z"),
            ),
            policy = RuntimeSupervisionPolicy(
                lookbackMinutes = 30,
                manualInterventionThreshold = 5,
                retryBackoffSecondsPerFailure = 7,
                recoveryRetryBackoffSecondsPerFailure = 9,
            ),
        )

        assertEquals(SupervisionAction.DELAY_RETRY, recommendation.action)
        assertEquals(18, recommendation.backoffSeconds)
    }

    private fun failure(sessionId: String, endedAt: String): RuntimeTerminationRecord {
        return RuntimeTerminationRecord(
            sessionId = sessionId,
            exitCode = 1,
            abnormalExit = true,
            terminationMode = "force",
            endedAt = endedAt,
        )
    }
}
