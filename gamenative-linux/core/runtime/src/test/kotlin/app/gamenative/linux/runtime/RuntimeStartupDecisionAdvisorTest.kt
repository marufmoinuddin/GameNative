package app.gamenative.linux.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeStartupDecisionAdvisorTest {
    private val advisor = RuntimeStartupDecisionAdvisor()

    @Test
    fun mapsCleanStartToNormalRecommendation() {
        val decision = RuntimeStartupDecision(
            action = RuntimeStartupAction.CLEAN_START,
            reason = "no-active-session-marker",
            decidedAt = "2026-03-30T00:00:00Z",
        )

        val recommendation = advisor.recommendation(decision)
        assertTrue(recommendation.contains("normal startup", ignoreCase = true))

        val payload = advisor.toPayload(decision)
        assertEquals(StartupRecommendationCode.STARTUP_CLEAN, payload.code)
        assertEquals(RecommendationSeverity.INFO, payload.severity)
        assertTrue(payload.tags.contains("clean"))
    }

    @Test
    fun mapsAttachToSessionRecommendation() {
        val decision = RuntimeStartupDecision(
            action = RuntimeStartupAction.ATTACH_RUNNING_SESSION,
            reason = "active-session-still-running",
            sessionId = "s1",
            pid = 1001,
            decidedAt = "2026-03-30T00:00:00Z",
        )

        val recommendation = advisor.recommendation(decision)
        assertTrue(recommendation.contains("attach", ignoreCase = true))

        val payload = advisor.toPayload(decision)
        assertEquals(StartupRecommendationCode.STARTUP_ATTACH, payload.code)
        assertEquals(RecommendationSeverity.WARNING, payload.severity)
        assertTrue(payload.tags.contains("attach"))
    }

    @Test
    fun renderSummaryIncludesRecommendationAndAction() {
        val decision = RuntimeStartupDecision(
            action = RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION,
            reason = "previous-session-interrupted",
            sessionId = "s2",
            pid = 2002,
            decidedAt = "2026-03-30T00:00:00Z",
        )

        val summary = advisor.renderSummary(decision)
        assertTrue(summary.contains("startup.action=RECOVER_INTERRUPTED_SESSION"))
        assertTrue(summary.contains("startup.recommendation="))
        assertTrue(summary.contains("startup.severity=WARNING"))
        assertTrue(summary.contains("startup.tags=startup,recovery,crash"))
        assertTrue(summary.contains("startup.summary=STARTUP_RECOVER|WARNING|startup,recovery,crash"))
    }
}
