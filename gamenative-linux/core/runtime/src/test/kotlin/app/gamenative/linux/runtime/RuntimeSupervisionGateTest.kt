package app.gamenative.linux.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeSupervisionGateTest {
    @Test
    fun returnsPersistedDelayRetryHoldBeforeAdvisor() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-supervision-gate-test")
        val recovery = FileRuntimeRecoveryService(stateDir)
        val now = Instant.parse("2026-03-30T12:00:00Z")
        recovery.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-crash-recovery",
                backoffSeconds = 45,
            ),
            now = now,
        )

        val gate = RecoveryBackedRuntimeSupervisionGate(
            recoveryService = recovery,
            advisor = RuntimeSupervisionAdvisor(nowProvider = { now }),
            nowProvider = { now.plusSeconds(10) },
        )

        val recommendation = gate.evaluate()
        assertEquals(SupervisionAction.DELAY_RETRY, recommendation.action)
        assertEquals("recent-crash-recovery", recommendation.reason)
    }

    @Test
    fun returnsPersistedManualHoldAcrossFreshGateInstance() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-supervision-gate-test")
        val recovery = FileRuntimeRecoveryService(stateDir)
        val now = Instant.parse("2026-03-30T12:00:00Z")
        recovery.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            ),
            now = now,
        )

        val gate = RecoveryBackedRuntimeSupervisionGate(
            recoveryService = FileRuntimeRecoveryService(stateDir),
            nowProvider = { now.plusSeconds(60) },
        )

        val recommendation = gate.evaluate()
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, recommendation.action)
    }
}
