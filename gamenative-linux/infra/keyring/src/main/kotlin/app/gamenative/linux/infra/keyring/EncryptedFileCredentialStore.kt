package app.gamenative.linux.infra.keyring

import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptedFileCredentialStore(
    private val storageFile: Path,
    private val passphraseProvider: () -> CharArray,
    private val random: SecureRandom = SecureRandom(),
) : CredentialStore {
    private val lock = Any()
    private val properties = Properties()

    init {
        synchronized(lock) {
            load()
            ensureSaltExists()
        }
    }

    override suspend fun write(secretId: String, secret: String) {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        synchronized(lock) {
            val encoded = encrypt(secret)
            properties.setProperty(secretId, encoded)
            persist()
        }
    }

    override suspend fun read(secretId: String): String? {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        synchronized(lock) {
            val encoded = properties.getProperty(secretId) ?: return null
            return decrypt(encoded)
        }
    }

    override suspend fun delete(secretId: String): Boolean {
        require(secretId.isNotBlank()) { "secretId cannot be blank" }

        synchronized(lock) {
            val previous = properties.remove(secretId)
            if (previous != null) {
                persist()
            }
            return previous != null
        }
    }

    private fun load() {
        if (!Files.exists(storageFile)) {
            return
        }
        Files.newInputStream(storageFile).use { properties.load(it) }
    }

    private fun persist() {
        val parent = storageFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
        Files.newOutputStream(storageFile).use { properties.store(it, "GameNative Linux credential store") }
    }

    private fun ensureSaltExists() {
        if (properties.getProperty(SALT_KEY) != null) {
            return
        }

        val salt = ByteArray(SALT_SIZE)
        random.nextBytes(salt)
        properties.setProperty(SALT_KEY, Base64.getEncoder().encodeToString(salt))
        persist()
    }

    private fun encrypt(plainText: String): String {
        val iv = ByteArray(IV_SIZE)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivEncoded = Base64.getEncoder().encodeToString(iv)
        val payloadEncoded = Base64.getEncoder().encodeToString(encrypted)
        return "$ivEncoded:$payloadEncoded"
    }

    private fun decrypt(payload: String): String? {
        val parts = payload.split(':', limit = 2)
        if (parts.size != 2) {
            return null
        }

        return try {
            val iv = Base64.getDecoder().decode(parts[0])
            val encrypted = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveKey(): SecretKey {
        val saltEncoded = properties.getProperty(SALT_KEY) ?: error("Missing credential store salt")
        val salt = Base64.getDecoder().decode(saltEncoded)
        val spec: KeySpec = PBEKeySpec(passphraseProvider(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, KEY_ALGORITHM)
    }

    companion object {
        private const val SALT_KEY = "__salt"
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
        private const val PBKDF2_ITERATIONS = 65_536
        private const val KEY_SIZE_BITS = 256

        private const val KEY_ALGORITHM = "AES"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
