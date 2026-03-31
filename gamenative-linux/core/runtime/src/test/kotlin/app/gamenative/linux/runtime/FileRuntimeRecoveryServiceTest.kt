package app.gamenative.linux.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileRuntimeRecoveryServiceTest {
    @Test
    fun detectsInterruptedSessionAndPersistsRecoveryRecord() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(
            stateDir = stateDir,
            isProcessAlive = { false },
        )

        service.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = "s1",
                pid = 999_001L,
                startedAt = "2026-03-30T00:00:00Z",
                command = "wine game.exe",
                profileId = "prototype",
            ),
        )

        val recovered = service.detectInterruptedSession()
        assertNotNull(recovered)
        assertEquals("s1", recovered.sessionId)
        assertEquals("previous-session-interrupted", recovered.reason)

        val latest = service.latestRecovery()
        assertNotNull(latest)
        assertEquals("s1", latest.sessionId)

        val decision = service.resolveStartupDecision()
        assertEquals(RuntimeStartupAction.CLEAN_START, decision.action)
    }

    @Test
    fun doesNotRecoverWhenProcessStillAlive() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(
            stateDir = stateDir,
            isProcessAlive = { true },
        )

        service.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = "s2",
                pid = 42L,
                startedAt = "2026-03-30T00:00:00Z",
                command = "wine game.exe",
            ),
        )

        val recovered = service.detectInterruptedSession()
        assertNull(recovered)

        val decision = service.resolveStartupDecision()
        assertEquals(RuntimeStartupAction.ATTACH_RUNNING_SESSION, decision.action)
        assertEquals("s2", decision.sessionId)
    }

    @Test
    fun clearsActiveMarkerOnSessionEnd() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)

        service.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = "s3",
                pid = 123L,
                startedAt = "2026-03-30T00:00:00Z",
                command = "wine game.exe",
            ),
        )

        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "s3",
                exitCode = 0,
                abnormalExit = false,
                terminationMode = "graceful",
                endedAt = "2026-03-30T00:01:00Z",
            ),
        )

        val recoveryDir = stateDir.resolve("recovery")
        val activeMarker = recoveryDir.resolve("active-session.json")
        val lastTermination = recoveryDir.resolve("last-termination.json")

        assertTrue(Files.exists(lastTermination))
        assertTrue(!Files.exists(activeMarker))
    }

    @Test
    fun resolvesInterruptedSessionStartupDecision() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(
            stateDir = stateDir,
            isProcessAlive = { false },
        )

        service.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = "s4",
                pid = 77L,
                startedAt = "2026-03-30T00:00:00Z",
                command = "wine crashed.exe",
            ),
        )

        val decision = service.resolveStartupDecision()
        assertEquals(RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION, decision.action)
        assertEquals("s4", decision.sessionId)

        val latest = service.latestStartupDecision()
        assertNotNull(latest)
        assertEquals(RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION, latest.action)
    }

    @Test
    fun storesRecentTerminationHistory() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)

        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "t1",
                exitCode = 1,
                abnormalExit = true,
                terminationMode = "force",
                endedAt = "2026-03-30T00:01:00Z",
            ),
        )
        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "t2",
                exitCode = 0,
                abnormalExit = false,
                terminationMode = "graceful",
                endedAt = "2026-03-30T00:02:00Z",
            ),
        )

        val recent = service.recentTerminations(limit = 10)
        assertEquals(2, recent.size)
        assertEquals("t1", recent[0].sessionId)
        assertEquals("t2", recent[1].sessionId)
    }

    @Test
    fun persistsDelayRetryHoldAndExpiresAfterRetryAt() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)
        val now = Instant.parse("2026-03-30T12:00:00Z")

        service.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-abnormal-termination",
                backoffSeconds = 30,
            ),
            now = now,
        )

        val heldNow = service.activeSupervisionRecommendation(now)
        assertNotNull(heldNow)
        assertEquals(SupervisionAction.DELAY_RETRY, heldNow.action)

        val heldAfter = service.activeSupervisionRecommendation(now.plusSeconds(40))
        assertNull(heldAfter)
    }

    @Test
    fun persistsManualInterventionHoldUntilClearedByProceed() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)
        val now = Instant.parse("2026-03-30T12:00:00Z")

        service.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            ),
            now = now,
        )

        val held = service.activeSupervisionRecommendation(now.plusSeconds(600))
        assertNotNull(held)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, held.action)

        service.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.PROCEED,
                reason = "runtime-stable",
            ),
            now = now.plusSeconds(601),
        )

        val cleared = service.activeSupervisionRecommendation(now.plusSeconds(602))
        assertNull(cleared)
    }

    @Test
    fun incrementsRetryAttemptsOnAbnormalExitAndClearsOnCleanExit() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)

        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "retry-1",
                exitCode = 23,
                abnormalExit = true,
                terminationMode = "graceful-timeout-force",
                endedAt = "2026-03-30T12:00:00Z",
            ),
        )
        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "retry-1",
                exitCode = 1,
                abnormalExit = true,
                terminationMode = "force",
                endedAt = "2026-03-30T12:01:00Z",
            ),
        )

        val attempt = service.retryAttemptFor("retry-1")
        assertNotNull(attempt)
        assertEquals(2, attempt.attempts)
        assertEquals("abnormal", attempt.lastOutcome)

        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "retry-1",
                exitCode = 0,
                abnormalExit = false,
                terminationMode = "graceful",
                endedAt = "2026-03-30T12:02:00Z",
            ),
        )

        assertNull(service.retryAttemptFor("retry-1"))
    }

    @Test
    fun recordsLaunchStateTimelineAndAutoAppendsExitedOnSessionEnd() {
        val stateDir = Files.createTempDirectory("gamenative-recovery-test")
        val service = FileRuntimeRecoveryService(stateDir = stateDir)

        service.recordLaunchStateEvent(
            sessionId = "timeline-1",
            state = RuntimeLaunchAttemptState.QUEUED,
            reason = "launch-requested",
            recordedAt = Instant.parse("2026-03-30T12:00:00Z"),
        )
        service.recordLaunchStateEvent(
            sessionId = "timeline-1",
            state = RuntimeLaunchAttemptState.LAUNCHED,
            reason = "process-started",
            pid = 42L,
            recordedAt = Instant.parse("2026-03-30T12:00:01Z"),
        )

        service.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "timeline-1",
                exitCode = 0,
                abnormalExit = false,
                terminationMode = "graceful",
                endedAt = "2026-03-30T12:00:02Z",
            ),
        )

        val timeline = service.launchStateTimeline("timeline-1")
        assertEquals(
            listOf(
                RuntimeLaunchAttemptState.QUEUED,
                RuntimeLaunchAttemptState.LAUNCHED,
                RuntimeLaunchAttemptState.EXITED,
            ),
            timeline.map { it.state },
        )
    }
}
