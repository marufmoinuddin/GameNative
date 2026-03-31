package app.gamenative.linux.runtime

import java.time.Duration
import java.time.Instant

class RuntimeSupervisionAdvisor(
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun recommendation(
        startupDecision: RuntimeStartupDecision?,
        recentTerminations: List<RuntimeTerminationRecord>,
        policy: RuntimeSupervisionPolicy = RuntimeSupervisionPolicy(),
    ): SupervisionRecommendation {
        val unstableFailures = recentTerminations.count { isUnstableFailure(it) && isRecent(it, policy) }
        val threshold = policy.manualInterventionThreshold.coerceAtLeast(1)

        if (unstableFailures >= threshold) {
            return SupervisionRecommendation(
                action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                reason = "multiple-abnormal-terminations-detected",
            )
        }

        if (startupDecision?.action == RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION && unstableFailures > 0) {
            return SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-crash-recovery",
                backoffSeconds = unstableFailures * policy.recoveryRetryBackoffSecondsPerFailure.coerceAtLeast(0),
            )
        }

        if (unstableFailures > 0) {
            return SupervisionRecommendation(
                action = SupervisionAction.DELAY_RETRY,
                reason = "recent-abnormal-termination",
                backoffSeconds = unstableFailures * policy.retryBackoffSecondsPerFailure.coerceAtLeast(0),
            )
        }

        return SupervisionRecommendation(
            action = SupervisionAction.PROCEED,
            reason = "runtime-stable",
        )
    }

    private fun isUnstableFailure(record: RuntimeTerminationRecord): Boolean {
        return record.abnormalExit ||
            record.exitCode != 0 ||
            record.terminationMode == "force" ||
            record.terminationMode == "graceful-timeout-force"
    }

    private fun isRecent(record: RuntimeTerminationRecord, policy: RuntimeSupervisionPolicy): Boolean {
        val endedAt = runCatching { Instant.parse(record.endedAt) }.getOrNull() ?: return false
        val age = Duration.between(endedAt, nowProvider())
        return !age.isNegative && age <= Duration.ofMinutes(policy.lookbackMinutes.coerceAtLeast(1).toLong())
    }
}
