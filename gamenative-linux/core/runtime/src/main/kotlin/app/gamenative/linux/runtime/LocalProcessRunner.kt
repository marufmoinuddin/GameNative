package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

data class RuntimeLaunchRecord(
    val sessionId: String,
    val command: List<String>,
    val environmentSize: Int,
    val environmentSummary: String,
    val profileId: String?,
    val startedAt: String,
    val pid: Long,
)

class LocalProcessRunner(
    private val stateDir: Path? = null,
    private val gracefulStopTimeoutMillis: Long = 5_000L,
) : ProcessRunner {
    private val processes = ConcurrentHashMap<String, Process>()
    private val recoveryService = stateDir?.let { FileRuntimeRecoveryService(it) }

    override suspend fun start(
        sessionId: String,
        command: List<String>,
        environment: Map<String, String>,
        workingDirectory: String?,
    ): RuntimeSession {
        require(command.isNotEmpty()) { "launch command cannot be empty" }

        val processBuilder = ProcessBuilder(command)

        if (workingDirectory != null) {
            processBuilder.directory(Path.of(workingDirectory).toFile())
        }

        processBuilder.environment().putAll(environment)

        val logsDir = stateDir?.resolve("logs")
        if (logsDir != null) {
            Files.createDirectories(logsDir)
            val stdoutFile = logsDir.resolve("$sessionId.log").toFile()
            val stderrFile = logsDir.resolve("$sessionId.stderr.log").toFile()
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(stdoutFile))
            processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(stderrFile))
        }

        val process = processBuilder.start()
        processes[sessionId] = process

        val startTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        writeLaunchRecord(
            RuntimeLaunchRecord(
                sessionId = sessionId,
                command = command,
                environmentSize = environment.size,
                environmentSummary = summarizeEnvironment(environment),
                profileId = environment["GN_PROFILE_ID"],
                startedAt = startTime,
                pid = process.pid(),
            ),
        )

        recoveryService?.markSessionActive(
            ActiveRuntimeSessionMarker(
                sessionId = sessionId,
                pid = process.pid(),
                startedAt = startTime,
                command = command.joinToString(" "),
                profileId = environment["GN_PROFILE_ID"],
            ),
        )

        return RuntimeSession(sessionId = sessionId, pid = process.pid())
    }

    override suspend fun stop(sessionId: String, force: Boolean): Boolean {
        val process = processes[sessionId] ?: return false

        val terminationMode = when {
            !process.isAlive -> {
                "already-exited"
            }
            force -> {
                destroyProcessTree(process, force = true)
                "force"
            }
            else -> {
                destroyProcessTree(process, force = false)
                val exitedGracefully = process.waitFor(gracefulStopTimeoutMillis, TimeUnit.MILLISECONDS)
                if (exitedGracefully) {
                    "graceful"
                } else {
                    destroyProcessTree(process, force = true)
                    "graceful-timeout-force"
                }
            }
        }

        if (!process.isAlive) {
            // process may have already exited naturally
        } else {
            process.waitFor()
        }

        val exitCode = process.exitValue()
        val abnormalExit = updateExitRecord(sessionId, exitCode, terminationMode)
        recoveryService?.markSessionEnded(
            RuntimeTerminationRecord(
                sessionId = sessionId,
                exitCode = exitCode,
                abnormalExit = abnormalExit,
                terminationMode = terminationMode,
                endedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            ),
        )
        processes.remove(sessionId)
        return true
    }

    private fun writeLaunchRecord(record: RuntimeLaunchRecord) {
        val recordsDir = stateDir?.resolve("sessions") ?: return
        Files.createDirectories(recordsDir)

        val safeCommand = record.command.joinToString(" ")
        val payload = buildString {
            appendLine("sessionId=${record.sessionId}")
            appendLine("pid=${record.pid}")
            appendLine("startedAt=${record.startedAt}")
            appendLine("command=$safeCommand")
            appendLine("environmentSize=${record.environmentSize}")
            appendLine("environmentSummary=${record.environmentSummary}")
            appendLine("profileId=${record.profileId ?: ""}")
        }

        Files.writeString(recordsDir.resolve("${record.sessionId}.properties"), payload)
    }

    private fun summarizeEnvironment(environment: Map<String, String>): String {
        if (environment.isEmpty()) {
            return ""
        }

        return environment
            .toSortedMap()
            .asSequence()
            .filter { (key, _) ->
                key.startsWith("GN_") ||
                    key.startsWith("WINE") ||
                    key.startsWith("BOX64") ||
                    key.startsWith("FEX")
            }
            .take(12)
            .joinToString("; ") { (key, value) ->
                val trimmedValue = if (value.length > 80) value.take(80) + "..." else value
                "$key=$trimmedValue"
            }
    }

    private fun updateExitRecord(sessionId: String, exitCode: Int, terminationMode: String): Boolean {
        val abnormalExit = ((terminationMode == "force" || terminationMode == "graceful-timeout-force") || exitCode != 0)

        val recordsDir = stateDir?.resolve("sessions") ?: return abnormalExit
        val sessionFile = recordsDir.resolve("${sessionId}.properties")
        if (!Files.exists(sessionFile)) {
            return abnormalExit
        }

        val props = Properties()
        Files.newInputStream(sessionFile).use { props.load(it) }
        props.setProperty("exitCode", exitCode.toString())
        props.setProperty("terminationMode", terminationMode)
        props.setProperty("abnormalExit", abnormalExit.toString())

        Files.newOutputStream(sessionFile).use { props.store(it, null) }
        return abnormalExit
    }

    private fun destroyProcessTree(process: Process, force: Boolean) {
        val descendants = process.toHandle().descendants().collect(Collectors.toList())

        if (force) {
            descendants.forEach { it.destroyForcibly() }
            process.destroyForcibly()
        } else {
            descendants.forEach { it.destroy() }
            process.destroy()
        }
    }
}
