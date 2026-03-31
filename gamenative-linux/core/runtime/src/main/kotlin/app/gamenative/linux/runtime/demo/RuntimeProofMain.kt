package app.gamenative.linux.runtime.demo

import app.gamenative.linux.runtime.ShellCapabilityDetector
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    val proofDir = Path.of("build/runtime-proof")
    Files.createDirectories(proofDir)
    val osArch = System.getProperty("os.arch").lowercase()
    val requiresBox64 = osArch.contains("aarch64") || osArch.contains("arm64")

    val report = ShellCapabilityDetector().detect()
    val reportFile = proofDir.resolve("runtime-proof-report.txt")
    val reportPayload = buildString {
        appendLine("osArch=$osArch")
        appendLine("requiresBox64=$requiresBox64")
        appendLine("wineAvailable=${report.wineAvailable}")
        appendLine("box64Available=${report.box64Available}")
        appendLine("fexAvailable=${report.fexAvailable}")
        appendLine("vulkanAvailable=${report.vulkanAvailable}")
        appendLine("diagnostics=${report.diagnostics.joinToString("; ")}")
    }
    Files.writeString(reportFile, reportPayload)

    if (!report.wineAvailable || (requiresBox64 && !report.box64Available)) {
        val requirementText = if (requiresBox64) "Wine+Box64" else "Wine"
        throw IllegalStateException(
            "Runtime proof failed: $requirementText is required for this host architecture. " +
                "See ${reportFile.toAbsolutePath()} for details.",
        )
    }

    val smokeCommand = if (requiresBox64) {
        listOf("sh", "-c", "wine --version >/dev/null 2>&1 && box64 --help >/dev/null 2>&1")
    } else {
        listOf("sh", "-c", "wine --version >/dev/null 2>&1")
    }
    val smoke = ProcessBuilder(smokeCommand)
        .redirectErrorStream(true)
        .start()

    val exitCode = smoke.waitFor()
    if (exitCode != 0) {
        throw IllegalStateException(
            "Runtime proof failed: smoke command exited with code $exitCode. " +
                "See ${reportFile.toAbsolutePath()} for capability details.",
        )
    }

    val successFile = proofDir.resolve("runtime-proof-success.txt")
    val successText = if (requiresBox64) {
        "PASS: Wine+Box64 capability and smoke checks succeeded."
    } else {
        "PASS: Wine capability and smoke checks succeeded for non-ARM64 host."
    }
    Files.writeString(
        successFile,
        successText,
    )

    println("Runtime proof report: ${reportFile.toAbsolutePath()}")
    println("Runtime proof success: ${successFile.toAbsolutePath()}")
}
