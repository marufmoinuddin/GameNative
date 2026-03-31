package app.gamenative.linux.runtime.demo

import app.gamenative.linux.runtime.FileRuntimeRecoveryService
import app.gamenative.linux.runtime.RuntimeStartupDecisionAdvisor
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val stateDir = if (args.isNotEmpty()) Path.of(args[0]) else Path.of("build/runtime-prototype-state")
    val reportPath = if (args.size > 1) {
        Path.of(args[1])
    } else {
        stateDir.resolve("recovery").resolve("startup-decision-report.txt")
    }

    val decision = FileRuntimeRecoveryService(stateDir).resolveStartupDecision()
    val advisor = RuntimeStartupDecisionAdvisor()
    val summary = advisor.renderSummary(decision)

    val parent = reportPath.parent
    if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent)
    }
    Files.writeString(reportPath, summary)

    println("Startup decision report: ${reportPath.toAbsolutePath()}")
    println("Startup decision: ${decision.action} (${decision.reason})")
    println("Recommendation: ${advisor.recommendation(decision)}")
}
