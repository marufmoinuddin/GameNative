package app.gamenative.linux.infra.network

enum class NetworkState {
    ONLINE,
    OFFLINE,
    DEGRADED,
    UNKNOWN,
}

interface NetworkStateService {
    fun currentState(): NetworkState
}
