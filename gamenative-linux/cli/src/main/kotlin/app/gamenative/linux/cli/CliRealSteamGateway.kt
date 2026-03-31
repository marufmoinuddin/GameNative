package app.gamenative.linux.cli

import app.gamenative.linux.store.steam.GatewayResult
import app.gamenative.linux.store.steam.SteamAuthGateway
import app.gamenative.linux.store.steam.SteamLibraryGateway
import app.gamenative.linux.store.steam.SteamLibraryRecord
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

class CliRealSteamGateway : SteamAuthGateway, SteamLibraryGateway {
    private var steamClient: SteamClient? = null
    private var callbackManager: CallbackManager? = null
    private var steamUser: SteamUser? = null
    private val callbackSubscriptions = mutableListOf<Closeable>()

    private var player: Player? = null
    private var accountName: String? = null

    private val connected = AtomicBoolean(false)
    private val loggedOn = AtomicBoolean(false)
    private var lastLogOnResult: EResult? = null

    override suspend fun connect(): GatewayResult = withContext(Dispatchers.IO) {
        if (connected.get()) {
            return@withContext GatewayResult(success = true, message = "Already connected to Steam")
        }

        runCatching {
            ensureClient()
            steamClient?.connect()
            val connectedOk = waitUntil(timeoutMillis = 25_000L) {
                callbackManager?.runWaitCallbacks(200L)
                connected.get()
            }

            if (!connectedOk) {
                GatewayResult(success = false, message = "Timed out connecting to Steam")
            } else {
                GatewayResult(success = true, message = "Connected to Steam")
            }
        }.getOrElse { error ->
            GatewayResult(success = false, message = error.message ?: "Failed to connect to Steam")
        }
    }

