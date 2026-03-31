package app.gamenative.linux.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InMemoryProfileRepositoryTest {
    @Test
    fun saveAndFetchProfile() {
        val repo = InMemoryProfileRepository()
        val profile = RuntimeProfile(
            id = "default",
            name = "Default",
            wineBinary = "wine",
            backend = RuntimeBackend.BOX64,
        )

        repo.saveProfile(profile)

        val loaded = repo.getProfile("default")
        assertNotNull(loaded)
        assertEquals("Default", loaded.name)
        assertEquals(1, repo.listProfiles().size)
    }
}
