package app.gamenative.linux.runtime

class RuntimeStartupDecisionAdvisor {
    fun recommendation(decision: RuntimeStartupDecision): String {
        return when (decision.action) {
            RuntimeStartupAction.CLEAN_START -> {
                "Proceed with normal startup flow. No stale runtime session was detected."
            }
            RuntimeStartupAction.ATTACH_RUNNING_SESSION -> {
                "Offer attach-to-session UI and live log tail; avoid launching a duplicate runtime process."
            }
            RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION -> {
                "Show recovery banner and suggest crash report export before next launch."
            }
        }
    }

    fun renderSummary(decision: RuntimeStartupDecision): String {
        val payload = toPayload(decision)
        return buildString {
            appendLine("startup.action=${decision.action}")
            appendLine("startup.reason=${decision.reason}")
            appendLine("startup.sessionId=${decision.sessionId ?: ""}")
            appendLine("startup.pid=${decision.pid ?: ""}")
            appendLine("startup.decidedAt=${decision.decidedAt}")
            appendLine("startup.code=${payload.code}")
            appendLine("startup.summary=${payload.code}|${payload.severity}|${payload.tags.joinToString(",")}")
            appendLine("startup.recommendation=${payload.recommendation}")
            appendLine("startup.severity=${payload.severity}")
            appendLine("startup.tags=${payload.tags.joinToString(",")}")
        }
    }

    fun toPayload(decision: RuntimeStartupDecision): StartupRecommendation {
        val code = when (decision.action) {
            RuntimeStartupAction.CLEAN_START -> StartupRecommendationCode.STARTUP_CLEAN
            RuntimeStartupAction.ATTACH_RUNNING_SESSION -> StartupRecommendationCode.STARTUP_ATTACH
            RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION -> StartupRecommendationCode.STARTUP_RECOVER
        }

        val severity = when (decision.action) {
            RuntimeStartupAction.CLEAN_START -> RecommendationSeverity.INFO
            RuntimeStartupAction.ATTACH_RUNNING_SESSION -> RecommendationSeverity.WARNING
            RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION -> RecommendationSeverity.WARNING
        }

        val tags = when (decision.action) {
            RuntimeStartupAction.CLEAN_START -> listOf("startup", "clean")
            RuntimeStartupAction.ATTACH_RUNNING_SESSION -> listOf("startup", "attach", "session")
            RuntimeStartupAction.RECOVER_INTERRUPTED_SESSION -> listOf("startup", "recovery", "crash")
        }

        return StartupRecommendation(
            code = code,
            action = decision.action,
            recommendation = recommendation(decision),
            severity = severity,
            tags = tags,
        )
    }
}
