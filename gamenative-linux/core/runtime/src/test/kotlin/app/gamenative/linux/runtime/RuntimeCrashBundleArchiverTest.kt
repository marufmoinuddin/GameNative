package app.gamenative.linux.runtime

import java.nio.file.Files
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuntimeCrashBundleArchiverTest {
    @Test
    fun archivesOnlyAbnormalCrashBundlesAndPrunesOld() {
        val stateDir = Files.createTempDirectory("gamenative-crash-archiver-test")
        val archiver = RuntimeCrashBundleArchiver(stateDir = stateDir, maxBundles = 2)

        val normalSnapshot = RuntimeDiagnosticsSnapshot(
            capabilityReport = CapabilityReport(true, true, false, true),
            crashBundle = CrashBundle(
                sessionId = "s-normal",
                command = "echo ok",
                environmentSummary = "",
                stdoutTail = "ok",
                stderrTail = "",
                exitCode = 0,
                abnormalExit = false,
            ),
        )

        val first = RuntimeDiagnosticsSnapshot(
            capabilityReport = CapabilityReport(true, true, false, true),
            crashBundle = CrashBundle(
                sessionId = "s1",
                profileId = "p1",
                command = "exit 1",
                environmentSummary = "GN_PROFILE_ID=p1",
                stdoutTail = "",
                stderrTail = "err",
                exitCode = 1,
                abnormalExit = true,
            ),
        )

        val second = first.copy(crashBundle = first.crashBundle?.copy(sessionId = "s2"))
        val third = first.copy(crashBundle = first.crashBundle?.copy(sessionId = "s3"))

        val skipped = archiver.archive(normalSnapshot)
        val f1 = archiver.archive(first)
        val f2 = archiver.archive(second)
        val f3 = archiver.archive(third)

        assertEquals(null, skipped)
        assertNotNull(f1)
        assertNotNull(f2)
        assertNotNull(f3)

        val crashesDir = stateDir.resolve("crashes")
        val files = Files.list(crashesDir).use { it.collect(Collectors.toList()) }
        assertEquals(2, files.size)

        val payload = Files.readString(f3)
        assertTrue(payload.contains("\"abnormalExit\": true"))
        assertTrue(payload.contains("\"sessionId\": \"s3\""))
    }
}
