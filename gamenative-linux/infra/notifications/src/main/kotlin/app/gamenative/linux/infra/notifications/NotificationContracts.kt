package app.gamenative.linux.infra.notifications

data class UserNotification(
    val title: String,
    val message: String,
)

interface NotificationService {
    fun show(notification: UserNotification)
}
