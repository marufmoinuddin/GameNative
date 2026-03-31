package app.gamenative.linux.desktop.shell

import app.gamenative.linux.runtime.RuntimeBackend
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopShellControllerTest {
    @Test
    fun savesAndReloadsProfiles() {
        val tempDir = Files.createTempDirectory("desktop-shell-controller-test")
        val controller = DesktopShellController(
            profileRepositoryPath = tempDir.resolve("profiles.json"),
            settingsStorePath = tempDir.resolve("desktop-settings.properties"),
            taskStorePath = tempDir.resolve("desktop-tasks.properties"),
        )

        controller.saveProfile(
            id = "test-profile",
            name = "Test Profile",
            wineBinary = "wine",
            backend = RuntimeBackend.BOX64,
        )

        val state = controller.refreshState()
        assertEquals(1, state.profiles.size)
        assertEquals("test-profile", state.profiles.first().id)
        assertTrue(state.profiles.first().name.contains("Test"))
    }

    @Test
    fun supportsLibraryDetailsDownloadTasksAndSettings() {
        val tempDir = Files.createTempDirectory("desktop-shell-controller-workflow-test")
        val controller = DesktopShellController(
            profileRepositoryPath = tempDir.resolve("profiles.json"),
            settingsStorePath = tempDir.resolve("desktop-settings.properties"),
            taskStorePath = tempDir.resolve("desktop-tasks.properties"),
        )

        val library = controller.refreshLibrary()
        assertTrue(library.isNotEmpty())

        val detail = controller.selectGame(library.first().id)
        assertEquals(library.first().id, detail?.appId)

        controller.enqueueDownload(library.first().id)
        val tasks = controller.tasksSnapshot()
        assertTrue(tasks.isNotEmpty())
        assertEquals("download", tasks.first().type)

        controller.saveSettings(
            downloadRoot = "/tmp/gamenative-tests",
            diagnosticsPollingSeconds = 5,
        )
        val state = controller.refreshState()
        assertEquals("/tmp/gamenative-tests", state.settings.downloadRoot)
        assertEquals(5, state.settings.diagnosticsPollingSeconds)
    }
}