    override suspend fun login(username: String, password: String): GatewayResult = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) {
            return@withContext GatewayResult(
                success = false,
                accountName = username.ifBlank { null },
                message = "Username/password cannot be blank",
            )
        }

        val connectResult = connect()
        if (!connectResult.success) {
            return@withContext GatewayResult(
                success = false,
                accountName = username,
                message = connectResult.message ?: "Failed to connect to Steam",
            )
        }

        try {
            val client = steamClient ?: return@withContext GatewayResult(
                success = false,
                accountName = username,
                message = "Steam client is not initialized",
            )

            val authDetails = AuthSessionDetails().apply {
                this.username = username.trim()
                this.password = password
                this.persistentSession = true
                this.authenticator = consoleAuthenticator()
                this.deviceFriendlyName = "GameNative CLI"
                this.clientOSType = EOSType.WinUnknown
            }

            val authSession = client.authentication.beginAuthSessionViaCredentials(authDetails).await()
            val pollResult = authSession.pollingWaitForResult().await()

            val resolvedAccountName = pollResult.accountName.ifBlank { username.trim() }
            val refreshToken = pollResult.refreshToken
            if (refreshToken.isBlank()) {
                return@withContext GatewayResult(
                    success = false,
                    accountName = resolvedAccountName,
                    message = "Steam returned an empty refresh token",
                )
            }

            loggedOn.set(false)
            lastLogOnResult = null

            steamUser?.logOn(
                LogOnDetails(
                    username = resolvedAccountName,
                    accessToken = refreshToken,
                    shouldRememberPassword = true,
                    loginID = 1,
                    machineName = "GameNative CLI",
                    chatMode = ChatMode.NEW_STEAM_CHAT,
                ),
            )

            val logOnSuccess = waitUntil(timeoutMillis = 30_000L) {
                callbackManager?.runWaitCallbacks(200L)
                loggedOn.get() || (lastLogOnResult != null && lastLogOnResult != EResult.OK)
            }

            if (!logOnSuccess || !loggedOn.get()) {
                val resultName = lastLogOnResult?.name ?: "UNKNOWN"
                return@withContext GatewayResult(
                    success = false,
                    accountName = resolvedAccountName,
                    message = "Steam logon failed: $resultName",
                )
            }

            val unifiedMessages = client.getHandler(SteamUnifiedMessages::class.java)
            player = unifiedMessages?.createService(Player::class.java)
            accountName = resolvedAccountName

            GatewayResult(
                success = true,
                accountName = resolvedAccountName,
                message = "Authenticated with Steam",
            )
        } catch (error: AuthenticationException) {
            GatewayResult(
                success = false,
                accountName = username,
                message = error.result?.name ?: error.message ?: "Steam authentication failed",
            )
        } catch (error: Exception) {
            GatewayResult(
                success = false,
                accountName = username,
                message = error.message ?: "Steam authentication failed",
            )
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { steamUser?.logOff() }
            runCatching { steamClient?.disconnect() }
            callbackSubscriptions.forEach { runCatching { it.close() } }
            callbackSubscriptions.clear()
            player = null
            steamUser = null
            callbackManager = null
            steamClient = null
            connected.set(false)
            loggedOn.set(false)
            lastLogOnResult = null
            accountName = null
        }
    }

    override suspend fun fetchOwnedApps(): List<SteamLibraryRecord> = withContext(Dispatchers.IO) {
        val steamId = steamClient?.steamID?.convertToUInt64() ?: return@withContext emptyList()
        val playerService = player ?: return@withContext emptyList()

        val request = SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request.newBuilder().apply {
            steamid = steamId
            includePlayedFreeGames = true
            includeFreeSub = true
            includeAppinfo = true
            includeExtendedAppinfo = true
        }.build()

        val result = runCatching { playerService.getOwnedGames(request).await() }.getOrNull() ?: return@withContext emptyList()
        if (result.result != EResult.OK) {
            return@withContext emptyList()
        }

        return@withContext result.body.gamesList.map { game ->
            SteamLibraryRecord(
                appId = game.appid,
                name = game.name.ifBlank { "App ${game.appid}" },
            )
        }
    }

    override suspend fun fetchOwnedApp(appId: Int): SteamLibraryRecord? {
        return fetchOwnedApps().firstOrNull { it.appId == appId }
    }

    private fun ensureClient() {
        if (steamClient != null && callbackManager != null && steamUser != null) {
            return
        }

        val configuration = SteamConfiguration.create {
            it.withConnectionTimeout(60_000L)
        }

        val client = SteamClient(configuration)
        val manager = CallbackManager(client)
        val user = client.getHandler(SteamUser::class.java)

        callbackSubscriptions += manager.subscribe(ConnectedCallback::class.java) {
            connected.set(true)
        }
        callbackSubscriptions += manager.subscribe(DisconnectedCallback::class.java) {
            connected.set(false)
            loggedOn.set(false)
        }
        callbackSubscriptions += manager.subscribe(LoggedOnCallback::class.java) { callback ->
            lastLogOnResult = callback.result
            loggedOn.set(callback.result == EResult.OK)
        }

        steamClient = client
        callbackManager = manager
        steamUser = user
    }

    private suspend fun waitUntil(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (condition()) {
                return true
            }
        }
        return false
    }

    private fun consoleAuthenticator(): IAuthenticator {
        return object : IAuthenticator {
            override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
                println("Approve Steam Guard on your mobile device, then press Enter to continue...")
                readLine()
                return CompletableFuture.completedFuture(true)
            }

            override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
                if (previousCodeWasIncorrect) {
                    println("Previous Steam Guard code was incorrect.")
                }
                print("Enter Steam Guard code: ")
                val code = readLine()?.trim().orEmpty()
                return CompletableFuture.completedFuture(code)
            }

            override fun getEmailCode(email: String?, previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
                if (previousCodeWasIncorrect) {
                    println("Previous email code was incorrect.")
                }
                if (!email.isNullOrBlank()) {
                    println("Enter code sent to: $email")
                }
                print("Enter email code: ")
                val code = readLine()?.trim().orEmpty()
                return CompletableFuture.completedFuture(code)
            }
        }
    }
}
