package app.gamenative.linux.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class RuntimeDiagnosticsReporterTest {
    @Test
    fun writesJsonSnapshotToDisk() {
        val tempFile = Files.createTempDirectory("gamenative-diag-reporter").resolve("snapshot.json")
        val service = object : RuntimeDiagnosticsService {
            override fun snapshot(): RuntimeDiagnosticsSnapshot {
                return RuntimeDiagnosticsSnapshot(
                    capabilityReport = CapabilityReport(
                        wineAvailable = true,
                        box64Available = false,
                        fexAvailable = false,
                        vulkanAvailable = true,
                        diagnostics = listOf("box64 missing"),
                    ),
                    lastSession = SessionDiagnostics(
                        sessionId = "s1",
                        pid = 42,
                        startedAt = "2026-03-30T00:00:00Z",
                        command = "echo ok",
                        environmentSize = 3,
                        logTail = "ok",
                    ),
                    startupRecommendation = StartupRecommendation(
                        code = StartupRecommendationCode.STARTUP_CLEAN,
                        action = RuntimeStartupAction.CLEAN_START,
                        recommendation = "Proceed with normal startup flow. No stale runtime session was detected.",
                        severity = RecommendationSeverity.INFO,
                        tags = listOf("startup", "clean"),
                    ),
                    supervisionHold = SupervisionHoldDiagnostics(
                        action = SupervisionAction.DELAY_RETRY,
                        reason = "recent-crash-recovery",
                        retryNotBefore = "2026-03-30T12:00:30Z",
                        remainingBackoffSeconds = 30,
                        active = true,
                    ),
                    retryAttempt = RetryAttemptDiagnostics(
                        sessionId = "s1",
                        attempts = 2,
                        lastOutcome = "abnormal",
                        updatedAt = "2026-03-30T11:59:00Z",
                    ),
                    supervisionEvents = listOf(
                        RuntimeSupervisionEvent(
                            action = SupervisionAction.DELAY_RETRY,
                            reason = "recent-crash-recovery",
                            sessionId = "s1",
                            backoffSeconds = 30,
                            recordedAt = "2026-03-30T11:59:30Z",
                        ),
                    ),
                    launchStateTimeline = listOf(
                        RuntimeLaunchStateEvent(
                            sessionId = "s1",
                            state = RuntimeLaunchAttemptState.QUEUED,
                            reason = "launch-requested",
                            recordedAt = "2026-03-30T11:58:00Z",
                        ),
                        RuntimeLaunchStateEvent(
                            sessionId = "s1",
                            state = RuntimeLaunchAttemptState.LAUNCHED,
                            reason = "process-started",
                            pid = 42,
                            recordedAt = "2026-03-30T11:58:01Z",
                        ),
                    ),
                    incidentSummary = RuntimeIncidentSummary(
                        title = "Runtime launch delayed",
                        summary = "Supervision policy applied retry backoff before next launch attempt.",
                        severity = RuntimeIncidentSeverity.WARNING,
                        recommendedAction = "Wait for retry window to expire or inspect recent crashes to reduce retries.",
                        signals = listOf("supervision=DELAY_RETRY"),
                    ),
                )
            }
        }

        val reporter = RuntimeDiagnosticsReporter(service)
        reporter.writeSnapshot(tempFile)

        val payload = Files.readString(tempFile)
        assertTrue(payload.contains("\"schemaVersion\": 1"))
        assertTrue(payload.contains("\"wineAvailable\": true"))
        assertTrue(payload.contains("\"sessionId\": \"s1\""))
        assertTrue(payload.contains("\"code\": \"STARTUP_CLEAN\""))
        assertTrue(payload.contains("\"supervisionHold\""))
        assertTrue(payload.contains("\"retryNotBefore\": \"2026-03-30T12:00:30Z\""))
        assertTrue(payload.contains("\"retryAttempt\""))
        assertTrue(payload.contains("\"attempts\": 2"))
        assertTrue(payload.contains("\"supervisionEvents\""))
        assertTrue(payload.contains("\"recordedAt\": \"2026-03-30T11:59:30Z\""))
        assertTrue(payload.contains("\"launchStateTimeline\""))
        assertTrue(payload.contains("\"state\": \"LAUNCHED\""))
        assertTrue(payload.contains("\"incidentSummary\""))
        assertTrue(payload.contains("\"severity\": \"WARNING\""))
    }
}
