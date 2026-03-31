package app.gamenative.linux.infra.persistence

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DbMigrationValidatorTest {
    @Test
    fun validatesFreshDatabaseAndAppliesLatestVersion() {
        val dbFile = Files.createTempFile("gamenative-db", ".sqlite")
        val validator = DbMigrationValidator()

        val version = validator.validateFreshDatabase(dbFile)

        assertEquals(2, version)
        DriverManager.getConnection("jdbc:sqlite:${dbFile.toAbsolutePath()}").use { conn ->
            conn.createStatement().use { statement ->
                val rs = statement.executeQuery("SELECT value FROM schema_meta WHERE key = 'schema_version'")
                assertTrue(rs.next())
                assertEquals("2", rs.getString(1))
            }
        }
    }

    @Test
    fun migrationIsIdempotentOnSecondRun() {
        val dbFile = Files.createTempFile("gamenative-db", ".sqlite")
        val migrator = SqliteSchemaMigrator()

        val first = migrator.migrate(dbFile)
        val second = migrator.migrate(dbFile)

        assertEquals(2, first)
        assertEquals(2, second)
    }
}
