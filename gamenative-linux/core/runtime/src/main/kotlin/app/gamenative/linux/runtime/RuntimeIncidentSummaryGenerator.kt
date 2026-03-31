package app.gamenative.linux.runtime

class RuntimeIncidentSummaryGenerator {
    fun generate(
        crashBundle: CrashBundle?,
        supervisionRecommendation: SupervisionRecommendation?,
        supervisionHold: SupervisionHoldDiagnostics?,
        retryAttempt: RetryAttemptDiagnostics?,
        launchStateTimeline: List<RuntimeLaunchStateEvent>,
        policy: RuntimeSupervisionPolicy = RuntimeSupervisionPolicy(),
    ): RuntimeIncidentSummary {
        val signals = buildList {
            if (crashBundle?.exitCode != null) add("exitCode=${crashBundle.exitCode}")
            if (crashBundle != null) add("terminationMode=${crashBundle.terminationMode}")
            if (retryAttempt != null) add("retryAttempts=${retryAttempt.attempts}")
            if (supervisionRecommendation != null) add("supervision=${supervisionRecommendation.action}")
            if (supervisionHold?.active == true) add("supervisionHoldActive=true")
            if (launchStateTimeline.isNotEmpty()) add("timelineStates=${launchStateTimeline.joinToString(",") { it.state.name }}")
        }

        if (supervisionRecommendation?.action == SupervisionAction.REQUIRE_MANUAL_INTERVENTION ||
            supervisionHold?.active == true && supervisionHold.action == SupervisionAction.REQUIRE_MANUAL_INTERVENTION
        ) {
            return RuntimeIncidentSummary(
                title = "Runtime launch blocked",
                summary = "Supervision policy blocked launch after repeated unstable terminations.",
                severity = RuntimeIncidentSeverity.CRITICAL,
                recommendedAction = "Review crash bundle and logs, then clear or override supervision hold before retry.",
                signals = signals,
            )
        }

        if (supervisionRecommendation?.action == SupervisionAction.DELAY_RETRY) {
            return RuntimeIncidentSummary(
                title = "Runtime launch delayed",
                summary = "Supervision policy applied retry backoff before next launch attempt.",
                severity = RuntimeIncidentSeverity.WARNING,
                recommendedAction = "Wait for retry window to expire or inspect recent crashes to reduce retries.",
                signals = signals,
            )
        }

        if (crashBundle?.abnormalExit == true) {
            val attempts = retryAttempt?.attempts ?: 0
            val severity = when {
                attempts >= policy.incidentCriticalRetryThreshold -> RuntimeIncidentSeverity.CRITICAL
                attempts >= policy.incidentWarningRetryThreshold -> RuntimeIncidentSeverity.WARNING
                else -> RuntimeIncidentSeverity.INFO
            }
            return RuntimeIncidentSummary(
                title = "Runtime exited abnormally",
                summary = "Latest runtime session ended unexpectedly.",
                severity = severity,
                recommendedAction = remediationHint(crashBundle),
                signals = signals,
            )
        }

        return RuntimeIncidentSummary(
            title = "Runtime healthy",
            summary = "No blocking supervision signals detected for the latest session.",
            severity = RuntimeIncidentSeverity.INFO,
            recommendedAction = "Proceed with normal runtime launch flow.",
            signals = signals,
        )
    }

    private fun remediationHint(crashBundle: CrashBundle): String {
        val stderr = crashBundle.stderrTail.lowercase()
        val exitCode = crashBundle.exitCode
        val terminationMode = crashBundle.terminationMode.lowercase()

        return when {
            exitCode == 139 || "segmentation fault" in stderr -> {
                "Suspected segmentation fault. Capture a backtrace and verify runtime/driver compatibility before retrying."
            }

            "vulkan" in stderr || "dxvk" in stderr -> {
                "Graphics initialization appears unstable. Check Vulkan/DXVK setup and try a safer graphics profile."
            }

            "esync" in stderr || "fsync" in stderr -> {
                "Synchronization layer failure detected. Toggle esync/fsync settings and relaunch with reduced concurrency."
            }

            terminationMode == "graceful-timeout-force" || terminationMode == "force" -> {
                "Runtime required force termination. Inspect hang indicators in logs and review launcher timeout settings."
            }

            else -> "Inspect stderr tail and crash bundle, then retry launch."
        }
    }
}
