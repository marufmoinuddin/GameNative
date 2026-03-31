package app.gamenative.linux.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FileProfileRepositoryTest {
    @Test
    fun savesAndReloadsProfilesFromDisk() {
        val root = Files.createTempDirectory("gamenative-runtime-profiles")
        val file = root.resolve("profiles.json")

        val repo = FileProfileRepository(file)
        repo.saveProfile(
            RuntimeProfile(
                id = "default",
                name = "Default",
                wineBinary = "wine",
                backend = RuntimeBackend.BOX64,
                env = mapOf("WINEDEBUG" to "-all"),
                supervisionPolicy = RuntimeSupervisionPolicy(
                    lookbackMinutes = 5,
                    manualInterventionThreshold = 2,
                    retryBackoffSecondsPerFailure = 3,
                    recoveryRetryBackoffSecondsPerFailure = 4,
                    incidentWarningRetryThreshold = 1,
                    incidentCriticalRetryThreshold = 2,
                ),
            ),
        )

        val reloaded = FileProfileRepository(file)
        val loaded = reloaded.getProfile("default")

        assertNotNull(loaded)
        assertEquals("Default", loaded.name)
        assertEquals(RuntimeBackend.BOX64, loaded.backend)
        assertEquals("-all", loaded.env["WINEDEBUG"])
        assertEquals(5, loaded.supervisionPolicy.lookbackMinutes)
        assertEquals(2, loaded.supervisionPolicy.manualInterventionThreshold)
        assertEquals(3, loaded.supervisionPolicy.retryBackoffSecondsPerFailure)
        assertEquals(4, loaded.supervisionPolicy.recoveryRetryBackoffSecondsPerFailure)
        assertEquals(1, loaded.supervisionPolicy.incidentWarningRetryThreshold)
        assertEquals(2, loaded.supervisionPolicy.incidentCriticalRetryThreshold)
    }

    @Test
    fun handlesMalformedJsonByStartingEmpty() {
        val root = Files.createTempDirectory("gamenative-runtime-profiles")
        val file = root.resolve("profiles.json")
        Files.writeString(file, "not-json")

        val repo = FileProfileRepository(file)

        assertEquals(0, repo.listProfiles().size)
    }
}
