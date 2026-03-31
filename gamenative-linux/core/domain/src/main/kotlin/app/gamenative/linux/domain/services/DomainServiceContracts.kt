package app.gamenative.linux.domain.services

import app.gamenative.data.LibraryItem
import app.gamenative.data.SteamApp

interface GameLibraryService {
    fun allItems(): List<LibraryItem>
    fun findByAppId(appId: String): LibraryItem?
}

interface LaunchPlanService {
    fun createForSteamApp(app: SteamApp, profileId: String): DomainLaunchPlan
}

interface GameFixApplicator {
    fun apply(store: StoreType, gameId: String, launchArguments: List<String>): GameFixResult
}

enum class StoreType {
    STEAM,
    GOG,
    EPIC,
    AMAZON,
}

data class DomainLaunchPlan(
    val executable: String,
    val args: List<String>,
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap(),
)

data class GameFixResult(
    val applied: Boolean,
    val notes: List<String> = emptyList(),
)
