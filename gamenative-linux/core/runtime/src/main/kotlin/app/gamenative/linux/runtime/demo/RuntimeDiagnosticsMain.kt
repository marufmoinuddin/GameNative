package app.gamenative.linux.runtime.demo

import app.gamenative.linux.runtime.FileRuntimeDiagnosticsService
import app.gamenative.linux.runtime.FileRuntimeRecoveryService
import app.gamenative.linux.runtime.RuntimeCrashBundleArchiver
import app.gamenative.linux.runtime.RuntimeDiagnosticsReporter
import app.gamenative.linux.runtime.ShellCapabilityDetector
import java.nio.file.Path

fun main(args: Array<String>) {
    val stateDir = if (args.isNotEmpty()) Path.of(args[0]) else Path.of("build/runtime-prototype-state")
    val outputPath = if (args.size > 1) Path.of(args[1]) else stateDir.resolve("diagnostics").resolve("runtime-snapshot.json")

    val diagnosticsService = FileRuntimeDiagnosticsService(
        stateDir = stateDir,
        capabilityDetector = ShellCapabilityDetector(),
    )
    val startupDecision = FileRuntimeRecoveryService(stateDir).resolveStartupDecision()
    val reporter = RuntimeDiagnosticsReporter(diagnosticsService)
    val snapshot = diagnosticsService.snapshot()
    val archived = RuntimeCrashBundleArchiver(stateDir).archive(snapshot)

    val written = reporter.writeSnapshot(outputPath)
    println("Runtime diagnostics JSON: ${written.toAbsolutePath()}")
    println("Startup decision: ${startupDecision.action} (${startupDecision.reason})")
    if (archived != null) {
        println("Crash bundle archive: ${archived.toAbsolutePath()}")
    }
}
