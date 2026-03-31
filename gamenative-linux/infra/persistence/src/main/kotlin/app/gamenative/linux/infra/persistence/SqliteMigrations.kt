package app.gamenative.linux.infra.persistence

import java.sql.Connection
import java.sql.DriverManager
import java.nio.file.Path

data class SqliteMigration(
    val version: Int,
    val statements: List<String>,
)

object SqliteMigrationPlan {
    val migrations: List<SqliteMigration> = listOf(
        SqliteMigration(
            version = 1,
            statements = listOf(
                """
                CREATE TABLE IF NOT EXISTS steam_app (
                    id INTEGER PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    package_id INTEGER NOT NULL DEFAULT 0,
                    release_state TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS steam_license (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    package_id INTEGER NOT NULL,
                    owner_account_id INTEGER NOT NULL DEFAULT 0,
                    flags INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            ),
        ),
        SqliteMigration(
            version = 2,
            statements = listOf(
                "ALTER TABLE steam_app ADD COLUMN owner_account_id TEXT NOT NULL DEFAULT ''",
                "CREATE INDEX IF NOT EXISTS idx_steam_app_name ON steam_app(name)",
            ),
        ),
    )
}

class SqliteSchemaMigrator(
    private val migrationPlan: List<SqliteMigration> = SqliteMigrationPlan.migrations,
) {
    fun migrate(databasePath: Path): Int {
        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            connection.autoCommit = false
            ensureMetaTable(connection)

            val currentVersion = readVersion(connection)
            val pending = migrationPlan.filter { it.version > currentVersion }.sortedBy { it.version }

            pending.forEach { migration ->
                migration.statements.forEach { sql ->
                    connection.createStatement().use { statement ->
                        statement.execute(sql)
                    }
                }
                writeVersion(connection, migration.version)
            }

            connection.commit()
            return readVersion(connection)
        }
    }

    private fun ensureMetaTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS schema_meta (
                    key TEXT PRIMARY KEY NOT NULL,
                    value TEXT NOT NULL
                )
                """.trimIndent(),
            )
            statement.execute("INSERT OR IGNORE INTO schema_meta(key, value) VALUES('schema_version', '0')")
        }
    }

    private fun readVersion(connection: Connection): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT value FROM schema_meta WHERE key = 'schema_version' LIMIT 1").use { rs ->
                return if (rs.next()) rs.getString(1).toIntOrNull() ?: 0 else 0
            }
        }
    }

    private fun writeVersion(connection: Connection, version: Int) {
        connection.prepareStatement("UPDATE schema_meta SET value = ? WHERE key = 'schema_version'").use { ps ->
            ps.setString(1, version.toString())
            ps.executeUpdate()
        }
    }
}

class DbMigrationValidator(
    private val migrator: SqliteSchemaMigrator = SqliteSchemaMigrator(),
) {
    fun validateFreshDatabase(databasePath: Path): Int {
        return migrator.migrate(databasePath)
    }
}
