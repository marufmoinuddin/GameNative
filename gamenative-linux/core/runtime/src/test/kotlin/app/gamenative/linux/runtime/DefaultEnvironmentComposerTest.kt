package app.gamenative.linux.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultEnvironmentComposerTest {
    @Test
    fun mergesProfileAndOverridesAndAppliesBackendDefaults() {
        val composer = DefaultEnvironmentComposer()
        val profile = RuntimeProfile(
            id = "p1",
            name = "Box64",
            wineBinary = "wine",
            backend = RuntimeBackend.BOX64,
            env = mapOf("WINEDEBUG" to "-all"),
        )

        val env = composer.compose(profile, overrides = mapOf("WINEDEBUG" to "warn,err"))

        assertEquals("warn,err", env["WINEDEBUG"])
        assertEquals("1", env["BOX64_DYNAREC"])
    }
}
