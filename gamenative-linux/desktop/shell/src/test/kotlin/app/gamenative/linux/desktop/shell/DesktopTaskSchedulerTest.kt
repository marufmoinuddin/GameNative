package app.gamenative.linux.desktop.shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopTaskSchedulerTest {
    @Test
    fun persistsAndReloadsTasks() {
        val tempDir = Files.createTempDirectory("desktop-task-scheduler-test")
        val file = tempDir.resolve("tasks.properties")

        val scheduler = DesktopTaskScheduler(file)
        val queued = scheduler.enqueue(type = "download", payload = "620")
        scheduler.updateStatus(queued.id, DesktopTaskStatus.RUNNING)

        val reloaded = DesktopTaskScheduler(file)
        assertEquals(1, reloaded.listTasks().size)
        assertEquals(DesktopTaskStatus.RUNNING, reloaded.listTasks().first().status)

        val resumed = reloaded.resumePendingTasks()
        assertEquals(1, resumed)
        assertEquals(DesktopTaskStatus.QUEUED, reloaded.listTasks().first().status)
    }

    @Test
    fun updatesLatestByPayload() {
        val tempDir = Files.createTempDirectory("desktop-task-scheduler-test")
        val file = tempDir.resolve("tasks.properties")

        val scheduler = DesktopTaskScheduler(file)
        scheduler.enqueue(type = "download", payload = "620")
        scheduler.markLatestByPayload(type = "download", payload = "620", status = DesktopTaskStatus.CANCELED)

        val tasks = scheduler.listTasks()
        assertTrue(tasks.isNotEmpty())
        assertEquals(DesktopTaskStatus.CANCELED, tasks.first().status)
    }
}
