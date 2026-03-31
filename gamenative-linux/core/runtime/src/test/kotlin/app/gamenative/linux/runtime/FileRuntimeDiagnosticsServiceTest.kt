package app.gamenative.linux.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileRuntimeDiagnosticsServiceTest {
    @Test
    fun returnsCapabilityAndLastSessionInfo() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-diagnostics-test")
        val sessionsDir = stateDir.resolve("sessions")
        val logsDir = stateDir.resolve("logs")
        Files.createDirectories(sessionsDir)
        Files.createDirectories(logsDir)

        Files.writeString(
            sessionsDir.resolve("session-1.properties"),
            "sessionId=session-1\n" +
                "pid=1234\n" +
                "startedAt=2026-03-30T10:00:00Z\n" +
                "command=sh -c echo hello\n" +
                "environmentSize=5\n" +
                "environmentSummary=GN_PROFILE_ID=prototype; WINEDEBUG=-all\n" +
                "profileId=prototype\n" +
                "exitCode=23\n" +
                "abnormalExit=true\n" +
                "terminationMode=graceful-timeout-force\n",
        )
        Files.writeString(logsDir.resolve("session-1.log"), "line1\nline2\n")
        Files.writeString(logsDir.resolve("session-1.stderr.log"), "err1\nerr2\n")

        val recoveryService = FileRuntimeRecoveryService(stateDir)
        recoveryService.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = "stale-1",
                pid = 999_000L,
                startedAt = "2026-03-30T09:59:00Z",
                command = "wine stale.exe",
                profileId = "prototype",
            ),
        )
        recoveryService.resolveStartupDecision()
        recoveryService.persistSupervisionRecommendation(
            SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            ),
            now = Instant.parse("2026-03-30T10:01:00Z"),
        )
        recoveryService.recordSupervisionEvent(
            action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
            reason = "multiple-abnormal-terminations-detected",
            sessionId = "session-1",
        )
        recoveryService.recordLaunchStateEvent(
            sessionId = "session-1",
            state = RuntimeLaunchAttemptState.QUEUED,
            reason = "launch-requested",
        )
        recoveryService.recordLaunchStateEvent(
            sessionId = "session-1",
            state = RuntimeLaunchAttemptState.LAUNCHED,
            reason = "process-started",
            pid = 1234,
        )
        recoveryService.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "session-1",
                exitCode = 23,
                abnormalExit = true,
                terminationMode = "graceful-timeout-force",
                endedAt = "2026-03-30T10:02:00Z",
            ),
        )

        val service = FileRuntimeDiagnosticsService(
            stateDir = stateDir,
            capabilityDetector = FakeCapabilityDetector,
        )

        val snapshot = service.snapshot()
        val session = snapshot.lastSession
        val crashBundle = snapshot.crashBundle
        val recovery = snapshot.recovery
        val startupDecision = snapshot.startupDecision
        val startupRecommendation = snapshot.startupRecommendation
        val supervisionRecommendation = snapshot.supervisionRecommendation
        val supervisionHold = snapshot.supervisionHold
        val retryAttempt = snapshot.retryAttempt
        val supervisionEvents = snapshot.supervisionEvents
        val launchStateTimeline = snapshot.launchStateTimeline
        val incidentSummary = snapshot.incidentSummary

        assertEquals(1, snapshot.schemaVersion)
        assertEquals(true, snapshot.capabilityReport.wineAvailable)
        assertNotNull(session)
        assertEquals("session-1", session.sessionId)
        assertEquals(1234L, session.pid)
        assertTrue(session.logTail.contains("line2"))
        assertNotNull(crashBundle)
        assertEquals("prototype", crashBundle.profileId)
        assertTrue(crashBundle.stdoutTail.contains("line2"))
        assertTrue(crashBundle.stderrTail.contains("err2"))
        assertTrue(crashBundle.environmentSummary.contains("GN_PROFILE_ID=prototype"))
        assertEquals(23, crashBundle.exitCode)
        assertEquals(true, crashBundle.abnormalExit)
        assertEquals("graceful-timeout-force", crashBundle.terminationMode)
        assertNotNull(recovery)
        assertEquals("stale-1", recovery.sessionId)
        assertEquals("previous-session-interrupted", recovery.reason)
        assertNotNull(startupDecision)
        assertEquals(RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION, startupDecision.action)
        assertEquals("stale-1", startupDecision.sessionId)
        assertNotNull(startupRecommendation)
        assertEquals(StartupRecommendationCode.STARTUP_RECOVER, startupRecommendation.code)
        assertEquals(RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION, startupRecommendation.action)
        assertTrue(startupRecommendation.recommendation.contains("recovery", ignoreCase = true))
        assertEquals(RecommendationSeverity.WARNING, startupRecommendation.severity)
        assertTrue(startupRecommendation.tags.contains("crash"))
        assertNotNull(supervisionRecommendation)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, supervisionRecommendation.action)
        assertNotNull(supervisionHold)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, supervisionHold.action)
        assertEquals("multiple-abnormal-terminations-detected", supervisionHold.reason)
        assertEquals(true, supervisionHold.active)
        assertNotNull(retryAttempt)
        assertEquals("session-1", retryAttempt.sessionId)
        assertEquals(1, retryAttempt.attempts)
        assertEquals("abnormal", retryAttempt.lastOutcome)
        assertEquals(1, supervisionEvents.size)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, supervisionEvents.first().action)
        assertEquals("session-1", supervisionEvents.first().sessionId)
        assertEquals(3, launchStateTimeline.size)
        assertEquals(RuntimeLaunchAttemptState.QUEUED, launchStateTimeline[0].state)
        assertEquals(RuntimeLaunchAttemptState.LAUNCHED, launchStateTimeline[1].state)
        assertEquals(RuntimeLaunchAttemptState.EXITED, launchStateTimeline[2].state)
        assertNotNull(incidentSummary)
        assertEquals(RuntimeIncidentSeverity.CRITICAL, incidentSummary.severity)
        assertTrue(incidentSummary.summary.contains("blocked", ignoreCase = true))
    }

    @Test
    fun returnsNullSessionWhenNoSessionFiles() {
        val stateDir = Files.createTempDirectory("gamenative-diagnostics-test")
        val service = FileRuntimeDiagnosticsService(
            stateDir = stateDir,
            capabilityDetector = FakeCapabilityDetector,
        )

        val snapshot = service.snapshot()
        assertNull(snapshot.lastSession)
        assertNull(snapshot.crashBundle)
    }

    @Test
    fun appliesProfilePolicyToIncidentSeverity() = runBlocking {
        val stateDir = Files.createTempDirectory("gamenative-diagnostics-threshold-test")
        val sessionsDir = stateDir.resolve("sessions")
        val logsDir = stateDir.resolve("logs")
        Files.createDirectories(sessionsDir)
        Files.createDirectories(logsDir)

        Files.writeString(
            sessionsDir.resolve("session-2.properties"),
            "sessionId=session-2\n" +
                "pid=2345\n" +
                "startedAt=2026-03-31T00:00:00Z\n" +
                "command=wine game.exe\n" +
                "environmentSize=5\n" +
                "environmentSummary=GN_PROFILE_ID=strict\n" +
                "profileId=strict\n" +
                "exitCode=1\n" +
                "abnormalExit=true\n" +
                "terminationMode=graceful\n",
        )

        val recoveryService = FileRuntimeRecoveryService(stateDir)
        recoveryService.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = "session-2",
                exitCode = 1,
                abnormalExit = true,
                terminationMode = "graceful",
                endedAt = "2026-03-31T00:01:00Z",
            ),
        )

        val profileRepo = FileProfileRepository(stateDir.resolve("profiles.json"))
        profileRepo.saveProfile(
            RuntimeProfile(
                id = "strict",
                name = "Strict",
                wineBinary = "wine",
                backend = RuntimeBackend.BOX64,
                supervisionPolicy = RuntimeSupervisionPolicy(
                    incidentWarningRetryThreshold = 1,
                    incidentCriticalRetryThreshold = 3,
                ),
            ),
        )

        val service = FileRuntimeDiagnosticsService(
            stateDir = stateDir,
            capabilityDetector = FakeCapabilityDetector,
            profileRepository = profileRepo,
        )

        val incidentSummary = service.snapshot().incidentSummary
        assertNotNull(incidentSummary)
        assertEquals(RuntimeIncidentSeverity.WARNING, incidentSummary.severity)
    }

    private object FakeCapabilityDetector : CapabilityDetector {
        override fun detect(): CapabilityReport {
            return CapabilityReport(
                wineAvailable = true,
                box64Available = false,
                fexAvailable = false,
                vulkanAvailable = true,
                diagnostics = listOf("box64 missing"),
            )
        }
    }
}
