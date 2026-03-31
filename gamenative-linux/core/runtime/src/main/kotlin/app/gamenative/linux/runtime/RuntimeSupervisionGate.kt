package app.gamenative.linux.runtime

import java.time.Instant

interface RuntimeSupervisionGate {
    suspend fun evaluate(): SupervisionRecommendation
}

object AllowAllRuntimeSupervisionGate : RuntimeSupervisionGate {
    override suspend fun evaluate(): SupervisionRecommendation {
        return SupervisionRecommendation(
            action = SupervisionAction.PROCEED,
            reason = "runtime-stable",
        )
    }
}

class RecoveryBackedRuntimeSupervisionGate(
    private val recoveryService: FileRuntimeRecoveryService,
    private val advisor: RuntimeSupervisionAdvisor = RuntimeSupervisionAdvisor(),
    private val policyProvider: () -> RuntimeSupervisionPolicy = { RuntimeSupervisionPolicy() },
    private val nowProvider: () -> Instant = { Instant.now() },
) : RuntimeSupervisionGate {
    override suspend fun evaluate(): SupervisionRecommendation {
        val now = nowProvider()
        val held = recoveryService.activeSupervisionRecommendation(now)
        if (held != null) {
            return held
        }

        val recommendation = advisor.recommendation(
            startupDecision = recoveryService.latestStartupDecision(),
            recentTerminations = recoveryService.recentTerminations(),
            policy = policyProvider(),
        )
        recoveryService.persistSupervisionRecommendation(recommendation, now)
        return recommendation
    }
}
