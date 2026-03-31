package app.gamenative.linux.infra.keyring

import java.nio.file.Path

class LinuxCredentialStore(
    private val primary: CredentialStore,
    private val fallback: CredentialStore,
) : CredentialStore {
    override suspend fun write(secretId: String, secret: String) {
        runCatching { primary.write(secretId, secret) }
            .getOrElse { fallback.write(secretId, secret) }
    }

    override suspend fun read(secretId: String): String? {
        val primaryValue = runCatching { primary.read(secretId) }.getOrNull()
        if (primaryValue != null) {
            return primaryValue
        }
        return runCatching { fallback.read(secretId) }.getOrNull()
    }

    override suspend fun delete(secretId: String): Boolean {
        val deletedPrimary = runCatching { primary.delete(secretId) }.getOrDefault(false)
        val deletedFallback = runCatching { fallback.delete(secretId) }.getOrDefault(false)
        return deletedPrimary || deletedFallback
    }

    companion object {
        fun default(
            encryptedFile: Path,
            passphraseProvider: () -> CharArray,
            serviceName: String = "app.gamenative",
            userName: String = System.getProperty("user.name", "unknown"),
            commandRunner: CommandRunner = ProcessCommandRunner(),
        ): LinuxCredentialStore {
            val primary = SecretToolCredentialStore(
                serviceName = serviceName,
                userName = userName,
                commandRunner = commandRunner,
            )
            val fallback = EncryptedFileCredentialStore(
                storageFile = encryptedFile,
                passphraseProvider = passphraseProvider,
            )
            return LinuxCredentialStore(primary = primary, fallback = fallback)
        }
    }
}

interface CommandRunner {
    fun run(command: List<String>, stdinPayload: String? = null): CommandResult
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

class ProcessCommandRunner : CommandRunner {
    override fun run(command: List<String>, stdinPayload: String?): CommandResult {
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        if (stdinPayload != null) {
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(stdinPayload)
            }
        } else {
            process.outputStream.close()
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return CommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }
}

class SecretToolCredentialStore(
    private val serviceName: String,
    private val userName: String,
    private val commandRunner: CommandRunner = ProcessCommandRunner(),
) : CredentialStore {
    override suspend fun write(secretId: String, secret: String) {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        val result = commandRunner.run(
            command = listOf(
                "secret-tool",
                "store",
                "--label",
                "GameNative $secretId",
                "service",
                serviceName,
                "user",
                userName,
                "secret_id",
                secretId,
            ),
            stdinPayload = "$secret\n",
        )

        check(result.exitCode == 0) { "secret-tool store failed: ${result.stderr.trim()}" }
    }

    override suspend fun read(secretId: String): String? {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        val result = commandRunner.run(
            command = listOf(
                "secret-tool",
                "lookup",
                "service",
                serviceName,
                "user",
                userName,
                "secret_id",
                secretId,
            ),
        )

        if (result.exitCode != 0) {
            return null
        }

        val value = result.stdout.trimEnd()
        return value.ifEmpty { null }
    }

    override suspend fun delete(secretId: String): Boolean {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        val result = commandRunner.run(
            command = listOf(
                "secret-tool",
                "clear",
                "service",
                serviceName,
                "user",
                userName,
                "secret_id",
                secretId,
            ),
        )

        return result.exitCode == 0
    }
}
