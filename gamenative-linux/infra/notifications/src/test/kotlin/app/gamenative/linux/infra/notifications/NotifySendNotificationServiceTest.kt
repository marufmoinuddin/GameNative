package app.gamenative.linux.infra.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotifySendNotificationServiceTest {
    @Test
    fun invokesNotifySendWhenAvailable() {
        val commands = mutableListOf<List<String>>()
        val logs = mutableListOf<String>()

        val service = NotifySendNotificationService(
            commandExists = { it == "notify-send" },
            commandRunner = {
                commands.add(it)
                0
            },
            fallbackLogger = { logs.add(it) },
        )

        service.show(UserNotification(title = "Title", message = "Body"))

        assertEquals(1, commands.size)
        assertEquals(listOf("notify-send", "Title", "Body"), commands.first())
        assertTrue(logs.isEmpty())
    }

    @Test
    fun logsFallbackWhenNotifySendUnavailable() {
        val commands = mutableListOf<List<String>>()
        val logs = mutableListOf<String>()

        val service = NotifySendNotificationService(
            commandExists = { false },
            commandRunner = {
                commands.add(it)
                0
            },
            fallbackLogger = { logs.add(it) },
        )

        service.show(UserNotification(title = "Title", message = "Body"))

        assertTrue(commands.isEmpty())
        assertTrue(logs.any { it.contains("notify-send unavailable") })
    }

    @Test
    fun logsFailureWhenNotifySendReturnsNonZero() {
        val logs = mutableListOf<String>()

        val service = NotifySendNotificationService(
            commandExists = { true },
            commandRunner = { 1 },
            fallbackLogger = { logs.add(it) },
        )

        service.show(UserNotification(title = "Title", message = "Body"))

        assertTrue(logs.any { it.contains("exit code 1") })
    }
}
