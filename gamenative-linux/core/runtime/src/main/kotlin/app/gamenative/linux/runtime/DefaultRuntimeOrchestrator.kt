package app.gamenative.linux.runtime

import java.util.UUID
import kotlinx.coroutines.delay

class DefaultRuntimeOrchestrator(
    private val processRunner: ProcessRunner,
    private val supervisionGate: RuntimeSupervisionGate = AllowAllRuntimeSupervisionGate,
    private val sleep: suspend (Long) -> Unit = { millis -> delay(millis) },
    private val recordSupervisionEvent: suspend (RuntimeSupervisionEvent) -> Unit = {},
    private val recordLaunchStateEvent: suspend (RuntimeLaunchStateEvent) -> Unit = {},
) : RuntimeOrchestrator {
    private val activeSessions = mutableSetOf<String>()

    override suspend fun launch(plan: LaunchPlan): RuntimeSession {
        require(plan.command.isNotEmpty()) { "launch plan command cannot be empty" }

        val sessionId = if (plan.sessionId.isBlank()) {
            UUID.randomUUID().toString()
        } else {
            plan.sessionId
        }

        recordLaunchStateEvent(
            RuntimeLaunchStateEvent(
                sessionId = sessionId,
                state = RuntimeLaunchAttemptState.QUEUED,
                reason = "launch-requested",
                recordedAt = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
            ),
        )

        val recommendation = supervisionGate.evaluate()
        recordSupervisionEvent(
            RuntimeSupervisionEvent(
                action = recommendation.action,
                reason = recommendation.reason,
                sessionId = sessionId,
                backoffSeconds = recommendation.backoffSeconds,
                recordedAt = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
            ),
        )
        when (recommendation.action) {
            SupervisionAction.PROCEED -> {
                // Continue launch immediately.
            }
            SupervisionAction.DELAY_RETRY -> {
                recordLaunchStateEvent(
                    RuntimeLaunchStateEvent(
                        sessionId = sessionId,
                        state = RuntimeLaunchAttemptState.GATED_DELAY,
                        reason = recommendation.reason,
                        backoffSeconds = recommendation.backoffSeconds,
                        recordedAt = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
                    ),
                )
                if (recommendation.backoffSeconds > 0) {
                    sleep(recommendation.backoffSeconds * 1000L)
                }
            }
            SupervisionAction.REQUIRE_MANUAL_INTERVENTION -> {
                recordLaunchStateEvent(
                    RuntimeLaunchStateEvent(
                        sessionId = sessionId,
                        state = RuntimeLaunchAttemptState.BLOCKED,
                        reason = recommendation.reason,
                        recordedAt = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
                    ),
                )
                throw IllegalStateException(
                    "Runtime launch blocked by supervision policy: ${recommendation.reason}",
                )
            }
        }

        val session = processRunner.start(
            sessionId = sessionId,
            command = plan.command,
            environment = plan.environment,
            workingDirectory = plan.workingDirectory,
        )
        recordLaunchStateEvent(
            RuntimeLaunchStateEvent(
                sessionId = session.sessionId,
                state = RuntimeLaunchAttemptState.LAUNCHED,
                reason = "process-started",
                pid = session.pid,
                recordedAt = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
            ),
        )
        activeSessions.add(session.sessionId)

        return session
    }

    override suspend fun stop(sessionId: String): Boolean {
        if (!activeSessions.contains(sessionId)) {
            return false
        }

        val stopped = processRunner.stop(sessionId, force = false)
        if (stopped) {
            activeSessions.remove(sessionId)
        }

        return stopped
    }
}
