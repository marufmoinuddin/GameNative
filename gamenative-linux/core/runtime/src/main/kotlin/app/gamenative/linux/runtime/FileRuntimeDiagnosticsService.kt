package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class FileRuntimeDiagnosticsService(
    private val stateDir: Path,
    private val capabilityDetector: CapabilityDetector,
    private val profileRepository: ProfileRepository? = null,
    private val logTailLines: Int = 50,
) : RuntimeDiagnosticsService {
    override fun snapshot(): RuntimeDiagnosticsSnapshot {
        val recoveryService = FileRuntimeRecoveryService(stateDir)
        val capability = capabilityDetector.detect()
        val session = readLastSession()
        val crashBundle = session?.let { readCrashBundle(it) }
        val recovery = recoveryService.latestRecovery()
        val startupDecision = recoveryService.latestStartupDecision()
        val startupRecommendation = startupDecision?.let { RuntimeStartupDecisionAdvisor().toPayload(it) }
        val activeHoldRecommendation = recoveryService.activeSupervisionRecommendation()
        val supervisionRecommendation = activeHoldRecommendation ?: RuntimeSupervisionAdvisor().recommendation(
            startupDecision = startupDecision,
            recentTerminations = recoveryService.recentTerminations(),
        )
        val supervisionHold = recoveryService.latestSupervisionHold()?.let { hold ->
            SupervisionHoldDiagnostics(
                action = hold.action,
                reason = hold.reason,
                retryNotBefore = hold.retryNotBefore,
                remainingBackoffSeconds = if (activeHoldRecommendation?.action == SupervisionAction.DELAY_RETRY) {
                    activeHoldRecommendation.backoffSeconds
                } else {
                    null
                },
                active = activeHoldRecommendation?.action == hold.action,
            )
        }
        val retryAttempt = session?.let { sessionInfo ->
            recoveryService.retryAttemptFor(sessionInfo.sessionId)?.let { entry ->
                RetryAttemptDiagnostics(
                    sessionId = entry.sessionId,
                    attempts = entry.attempts,
                    lastOutcome = entry.lastOutcome,
                    updatedAt = entry.updatedAt,
                )
            }
        }
        val supervisionEvents = recoveryService.recentSupervisionEvents()
        val launchStateTimeline = session?.let { recoveryService.launchStateTimeline(it.sessionId) } ?: emptyList()
        val incidentPolicy = crashBundle?.profileId
            ?.let { profileRepository?.getProfile(it)?.supervisionPolicy }
            ?: RuntimeSupervisionPolicy()
        val incidentSummary = RuntimeIncidentSummaryGenerator().generate(
            crashBundle = crashBundle,
            supervisionRecommendation = supervisionRecommendation,
            supervisionHold = supervisionHold,
            retryAttempt = retryAttempt,
            launchStateTimeline = launchStateTimeline,
            policy = incidentPolicy,
        )
        return RuntimeDiagnosticsSnapshot(
            capabilityReport = capability,
            lastSession = session,
            crashBundle = crashBundle,
            recovery = recovery,
            startupDecision = startupDecision,
            startupRecommendation = startupRecommendation,
            supervisionRecommendation = supervisionRecommendation,
            supervisionHold = supervisionHold,
            retryAttempt = retryAttempt,
            supervisionEvents = supervisionEvents,
            launchStateTimeline = launchStateTimeline,
            incidentSummary = incidentSummary,
        )
    }

    private fun readLastSession(): SessionDiagnostics? {
        val sessionsDir = stateDir.resolve("sessions")
        if (!Files.exists(sessionsDir) || !Files.isDirectory(sessionsDir)) {
            return null
        }

        val latestSessionFile = Files.list(sessionsDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .max { a, b -> Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b)) }
                .orElse(null)
        } ?: return null

        val props = Properties()
        Files.newInputStream(latestSessionFile).use { props.load(it) }

        val sessionId = props.getProperty("sessionId") ?: return null
        val pid = props.getProperty("pid")?.toLongOrNull() ?: -1L
        val startedAt = props.getProperty("startedAt", "")
        val command = props.getProperty("command", "")
        val environmentSize = props.getProperty("environmentSize")?.toIntOrNull() ?: 0

        val logFile = stateDir.resolve("logs").resolve("$sessionId.log")
        val logTail = readLogTail(logFile)

        return SessionDiagnostics(
            sessionId = sessionId,
            pid = pid,
            startedAt = startedAt,
            command = command,
            environmentSize = environmentSize,
            logTail = logTail,
        )
    }

    private fun readCrashBundle(session: SessionDiagnostics): CrashBundle {
        val sessionFile = stateDir.resolve("sessions").resolve("${session.sessionId}.properties")
        val props = Properties()
        if (Files.exists(sessionFile)) {
            Files.newInputStream(sessionFile).use { props.load(it) }
        }

        val environmentSummary = props.getProperty("environmentSummary", "")
        val profileId = props.getProperty("profileId")
        val exitCode = props.getProperty("exitCode")?.toIntOrNull()
        val terminationMode = props.getProperty("terminationMode", "unknown")
        val abnormalExit = props.getProperty("abnormalExit")?.toBooleanStrictOrNull() ?: ((exitCode ?: 0) != 0)
        val stdoutTail = readLogTail(stateDir.resolve("logs").resolve("${session.sessionId}.log"))
        val stderrTail = readLogTail(stateDir.resolve("logs").resolve("${session.sessionId}.stderr.log"))

        return CrashBundle(
            sessionId = session.sessionId,
            profileId = profileId,
            command = session.command,
            environmentSummary = environmentSummary,
            stdoutTail = stdoutTail,
            stderrTail = stderrTail,
            exitCode = exitCode,
            abnormalExit = abnormalExit,
            terminationMode = terminationMode,
        )
    }

    private fun readLogTail(logFile: Path): String {
        if (!Files.exists(logFile)) {
            return ""
        }

        val lines = Files.readAllLines(logFile)
        return lines.takeLast(logTailLines).joinToString("\n")
    }
}
