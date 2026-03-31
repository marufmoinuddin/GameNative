package app.gamenative.linux.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimePhase3IntegrationTest {
    @Test
    fun delayHoldLaunchPersistsSupervisionAndTimelineEvents() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-phase3-integration")
        val recovery = FileRuntimeRecoveryService(stateDir)
        val now = Instant.parse("2026-03-31T10:00:00Z")

        recovery.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-abnormal-termination",
                backoffSeconds = 2,
            ),
            now = now,
        )

        val runner = TrackingProcessRunner()
        val slept = mutableListOf<Long>()
        val orchestrator = DefaultRuntimeOrchestrator(
            processRunner = runner,
            supervisionGate = RecoveryBackedRuntimeSupervisionGate(
                recoveryService = recovery,
                nowProvider = { now.plusSeconds(1) },
            ),
            sleep = { millis -> slept += millis },
            recordSupervisionEvent = { event ->
                recovery.recordSupervisionEvent(
                    action = event.action,
                    reason = event.reason,
                    sessionId = event.sessionId,
                    backoffSeconds = event.backoffSeconds,
                    recordedAt = Instant.parse(event.recordedAt),
                )
            },
            recordLaunchStateEvent = { event ->
                recovery.recordLaunchStateEvent(
                    sessionId = event.sessionId,
                    state = event.state,
                    reason = event.reason,
                    pid = event.pid,
                    backoffSeconds = event.backoffSeconds,
                    recordedAt = Instant.parse(event.recordedAt),
                )
            },
        )

        val session = orchestrator.launch(
            LaunchPlan(
                sessionId = "phase3-delay-1",
                command = listOf("/bin/echo", "ok"),
                environment = emptyMap(),
            ),
        )

        assertEquals("phase3-delay-1", session.sessionId)
        assertEquals(listOf(1000L), slept)
        assertEquals(1, recovery.recentSupervisionEvents().size)
        assertEquals(SupervisionAction.DELAY_RETRY, recovery.recentSupervisionEvents().first().action)
        assertEquals(
            listOf(
                RuntimeLaunchAttemptState.QUEUED,
                RuntimeLaunchAttemptState.GATED_DELAY,
                RuntimeLaunchAttemptState.LAUNCHED,
            ),
            recovery.launchStateTimeline("phase3-delay-1").map { it.state },
        )
    }

    @Test
    fun manualHoldBlocksLaunchAcrossRestartedRecoveryService() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-phase3-integration")
        val now = Instant.parse("2026-03-31T11:00:00Z")

        FileRuntimeRecoveryService(stateDir).persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            ),
            now = now,
        )

        val recovery = FileRuntimeRecoveryService(stateDir)
        val orchestrator = DefaultRuntimeOrchestrator(
            processRunner = TrackingProcessRunner(),
            supervisionGate = RecoveryBackedRuntimeSupervisionGate(
                recoveryService = recovery,
                nowProvider = { now.plusSeconds(120) },
            ),
            recordSupervisionEvent = { event ->
                recovery.recordSupervisionEvent(
                    action = event.action,
                    reason = event.reason,
                    sessionId = event.sessionId,
                    backoffSeconds = event.backoffSeconds,
                    recordedAt = Instant.parse(event.recordedAt),
                )
            },
            recordLaunchStateEvent = { event ->
                recovery.recordLaunchStateEvent(
                    sessionId = event.sessionId,
                    state = event.state,
                    reason = event.reason,
                    pid = event.pid,
                    backoffSeconds = event.backoffSeconds,
                    recordedAt = Instant.parse(event.recordedAt),
                )
            },
        )

        assertFailsWith<IllegalStateException> {
            orchestrator.launch(
                LaunchPlan(
                    sessionId = "phase3-block-1",
                    command = listOf("/bin/echo", "ok"),
                    environment = emptyMap(),
                ),
            )
        }

        assertEquals(1, recovery.recentSupervisionEvents().size)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, recovery.recentSupervisionEvents().first().action)
        assertEquals(
            listOf(
                RuntimeLaunchAttemptState.QUEUED,
                RuntimeLaunchAttemptState.BLOCKED,
            ),
            recovery.launchStateTimeline("phase3-block-1").map { it.state },
        )
    }

    private class TrackingProcessRunner : ProcessRunner {
        override suspend fun start(
            sessionId: String,
            command: List<String>,
            environment: Map<String, String>,
            workingDirectory: String?,
        ): RuntimeSession {
            return RuntimeSession(sessionId = sessionId, pid = 4242L)
        }

        override suspend fun stop(sessionId: String, force: Boolean): Boolean {
            return true
        }
    }
}
