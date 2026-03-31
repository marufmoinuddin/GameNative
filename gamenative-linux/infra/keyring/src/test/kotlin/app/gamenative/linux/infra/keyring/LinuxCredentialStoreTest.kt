package app.gamenative.linux.infra.keyring

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinuxCredentialStoreTest {
    @Test
    fun fallsBackWhenPrimaryWriteFails() = runBlocking {
        val primary = InMemoryCredentialStore(failWrites = true)
        val fallback = InMemoryCredentialStore()
        val store = LinuxCredentialStore(primary = primary, fallback = fallback)

        store.write("steam.session", "abc")

        assertEquals("abc", fallback.read("steam.session"))
        assertEquals(null, primary.read("steam.session"))
    }

    @Test
    fun readsFromFallbackWhenPrimaryMissing() = runBlocking {
        val primary = InMemoryCredentialStore()
        val fallback = InMemoryCredentialStore().also { it.write("steam.session", "backup") }
        val store = LinuxCredentialStore(primary = primary, fallback = fallback)

        val value = store.read("steam.session")

        assertEquals("backup", value)
    }

    @Test
    fun deleteCleansBothStores() = runBlocking {
        val primary = InMemoryCredentialStore().also { it.write("steam.session", "primary") }
        val fallback = InMemoryCredentialStore().also { it.write("steam.session", "fallback") }
        val store = LinuxCredentialStore(primary = primary, fallback = fallback)

        val deleted = store.delete("steam.session")

        assertTrue(deleted)
        assertEquals(null, primary.read("steam.session"))
        assertEquals(null, fallback.read("steam.session"))
    }

    @Test
    fun secretToolStoreReadDeleteFlowUsesExpectedCommands() = runBlocking {
        val runner = FakeCommandRunner()
        val store = SecretToolCredentialStore(
            serviceName = "app.gamenative",
            userName = "tester",
            commandRunner = runner,
        )

        store.write("steam.session", "abc123")
        val read = store.read("steam.session")
        val deleted = store.delete("steam.session")

        assertEquals("abc123", read)
        assertTrue(deleted)
        assertEquals(3, runner.calls.size)
        assertTrue(runner.calls[0].first() == "secret-tool")
        assertTrue(runner.calls[0].contains("store"))
        assertTrue(runner.calls[1].contains("lookup"))
        assertTrue(runner.calls[2].contains("clear"))
    }
}

private class InMemoryCredentialStore(
    private val failWrites: Boolean = false,
) : CredentialStore {
    private val values = mutableMapOf<String, String>()

    override suspend fun write(secretId: String, secret: String) {
        if (failWrites) {
            error("write failed")
        }
        values[secretId] = secret
    }

    override suspend fun read(secretId: String): String? = values[secretId]

    override suspend fun delete(secretId: String): Boolean = values.remove(secretId) != null
}

private class FakeCommandRunner : CommandRunner {
    val calls = mutableListOf<List<String>>()
    private var storedSecret: String? = null

    override fun run(command: List<String>, stdinPayload: String?): CommandResult {
        calls += command
        return when {
            command.contains("store") -> {
                storedSecret = stdinPayload?.trimEnd('\n')
                CommandResult(exitCode = 0, stdout = "", stderr = "")
            }

            command.contains("lookup") -> {
                CommandResult(exitCode = 0, stdout = storedSecret.orEmpty(), stderr = "")
            }

            command.contains("clear") -> {
                val hadValue = storedSecret != null
                storedSecret = null
                CommandResult(exitCode = if (hadValue) 0 else 1, stdout = "", stderr = "")
            }

            else -> CommandResult(exitCode = 1, stdout = "", stderr = "unsupported")
        }
    }
}
