package app.gamenative.linux.infra.keyring

interface CredentialStore {
    suspend fun write(secretId: String, secret: String)
    suspend fun read(secretId: String): String?
    suspend fun delete(secretId: String): Boolean
}
