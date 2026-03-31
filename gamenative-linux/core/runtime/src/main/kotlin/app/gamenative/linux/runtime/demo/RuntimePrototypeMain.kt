package app.gamenative.linux.runtime.demo

import app.gamenative.linux.runtime.DefaultEnvironmentComposer
import app.gamenative.linux.runtime.DefaultRuntimeOrchestrator
import app.gamenative.linux.runtime.FileRuntimeDiagnosticsService
import app.gamenative.linux.runtime.FileProfileRepository
import app.gamenative.linux.runtime.FileRuntimeRecoveryService
import app.gamenative.linux.runtime.LaunchPlan
import app.gamenative.linux.runtime.LocalProcessRunner
import app.gamenative.linux.runtime.RecoveryBackedRuntimeSupervisionGate
import app.gamenative.linux.runtime.RuntimeBackend
import app.gamenative.linux.runtime.RuntimeCrashBundleArchiver
import app.gamenative.linux.runtime.RuntimeDiagnosticsReporter
import app.gamenative.linux.runtime.RuntimeProfile
import app.gamenative.linux.runtime.ShellCapabilityDetector
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val stateDir = Path.of("build/runtime-prototype-state")
    Files.createDirectories(stateDir)
    val configDir = stateDir.resolve("config")
    Files.createDirectories(configDir)
    val diagnosticsDir = stateDir.resolve("diagnostics")
    Files.createDirectories(diagnosticsDir)
    val recoveryService = FileRuntimeRecoveryService(stateDir)
    val startupDecision = recoveryService.resolveStartupDecision()

    val profileRepository = FileProfileRepository(configDir.resolve("profiles.json"))

    val profile = profileRepository.getProfile("prototype") ?: RuntimeProfile(
        id = "prototype",
        name = "Prototype Profile",
        wineBinary = "wine",
        backend = RuntimeBackend.BOX64,
        env = mapOf("WINEDEBUG" to "-all"),
    ).also { profileRepository.saveProfile(it) }

    val capabilityReport = ShellCapabilityDetector().detect()
    val capabilityReportFile = diagnosticsDir.resolve("capability-report.txt")
    val capabilityPayload = buildString {
        appendLine("wineAvailable=${capabilityReport.wineAvailable}")
        appendLine("box64Available=${capabilityReport.box64Available}")
        appendLine("fexAvailable=${capabilityReport.fexAvailable}")
        appendLine("vulkanAvailable=${capabilityReport.vulkanAvailable}")
        appendLine("diagnostics=${capabilityReport.diagnostics.joinToString("; ")}")
    }
    Files.writeString(capabilityReportFile, capabilityPayload)

    val environment = DefaultEnvironmentComposer().compose(
        profile = profile,
        overrides = mapOf("GN_PROFILE_ID" to profile.id),
    )
    val command = listOf(
        "sh",
        "-c",
        "echo runtime-prototype-ok && echo runtime-prototype-err 1>&2 && exit 23",
    )

    val orchestrator = DefaultRuntimeOrchestrator(
        processRunner = LocalProcessRunner(stateDir = stateDir),
        supervisionGate = RecoveryBackedRuntimeSupervisionGate(
            recoveryService = recoveryService,
            policyProvider = { profile.supervisionPolicy },
        ),
        recordSupervisionEvent = { event ->
            recoveryService.recordSupervisionEvent(
                action = event.action,
                reason = event.reason,
                sessionId = event.sessionId,
                backoffSeconds = event.backoffSeconds,
            )
        },
        recordLaunchStateEvent = { event ->
            recoveryService.recordLaunchStateEvent(
                sessionId = event.sessionId,
                state = event.state,
                reason = event.reason,
                pid = event.pid,
                backoffSeconds = event.backoffSeconds,
            )
        },
    )

    val session = orchestrator.launch(
        LaunchPlan(
            sessionId = "runtime-prototype",
            command = command,
            environment = environment,
            workingDirectory = null,
        ),
    )

    // Give the process enough time to emit output before stopping.
    Thread.sleep(300)
    orchestrator.stop(session.sessionId)

    val diagnosticsSnapshot = FileRuntimeDiagnosticsService(
        stateDir = stateDir,
        capabilityDetector = ShellCapabilityDetector(),
    ).snapshot()

    val diagnosticsSnapshotFile = diagnosticsDir.resolve("runtime-snapshot.txt")
    val snapshotPayload = buildString {
        appendLine("wineAvailable=${diagnosticsSnapshot.capabilityReport.wineAvailable}")
        appendLine("box64Available=${diagnosticsSnapshot.capabilityReport.box64Available}")
        appendLine("fexAvailable=${diagnosticsSnapshot.capabilityReport.fexAvailable}")
        appendLine("vulkanAvailable=${diagnosticsSnapshot.capabilityReport.vulkanAvailable}")
        appendLine("diagnostics=${diagnosticsSnapshot.capabilityReport.diagnostics.joinToString("; ")}")
        diagnosticsSnapshot.lastSession?.let { sessionInfo ->
            appendLine("lastSession.id=${sessionInfo.sessionId}")
            appendLine("lastSession.pid=${sessionInfo.pid}")
            appendLine("lastSession.command=${sessionInfo.command}")
            appendLine("lastSession.environmentSize=${sessionInfo.environmentSize}")
            appendLine("lastSession.startedAt=${sessionInfo.startedAt}")
        }
        diagnosticsSnapshot.crashBundle?.let { crash ->
            appendLine("crashBundle.sessionId=${crash.sessionId}")
            appendLine("crashBundle.profileId=${crash.profileId ?: ""}")
            appendLine("crashBundle.environmentSummary=${crash.environmentSummary}")
            appendLine("crashBundle.exitCode=${crash.exitCode}")
            appendLine("crashBundle.abnormalExit=${crash.abnormalExit}")
            appendLine("crashBundle.terminationMode=${crash.terminationMode}")
            appendLine("crashBundle.stdoutTail=${crash.stdoutTail}")
            appendLine("crashBundle.stderrTail=${crash.stderrTail}")
        }
        diagnosticsSnapshot.recovery?.let { recovery ->
            appendLine("recovery.sessionId=${recovery.sessionId}")
            appendLine("recovery.reason=${recovery.reason}")
            appendLine("recovery.detectedAt=${recovery.detectedAt}")
        }
        diagnosticsSnapshot.startupDecision?.let { decision ->
            appendLine("startupDecision.action=${decision.action}")
            appendLine("startupDecision.reason=${decision.reason}")
            appendLine("startupDecision.sessionId=${decision.sessionId ?: ""}")
            appendLine("startupDecision.decidedAt=${decision.decidedAt}")
        }
    }
    Files.writeString(diagnosticsSnapshotFile, snapshotPayload)

    val diagnosticsJsonFile = diagnosticsDir.resolve("runtime-snapshot.json")
    RuntimeDiagnosticsReporter(
        diagnosticsService = FileRuntimeDiagnosticsService(
            stateDir = stateDir,
            capabilityDetector = ShellCapabilityDetector(),
        ),
    ).writeSnapshot(diagnosticsJsonFile)

    val crashArchive = RuntimeCrashBundleArchiver(stateDir).archive(diagnosticsSnapshot)

    val sessionFile = stateDir.resolve("sessions").resolve("${session.sessionId}.properties")
    val logFile = stateDir.resolve("logs").resolve("${session.sessionId}.log")
    val profilesFile = configDir.resolve("profiles.json")

    println("Runtime prototype session: ${session.sessionId}")
    println("Session metadata: ${sessionFile.toAbsolutePath()}")
    println("Session log: ${logFile.toAbsolutePath()}")
    println("Profiles file: ${profilesFile.toAbsolutePath()}")
    println("Capability report: ${capabilityReportFile.toAbsolutePath()}")
    println("Runtime snapshot: ${diagnosticsSnapshotFile.toAbsolutePath()}")
    println("Runtime snapshot JSON: ${diagnosticsJsonFile.toAbsolutePath()}")
    println("Startup decision: ${startupDecision.action} (${startupDecision.reason})")
    if (crashArchive != null) {
        println("Crash bundle archive: ${crashArchive.toAbsolutePath()}")
    }
}
