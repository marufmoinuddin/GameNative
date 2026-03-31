package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ActiveRuntimeSessionMarker(
    val sessionId: String,
    val pid: Long,
    val startedAt: String,
    val command: String,
    val profileId: String? = null,
)

@Serializable
data class RuntimeTerminationRecord(
    val sessionId: String,
    val exitCode: Int,
    val abnormalExit: Boolean,
    val terminationMode: String,
    val endedAt: String,
)

@Serializable
data class RuntimeRecoveryRecord(
    val sessionId: String,
    val pid: Long,
    val startedAt: String,
    val command: String,
    val profileId: String? = null,
    val reason: String,
    val detectedAt: String,
)

@Serializable
enum class RuntimeStartupAction {
    CLEAN_START,
    ATTACH_RUNNING_SESSION,
    RECOVER_INTERRUPTED_SESSION,
}

@Serializable
data class RuntimeStartupDecision(
    val action: RuntimeStartupAction,
    val reason: String,
    val sessionId: String? = null,
    val pid: Long? = null,
    val decidedAt: String,
)

@Serializable
data class RuntimeSupervisionHold(
    val action: SupervisionAction,
    val reason: String,
    val retryNotBefore: String? = null,
    val recordedAt: String,
)

@Serializable
data class RuntimeRetryAttemptEntry(
    val sessionId: String,
    val attempts: Int,
    val lastOutcome: String,
    val updatedAt: String,
)

@Serializable
data class RuntimeSupervisionEvent(
    val action: SupervisionAction,
    val reason: String,
    val sessionId: String? = null,
    val backoffSeconds: Int = 0,
    val recordedAt: String,
)

@Serializable
enum class RuntimeLaunchAttemptState {
    QUEUED,
    GATED_DELAY,
    LAUNCHED,
    BLOCKED,
    EXITED,
}

@Serializable
data class RuntimeLaunchStateEvent(
    val sessionId: String,
    val state: RuntimeLaunchAttemptState,
    val reason: String = "",
    val pid: Long? = null,
    val backoffSeconds: Int = 0,
    val recordedAt: String,
)

