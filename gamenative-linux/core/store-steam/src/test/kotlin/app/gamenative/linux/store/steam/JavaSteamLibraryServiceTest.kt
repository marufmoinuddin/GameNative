package app.gamenative.linux.store.steam

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JavaSteamLibraryServiceTest {
    @Test
    fun refreshLoadsOwnedAppsFromGateway() = runBlocking {
        val gateway = FakeSteamLibraryGateway(
            ownedApps = listOf(
                SteamLibraryRecord(appId = 570, name = "Dota 2"),
                SteamLibraryRecord(appId = 730, name = "Counter-Strike 2"),
            ),
            appById = emptyMap(),
        )
        val service = JavaSteamLibraryService(gateway)

        val apps = service.refreshOwnedApps()

        assertEquals(2, apps.size)
        assertEquals("Dota 2", apps.first { it.id == 570 }.name)
        assertEquals(listOf("fetchOwnedApps"), gateway.calls)
    }

    @Test
    fun getOwnedAppUsesCacheBeforeGatewayLookup() = runBlocking {
        val gateway = FakeSteamLibraryGateway(
            ownedApps = listOf(SteamLibraryRecord(appId = 570, name = "Dota 2")),
            appById = mapOf(570 to SteamLibraryRecord(appId = 570, name = "Different Name")),
        )
        val service = JavaSteamLibraryService(gateway)
        service.refreshOwnedApps()

        val app = service.getOwnedApp(570)

        assertEquals("Dota 2", app?.name)
        assertEquals(listOf("fetchOwnedApps"), gateway.calls)
    }

    @Test
    fun getOwnedAppFetchesFromGatewayWhenCacheMiss() = runBlocking {
        val gateway = FakeSteamLibraryGateway(
            ownedApps = emptyList(),
            appById = mapOf(440 to SteamLibraryRecord(appId = 440, name = "Team Fortress 2")),
        )
        val service = JavaSteamLibraryService(gateway)

        val app = service.getOwnedApp(440)

        assertEquals("Team Fortress 2", app?.name)
        assertEquals(listOf("fetchOwnedApp:440"), gateway.calls)
    }

    @Test
    fun getOwnedAppReturnsNullWhenGatewayMisses() = runBlocking {
        val gateway = FakeSteamLibraryGateway(
            ownedApps = emptyList(),
            appById = emptyMap(),
        )
        val service = JavaSteamLibraryService(gateway)

        val app = service.getOwnedApp(999)

        assertNull(app)
        assertEquals(listOf("fetchOwnedApp:999"), gateway.calls)
    }
}

private class FakeSteamLibraryGateway(
    private val ownedApps: List<SteamLibraryRecord>,
    private val appById: Map<Int, SteamLibraryRecord>,
) : SteamLibraryGateway {
    val calls = mutableListOf<String>()

    override suspend fun fetchOwnedApps(): List<SteamLibraryRecord> {
        calls += "fetchOwnedApps"
        return ownedApps
    }

    override suspend fun fetchOwnedApp(appId: Int): SteamLibraryRecord? {
        calls += "fetchOwnedApp:$appId"
        return appById[appId]
    }
}
