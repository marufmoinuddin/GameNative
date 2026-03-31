package app.gamenative.linux.desktop.shell

import app.gamenative.linux.store.steam.GatewayResult
import app.gamenative.linux.store.steam.SteamAuthGateway
import app.gamenative.linux.store.steam.SteamDownloadGateway
import app.gamenative.linux.store.steam.SteamDownloadQueueSnapshot
import app.gamenative.linux.store.steam.SteamLibraryGateway
import app.gamenative.linux.store.steam.SteamLibraryRecord
import java.nio.file.Files
import java.nio.file.Path

class DesktopShellPrototypeAuthGateway : SteamAuthGateway {
    override suspend fun connect(): GatewayResult {
        return GatewayResult(
            success = true,
            message = "Desktop prototype gateway connected",
        )
    }

    override suspend fun login(username: String, password: String): GatewayResult {
        if (username.isBlank() || password.isBlank()) {
            return GatewayResult(
                success = false,
                accountName = username.ifBlank { null },
                message = "Username/password cannot be blank",
            )
        }

        return GatewayResult(
            success = true,
            accountName = username,
            message = "Desktop prototype authentication succeeded",
        )
    }

    override suspend fun disconnect() {
    }
}

class DesktopShellPrototypeLibraryGateway : SteamLibraryGateway {
    private val apps = listOf(
        SteamLibraryRecord(appId = 620, name = "Portal 2"),
        SteamLibraryRecord(appId = 892970, name = "Valheim"),
        SteamLibraryRecord(appId = 1086940, name = "Baldur's Gate 3"),
    )

    override suspend fun fetchOwnedApps(): List<SteamLibraryRecord> = apps

    override suspend fun fetchOwnedApp(appId: Int): SteamLibraryRecord? {
        return apps.firstOrNull { it.appId == appId }
    }
}

class DesktopShellPrototypeDownloadGateway : SteamDownloadGateway {
    override suspend fun fetchQueueSnapshot(): SteamDownloadQueueSnapshot {
        return SteamDownloadQueueSnapshot(
            queuedAppIds = emptyList(),
            pausedAppIds = emptyList(),
        )
    }

    override suspend fun enqueue(appId: Int): Boolean = true

    override suspend fun pause(appId: Int): Boolean = true

    override suspend fun cancel(appId: Int): Boolean = true
}

class DesktopShellFixtureAuthGateway(
    private val usersFile: Path,
) : SteamAuthGateway {
    override suspend fun connect(): GatewayResult {
        return GatewayResult(
            success = true,
            message = "Desktop fixture gateway connected",
        )
    }

    override suspend fun login(username: String, password: String): GatewayResult {
        if (username.isBlank() || password.isBlank()) {
            return GatewayResult(
                success = false,
                accountName = username.ifBlank { null },
                message = "Username/password cannot be blank",
            )
        }

        val allowedUsers = loadAllowedUsers()
        if (allowedUsers.isEmpty() || allowedUsers.contains(username.trim())) {
            return GatewayResult(
                success = true,
                accountName = username.trim(),
                message = "Desktop fixture authentication succeeded",
            )
        }

        return GatewayResult(
            success = false,
            accountName = username.trim(),
            message = "Fixture authentication denied for user",
        )
    }

    override suspend fun disconnect() {
    }

    private fun loadAllowedUsers(): Set<String> {
        if (!Files.exists(usersFile)) {
            return emptySet()
        }

        return runCatching { Files.readAllLines(usersFile) }
            .getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
