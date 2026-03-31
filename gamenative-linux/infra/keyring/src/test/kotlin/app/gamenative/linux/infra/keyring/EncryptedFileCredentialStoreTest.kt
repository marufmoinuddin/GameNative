package app.gamenative.linux.infra.keyring

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptedFileCredentialStoreTest {
    @Test
    fun writeReadDeleteCycleWorks() = runBlocking {
        val tempFile = Files.createTempDirectory("gamenative-keyring-test").resolve("credentials.properties")
        val store = EncryptedFileCredentialStore(
            storageFile = tempFile,
            passphraseProvider = { "test-passphrase".toCharArray() },
        )

        store.write("steam.session", "abc123")

        assertEquals("abc123", store.read("steam.session"))
        assertTrue(store.delete("steam.session"))
        assertNull(store.read("steam.session"))
    }

    @Test
    fun wrongPassphraseCannotDecrypt() = runBlocking {
        val tempFile = Files.createTempDirectory("gamenative-keyring-test").resolve("credentials.properties")

        val writer = EncryptedFileCredentialStore(
            storageFile = tempFile,
            passphraseProvider = { "correct-passphrase".toCharArray() },
        )
        writer.write("steam.session", "top-secret")

        val reader = EncryptedFileCredentialStore(
            storageFile = tempFile,
            passphraseProvider = { "wrong-passphrase".toCharArray() },
        )
        assertNull(reader.read("steam.session"))
    }
}
