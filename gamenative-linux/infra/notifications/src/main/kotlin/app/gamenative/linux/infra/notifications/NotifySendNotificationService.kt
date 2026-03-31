package app.gamenative.linux.infra.notifications

class NotifySendNotificationService(
    private val commandExists: (String) -> Boolean = ::defaultCommandExists,
    private val commandRunner: (List<String>) -> Int = ::defaultCommandRunner,
    private val fallbackLogger: (String) -> Unit = { System.err.println(it) },
) : NotificationService {
    override fun show(notification: UserNotification) {
        if (!commandExists(NOTIFY_SEND_COMMAND)) {
            fallbackLogger("notify-send unavailable; ${notification.title}: ${notification.message}")
            return
        }

        val exitCode = commandRunner(listOf(NOTIFY_SEND_COMMAND, notification.title, notification.message))
        if (exitCode != 0) {
            fallbackLogger("notify-send failed with exit code $exitCode")
        }
    }

    companion object {
        private const val NOTIFY_SEND_COMMAND = "notify-send"

        private fun defaultCommandExists(command: String): Boolean {
            val process = ProcessBuilder("sh", "-c", "command -v $command >/dev/null 2>&1").start()
            return process.waitFor() == 0
        }

        private fun defaultCommandRunner(command: List<String>): Int {
            val process = ProcessBuilder(command).start()
            return process.waitFor()
        }
    }
}
