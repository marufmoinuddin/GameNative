package app.gamenative.events

interface AppEvent<T> : Event<T> {
    data object BackPressed : AppEvent<Unit>
    data class SetSystemUiVisibility(val visible: Boolean) : AppEvent<Unit>
    data class SetAllowedOrientation(val orientations: Set<String>) : AppEvent<Unit>
    data object HostDestroyed : AppEvent<Unit>
    data object GuestProcessTerminated : AppEvent<Unit>
    data class KeyInputEvent(
        val keyCode: Int,
        val action: Int,
        val unicodeChar: Int = 0,
    ) : AppEvent<Boolean>
    data class PointerInputEvent(
        val x: Float,
        val y: Float,
        val action: Int,
        val pointerCount: Int = 1,
    ) : AppEvent<Boolean>
    data object EndProcess : AppEvent<Unit>
    data class ExternalGameLaunch(val appId: String) : AppEvent<Unit>
    data class PromptSaveContainerConfig(val appId: String) : AppEvent<Unit>
    data class ShowGameFeedback(val appId: String) : AppEvent<Unit>
    data class ShowLaunchingOverlay(val appName: String) : AppEvent<Unit>
    data object HideLaunchingOverlay : AppEvent<Unit>
    data class SetBootingSplashText(val text: String) : AppEvent<Unit>
    data class DownloadPausedDueToConnectivity(val appId: Int) : AppEvent<Unit>
    data class DownloadStatusChanged(val appId: Int, val isDownloading: Boolean) : AppEvent<Unit>
    data class LibraryInstallStatusChanged(val appId: Int) : AppEvent<Unit>
    data class CustomGameImagesFetched(val appId: String) : AppEvent<Unit>
    data class GogAuthCodeReceived(val authCode: String) : AppEvent<Unit>
    data class EpicAuthCodeReceived(val authCode: String) : AppEvent<Unit>
    data object ServiceReady : AppEvent<Unit>
}
