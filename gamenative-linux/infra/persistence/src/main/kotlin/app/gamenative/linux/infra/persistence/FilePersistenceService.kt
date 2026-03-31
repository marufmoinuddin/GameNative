package app.gamenative.linux.infra.persistence

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter

class FilePersistenceService(
    private val rootDir: Path,
) : PersistenceService {
    private val dataDir: Path = rootDir.resolve("data")
    private val backupDir: Path = rootDir.resolve("backup")
    private val healthFile: Path = rootDir.resolve(".health")

    override suspend fun initialize() {
        Files.createDirectories(dataDir)
        Files.createDirectories(backupDir)

        if (!Files.exists(healthFile)) {
            Files.writeString(healthFile, "ok")
        }
    }

    override suspend fun healthCheck(): Boolean {
        return try {
            Files.exists(dataDir) &&
                Files.isDirectory(dataDir) &&
                Files.exists(backupDir) &&
                Files.isDirectory(backupDir) &&
                Files.exists(healthFile) &&
                Files.isReadable(healthFile)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun backup(tag: String): String {
        require(tag.isNotBlank()) { "backup tag cannot be blank" }

        val sanitizedTag = tag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-")
        val target = backupDir.resolve("data-${sanitizedTag}-${timestamp}")

        Files.createDirectories(target)
        copyRecursively(dataDir, target)

        return target.toString()
    }

    private fun copyRecursively(sourceDir: Path, targetDir: Path) {
        if (!Files.exists(sourceDir)) {
            return
        }

        Files.walk(sourceDir).forEach { source ->
            val relative = sourceDir.relativize(source)
            val target = targetDir.resolve(relative)

            if (Files.isDirectory(source)) {
                Files.createDirectories(target)
            } else {
                val parent = target.parent
                if (parent != null) {
                    Files.createDirectories(parent)
                }
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
