package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuntimeCrashBundleArchiver(
    private val stateDir: Path,
    private val maxBundles: Int = 20,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
    },
) {
    fun archive(snapshot: RuntimeDiagnosticsSnapshot): Path? {
        val bundle = snapshot.crashBundle ?: return null
        if (!bundle.abnormalExit) {
            return null
        }

        val crashesDir = stateDir.resolve("crashes")
        Files.createDirectories(crashesDir)

        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-")
        val sessionSafe = bundle.sessionId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val crashFile = crashesDir.resolve("crash-${sessionSafe}-${timestamp}.json")

        Files.writeString(crashFile, json.encodeToString(bundle))
        prune(crashesDir)
        return crashFile
    }

    private fun prune(crashesDir: Path) {
        if (maxBundles < 1) {
            return
        }

        val files = Files.list(crashesDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .sorted { a, b -> Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)) }
                .collect(Collectors.toList())
        }

        files.drop(maxBundles).forEach { stale ->
            Files.deleteIfExists(stale)
        }
    }
}
