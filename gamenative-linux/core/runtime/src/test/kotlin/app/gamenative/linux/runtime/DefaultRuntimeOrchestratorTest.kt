package app.gamenative.linux.runtime

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultRuntimeOrchestratorTest {
    @Test
    fun launchAndStopSession() = runBlocking {
        val runner = FakeProcessRunner()
        val events = mutableListOf<RuntimeSupervisionEvent>()
        val launchEvents = mutableListOf<RuntimeLaunchStateEvent>()
        val orchestrator = DefaultRuntimeOrchestrator(
            processRunner = runner,
            supervisionGate = StaticSupervisionGate(
                SupervisionRecommendation(
                    action = SupervisionAction.PROCEED,
                    reason = "runtime-stable",
                ),
            ),
            recordSupervisionEvent = { events += it },
            recordLaunchStateEvent = { launchEvents += it },
        )

        val session = orchestrator.launch(
            LaunchPlan(
                sessionId = "s1",
                command = listOf("/bin/echo", "hello"),
                environment = emptyMap(),
            ),
        )

        assertEquals("s1", session.sessionId)
        assertEquals(1, events.size)
        assertEquals(SupervisionAction.PROCEED, events.first().action)
        assertEquals("s1", events.first().sessionId)
        assertEquals(listOf(RuntimeLaunchAttemptState.QUEUED, RuntimeLaunchAttemptState.LAUNCHED), launchEvents.map { it.state })
        assertTrue(orchestrator.stop("s1"))
        assertFalse(orchestrator.stop("s1"))
    }

    @Test
    fun delaysLaunchWhenSupervisionRequiresBackoff() = runBlocking {
        val runner = FakeProcessRunner()
        val slept = mutableListOf<Long>()
        val events = mutableListOf<RuntimeSupervisionEvent>()
        val launchEvents = mutableListOf<RuntimeLaunchStateEvent>()
        val orchestrator = DefaultRuntimeOrchestrator(
            processRunner = runner,
            supervisionGate = StaticSupervisionGate(
                SupervisionRecommendation(
                    action = SupervisionAction.DELAY_RETRY,
                    reason = "recent-abnormal-termination",
                    backoffSeconds = 2,
                ),
            ),
            sleep = { millis -> slept += millis },
            recordSupervisionEvent = { events += it },
            recordLaunchStateEvent = { launchEvents += it },
        )

        orchestrator.launch(
            LaunchPlan(
                sessionId = "s-delay",
                command = listOf("/bin/echo", "ok"),
                environment = emptyMap(),
            ),
        )

        assertEquals(listOf(2000L), slept)
        assertEquals(1, events.size)
        assertEquals(SupervisionAction.DELAY_RETRY, events.first().action)
        assertEquals(2, events.first().backoffSeconds)
        assertEquals(
            listOf(
                RuntimeLaunchAttemptState.QUEUED,
                RuntimeLaunchAttemptState.GATED_DELAY,
                RuntimeLaunchAttemptState.LAUNCHED,
            ),
            launchEvents.map { it.state },
        )
    }

    @Test
    fun blocksLaunchWhenManualInterventionRequired() = runBlocking {
        val runner = FakeProcessRunner()
        val events = mutableListOf<RuntimeSupervisionEvent>()
        val launchEvents = mutableListOf<RuntimeLaunchStateEvent>()
        val orchestrator = DefaultRuntimeOrchestrator(
            processRunner = runner,
            supervisionGate = StaticSupervisionGate(
                SupervisionRecommendation(
                    action = SupervisionAction.REQUIRE_MANUAL_INTERVENTION,
                    reason = "multiple-abnormal-terminations-detected",
                ),
            ),
            recordSupervisionEvent = { events += it },
            recordLaunchStateEvent = { launchEvents += it },
        )

        assertFailsWith<IllegalStateException> {
            orchestrator.launch(
                LaunchPlan(
                    sessionId = "s-block",
                    command = listOf("/bin/echo", "ok"),
                    environment = emptyMap(),
                ),
            )
        }
        assertEquals(1, events.size)
        assertEquals(SupervisionAction.REQUIRE_MANUAL_INTERVENTION, events.first().action)
        assertEquals(listOf(RuntimeLaunchAttemptState.QUEUED, RuntimeLaunchAttemptState.BLOCKED), launchEvents.map { it.state })
    }

    private class FakeProcessRunner : ProcessRunner {
        private val sessions = mutableSetOf<String>()

        override suspend fun start(
            sessionId: String,
            command: List<String>,
            environment: Map<String, String>,
            workingDirectory: String?,
        ): RuntimeSession {
            sessions.add(sessionId)
            return RuntimeSession(sessionId, 1234L)
        }

        override suspend fun stop(sessionId: String, force: Boolean): Boolean {
            return sessions.remove(sessionId)
        }
    }
}

private class StaticSupervisionGate(
    private val recommendation: SupervisionRecommendation,
) : RuntimeSupervisionGate {
    override suspend fun evaluate(): SupervisionRecommendation = recommendation
}
