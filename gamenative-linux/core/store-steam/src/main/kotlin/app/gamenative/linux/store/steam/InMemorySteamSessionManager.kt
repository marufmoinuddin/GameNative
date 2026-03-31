package app.gamenative.linux.store.steam

class InMemorySteamSessionManager : SteamSessionManager {
    private var snapshot = SteamSessionSnapshot(state = SteamSessionState.DISCONNECTED)

    override suspend fun login(username: String, password: String): SteamSessionSnapshot {
        snapshot = if (username.isBlank() || password.isBlank()) {
            SteamSessionSnapshot(
                state = SteamSessionState.FAILED,
                accountName = username.ifBlank { null },
                message = "Username/password cannot be blank",
            )
        } else {
            SteamSessionSnapshot(
                state = SteamSessionState.AUTHENTICATED,
                accountName = username,
                message = null,
            )
        }

        return snapshot
    }

    override suspend fun logout() {
        snapshot = SteamSessionSnapshot(state = SteamSessionState.DISCONNECTED)
    }

    override fun currentSession(): SteamSessionSnapshot = snapshot
}