class FileRuntimeRecoveryService(
    private val stateDir: Path,
    private val isProcessAlive: (Long) -> Boolean = ::defaultIsProcessAlive,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) {
    private val recoveryDir = stateDir.resolve("recovery")
    private val activeSessionFile = recoveryDir.resolve("active-session.json")
    private val lastTerminationFile = recoveryDir.resolve("last-termination.json")
    private val recentTerminationsFile = recoveryDir.resolve("recent-terminations.json")
    private val lastRecoveryFile = recoveryDir.resolve("last-recovery.json")
    private val startupDecisionFile = recoveryDir.resolve("startup-decision.json")
    private val supervisionHoldFile = recoveryDir.resolve("supervision-hold.json")
    private val retryAttemptsFile = recoveryDir.resolve("retry-attempts.json")
    private val supervisionEventsFile = recoveryDir.resolve("supervision-events.json")
    private val launchStateEventsFile = recoveryDir.resolve("launch-state-events.json")

    fun markSessionActive(marker: ActiveRuntimeSessionMarker) {
        ensureDir()
        Files.writeString(activeSessionFile, json.encodeToString(marker))
    }

    fun markSessionEnded(record: RuntimeTerminationRecord) {
        ensureDir()
        Files.writeString(lastTerminationFile, json.encodeToString(record))
        writeRecentTerminations(readRecentTerminations() + record)
        updateRetryAttempts(record)
        recordLaunchStateEvent(
            sessionId = record.sessionId,
            state = RuntimeLaunchAttemptState.EXITED,
            reason = "exit:${record.terminationMode}:code:${record.exitCode}",
            recordedAt = runCatching { Instant.parse(record.endedAt) }.getOrElse { Instant.now() },
        )

        val active = readActiveSession()
        if (active != null && active.sessionId == record.sessionId) {
            Files.deleteIfExists(activeSessionFile)
        }
    }

    fun recentTerminations(limit: Int = 10): List<RuntimeTerminationRecord> {
        return readRecentTerminations().takeLast(limit)
    }

    fun detectInterruptedSession(): RuntimeRecoveryRecord? {
        val active = readActiveSession() ?: return null
        if (active.pid > 0 && isProcessAlive(active.pid)) {
            return null
        }

        val recovery = RuntimeRecoveryRecord(
            sessionId = active.sessionId,
            pid = active.pid,
            startedAt = active.startedAt,
            command = active.command,
            profileId = active.profileId,
            reason = "previous-session-interrupted",
            detectedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
        )

        ensureDir()
        Files.writeString(lastRecoveryFile, json.encodeToString(recovery))
        Files.deleteIfExists(activeSessionFile)
        return recovery
    }

    fun resolveStartupDecision(): RuntimeStartupDecision {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val active = readActiveSession()

        val decision = if (active != null && active.pid > 0 && isProcessAlive(active.pid)) {
            RuntimeStartupDecision(
                action = RuntimeStartupAction.ATTACH_RUNNING_SESSION,
                reason = "active-session-still-running",
                sessionId = active.sessionId,
                pid = active.pid,
                decidedAt = now,
            )
        } else {
            val recovered = detectInterruptedSession()
            if (recovered != null) {
                RuntimeStartupDecision(
                    action = RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION,
                    reason = recovered.reason,
                    sessionId = recovered.sessionId,
                    pid = recovered.pid,
                    decidedAt = now,
                )
            } else {
                RuntimeStartupDecision(
                    action = RuntimeStartupAction.CLEAN_START,
                    reason = "no-active-session-marker",
                    decidedAt = now,
                )
            }
        }

        ensureDir()
        Files.writeString(startupDecisionFile, json.encodeToString(decision))
        return decision
    }

    fun persistSupervisionRecommendation(
        recommendation: SupervisionRecommendation,
        now: Instant = Instant.now(),
    ) {
        ensureDir()
        when (recommendation.action) {
            SupervisionAction.PROCEED -> {
                Files.deleteIfExists(supervisionHoldFile)
            }
            SupervisionAction.DELAY_RETRY -> {
                val retryAt = now.plusSeconds(recommendation.backoffSeconds.toLong())
                val hold = RuntimeSupervisionHold(
                    action = recommendation.action,
                    reason = recommendation.reason,
                    retryNotBefore = DateTimeFormatter.ISO_INSTANT.format(retryAt),
                    recordedAt = DateTimeFormatter.ISO_INSTANT.format(now),
                )
                Files.writeString(supervisionHoldFile, json.encodeToString(hold))
            }
            SupervisionAction.REQUIRE_MANUAL_INTERVENTION -> {
                val hold = RuntimeSupervisionHold(
                    action = recommendation.action,
                    reason = recommendation.reason,
                    retryNotBefore = null,
                    recordedAt = DateTimeFormatter.ISO_INSTANT.format(now),
                )
                Files.writeString(supervisionHoldFile, json.encodeToString(hold))
            }
        }
    }

    fun activeSupervisionRecommendation(now: Instant = Instant.now()): SupervisionRecommendation? {
        val hold = latestSupervisionHold() ?: return null

        return when (hold.action) {
            SupervisionAction.PROCEED -> {
                Files.deleteIfExists(supervisionHoldFile)
                null
            }
            SupervisionAction.REQUIRE_MANUAL_INTERVENTION -> {
                SupervisionRecommendation(
                    action = hold.action,
                    reason = hold.reason,
                )
            }
            SupervisionAction.DELAY_RETRY -> {
                val retryAt = hold.retryNotBefore?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (retryAt == null || !retryAt.isAfter(now)) {
                    Files.deleteIfExists(supervisionHoldFile)
                    null
                } else {
                    val remainingSeconds = Duration.between(now, retryAt).seconds
                    SupervisionRecommendation(
                        action = SupervisionAction.DELAY_RETRY,
                        reason = hold.reason,
                        backoffSeconds = remainingSeconds.coerceAtLeast(1).toInt(),
                    )
                }
            }
        }
    }

    fun latestRecovery(): RuntimeRecoveryRecord? {
        if (!Files.exists(lastRecoveryFile)) {
            return null
        }

        return runCatching {
            json.decodeFromString(RuntimeRecoveryRecord.serializer(), Files.readString(lastRecoveryFile))
        }.getOrNull()
    }

    fun latestStartupDecision(): RuntimeStartupDecision? {
        if (!Files.exists(startupDecisionFile)) {
            return null
        }

        return runCatching {
            json.decodeFromString(RuntimeStartupDecision.serializer(), Files.readString(startupDecisionFile))
        }.getOrNull()
    }

    fun latestSupervisionHold(): RuntimeSupervisionHold? {
        if (!Files.exists(supervisionHoldFile)) {
            return null
        }

        return runCatching {
            json.decodeFromString(RuntimeSupervisionHold.serializer(), Files.readString(supervisionHoldFile))
        }.getOrNull()
    }

    fun retryAttemptFor(sessionId: String): RuntimeRetryAttemptEntry? {
        if (sessionId.isBlank()) {
            return null
        }
        return readRetryAttempts().firstOrNull { it.sessionId == sessionId }
    }

    fun recordSupervisionEvent(
        action: SupervisionAction,
        reason: String,
        sessionId: String? = null,
        backoffSeconds: Int = 0,
        recordedAt: Instant = Instant.now(),
    ) {
        ensureDir()
        val current = readSupervisionEvents()
        val updated = current + RuntimeSupervisionEvent(
            action = action,
            reason = reason,
            sessionId = sessionId,
            backoffSeconds = backoffSeconds,
            recordedAt = DateTimeFormatter.ISO_INSTANT.format(recordedAt),
        )
        writeSupervisionEvents(updated)
    }

    fun recentSupervisionEvents(limit: Int = 20): List<RuntimeSupervisionEvent> {
        return readSupervisionEvents().takeLast(limit)
    }

    fun recordLaunchStateEvent(
        sessionId: String,
        state: RuntimeLaunchAttemptState,
        reason: String = "",
        pid: Long? = null,
        backoffSeconds: Int = 0,
        recordedAt: Instant = Instant.now(),
    ) {
        if (sessionId.isBlank()) {
            return
        }
        ensureDir()
        val current = readLaunchStateEvents()
        val updated = current + RuntimeLaunchStateEvent(
            sessionId = sessionId,
            state = state,
            reason = reason,
            pid = pid,
            backoffSeconds = backoffSeconds,
            recordedAt = DateTimeFormatter.ISO_INSTANT.format(recordedAt),
        )
        writeLaunchStateEvents(updated)
    }

    fun launchStateTimeline(sessionId: String, limit: Int = 50): List<RuntimeLaunchStateEvent> {
        if (sessionId.isBlank()) {
            return emptyList()
        }
        return readLaunchStateEvents().filter { it.sessionId == sessionId }.takeLast(limit)
    }

    fun readActiveSession(): ActiveRuntimeSessionMarker? {
        if (!Files.exists(activeSessionFile)) {
            return null
        }

        return runCatching {
            json.decodeFromString(ActiveRuntimeSessionMarker.serializer(), Files.readString(activeSessionFile))
        }.getOrNull()
    }

    private fun readRecentTerminations(): List<RuntimeTerminationRecord> {
        if (!Files.exists(recentTerminationsFile)) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeTerminationRecord.serializer()),
                Files.readString(recentTerminationsFile),
            )
        }.getOrElse { emptyList() }
    }

    private fun writeRecentTerminations(records: List<RuntimeTerminationRecord>) {
        val bounded = records.takeLast(30)
        Files.writeString(
            recentTerminationsFile,
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeTerminationRecord.serializer()),
                bounded,
            ),
        )
    }

    private fun updateRetryAttempts(record: RuntimeTerminationRecord) {
        val current = readRetryAttempts().associateBy { it.sessionId }.toMutableMap()
        if (record.abnormalExit || record.exitCode != 0) {
            val previousAttempts = current[record.sessionId]?.attempts ?: 0
            current[record.sessionId] = RuntimeRetryAttemptEntry(
                sessionId = record.sessionId,
                attempts = previousAttempts + 1,
                lastOutcome = "abnormal",
                updatedAt = record.endedAt,
            )
        } else {
            // Clean exits clear prior retry pressure for this session id.
            current.remove(record.sessionId)
        }
        writeRetryAttempts(current.values.toList())
    }

    private fun readRetryAttempts(): List<RuntimeRetryAttemptEntry> {
        if (!Files.exists(retryAttemptsFile)) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeRetryAttemptEntry.serializer()),
                Files.readString(retryAttemptsFile),
            )
        }.getOrElse { emptyList() }
    }

    private fun writeRetryAttempts(entries: List<RuntimeRetryAttemptEntry>) {
        val bounded = entries.takeLast(100)
        Files.writeString(
            retryAttemptsFile,
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeRetryAttemptEntry.serializer()),
                bounded,
            ),
        )
    }

    private fun readSupervisionEvents(): List<RuntimeSupervisionEvent> {
        if (!Files.exists(supervisionEventsFile)) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeSupervisionEvent.serializer()),
                Files.readString(supervisionEventsFile),
            )
        }.getOrElse { emptyList() }
    }

    private fun writeSupervisionEvents(events: List<RuntimeSupervisionEvent>) {
        val bounded = events.takeLast(200)
        Files.writeString(
            supervisionEventsFile,
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeSupervisionEvent.serializer()),
                bounded,
            ),
        )
    }

    private fun readLaunchStateEvents(): List<RuntimeLaunchStateEvent> {
        if (!Files.exists(launchStateEventsFile)) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeLaunchStateEvent.serializer()),
                Files.readString(launchStateEventsFile),
            )
        }.getOrElse { emptyList() }
    }

    private fun writeLaunchStateEvents(events: List<RuntimeLaunchStateEvent>) {
        val bounded = events.takeLast(400)
        Files.writeString(
            launchStateEventsFile,
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(RuntimeLaunchStateEvent.serializer()),
                bounded,
            ),
        )
    }

    private fun ensureDir() {
        if (!Files.exists(recoveryDir)) {
            Files.createDirectories(recoveryDir)
        }
    }

    companion object {
        private fun defaultIsProcessAlive(pid: Long): Boolean {
            return runCatching {
                ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
            }.getOrDefault(false)
        }
    }
}
