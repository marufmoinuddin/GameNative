package app.gamenative.linux.infra.persistence

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilePersistenceServiceTest {
    @Test
    fun initializeCreatesStructureAndHealthCheckPasses() = runBlocking {
        val root = Files.createTempDirectory("gamenative-persistence-test")
        val service = FilePersistenceService(root)

        service.initialize()

        assertTrue(service.healthCheck())
        assertTrue(Files.isDirectory(root.resolve("data")))
        assertTrue(Files.isDirectory(root.resolve("backup")))
    }

    @Test
    fun backupCopiesDataContents() = runBlocking {
        val root = Files.createTempDirectory("gamenative-persistence-test")
        val service = FilePersistenceService(root)

        service.initialize()
        val dataFile = root.resolve("data").resolve("state.txt")
        Files.writeString(dataFile, "hello")

        val backupPath = service.backup("daily")
        val backupFile = java.nio.file.Path.of(backupPath).resolve("state.txt")

        assertTrue(Files.exists(backupFile))
        assertEquals("hello", Files.readString(backupFile))
    }
}
