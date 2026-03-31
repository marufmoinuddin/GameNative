package app.gamenative.linux.store.steam

/**
 * Narrow auth boundary for incremental JavaSteam integration.
 */
interface SteamAuthGateway {
    suspend fun connect(): GatewayResult
    suspend fun login(username: String, password: String): GatewayResult
    suspend fun disconnect()
}

data class GatewayResult(
    val success: Boolean,
    val accountName: String? = null,
    val message: String? = null,
)

class JavaSteamSessionManager(
    private val gateway: SteamAuthGateway,
) : SteamSessionManager {
    private var snapshot = SteamSessionSnapshot(state = SteamSessionState.DISCONNECTED)

    override suspend fun login(username: String, password: String): SteamSessionSnapshot {
        if (username.isBlank() || password.isBlank()) {
            snapshot = SteamSessionSnapshot(
                state = SteamSessionState.FAILED,
                accountName = username.ifBlank { null },
                message = "Username/password cannot be blank",
            )
            return snapshot
        }

        snapshot = SteamSessionSnapshot(
            state = SteamSessionState.CONNECTING,
            accountName = username,
            message = "Connecting to Steam",
        )

        val connectResult = gateway.connect()
        if (!connectResult.success) {
            snapshot = SteamSessionSnapshot(
                state = SteamSessionState.FAILED,
                accountName = username,
                message = connectResult.message ?: "Failed to connect to Steam",
            )
            return snapshot
        }

        snapshot = SteamSessionSnapshot(
            state = SteamSessionState.CONNECTED,
            accountName = username,
            message = connectResult.message,
        )

        val loginResult = gateway.login(username, password)
        snapshot = if (loginResult.success) {
            SteamSessionSnapshot(
                state = SteamSessionState.AUTHENTICATED,
                accountName = loginResult.accountName ?: username,
                message = loginResult.message,
            )
        } else {
            SteamSessionSnapshot(
                state = SteamSessionState.FAILED,
                accountName = username,
                message = loginResult.message ?: "Steam authentication failed",
            )
        }

        return snapshot
    }

    override suspend fun logout() {
        gateway.disconnect()
        snapshot = SteamSessionSnapshot(state = SteamSessionState.DISCONNECTED)
    }

    override fun currentSession(): SteamSessionSnapshot = snapshot
}
