package app.gamenative.linux.runtime

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalProcessRunnerTest {
    @Test
    fun writesSessionMetadataAndLogs() = runBlocking {
        val tempDir = Files.createTempDirectory("gamenative-runtime-test")
        val runner = LocalProcessRunner(tempDir)

        val session = runner.start(
            sessionId = "session-1",
            command = listOf("sh", "-c", "echo runtime-ok && echo runtime-err 1>&2"),
            environment = mapOf(
                "GN_PROFILE_ID" to "test-profile",
                "WINEDEBUG" to "-all",
            ),
            workingDirectory = null,
        )

        val metadata = tempDir.resolve("sessions").resolve("session-1.properties")
        val logs = tempDir.resolve("logs").resolve("session-1.log")
        val stderrLogs = tempDir.resolve("logs").resolve("session-1.stderr.log")

        assertEquals("session-1", session.sessionId)
        assertTrue(Files.exists(metadata))
        assertTrue(Files.exists(logs))
        assertTrue(Files.exists(stderrLogs))

        val stdoutContent = Files.readString(logs)
        val stderrContent = Files.readString(stderrLogs)
        assertTrue(stdoutContent.contains("runtime-ok"))
        assertTrue(stderrContent.contains("runtime-err"))

        val metadataContent = Files.readString(metadata)
        assertTrue(metadataContent.contains("profileId=test-profile"))
        assertTrue(metadataContent.contains("environmentSummary=GN_PROFILE_ID=test-profile; WINEDEBUG=-all"))
        assertTrue(runner.stop("session-1", force = false))

        val metadataAfterStop = Files.readString(metadata)
        assertTrue(metadataAfterStop.contains("exitCode=0"))
        assertTrue(metadataAfterStop.contains("abnormalExit=false"))
        assertTrue(metadataAfterStop.contains("terminationMode=already-exited") || metadataAfterStop.contains("terminationMode=graceful"))
    }
}
