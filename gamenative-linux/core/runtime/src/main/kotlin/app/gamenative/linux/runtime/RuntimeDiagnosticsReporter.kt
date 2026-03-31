package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuntimeDiagnosticsReporter(
    private val diagnosticsService: RuntimeDiagnosticsService,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
    },
) {
    fun snapshotJson(): String {
        val snapshot = diagnosticsService.snapshot()
        return json.encodeToString(snapshot)
    }

    fun writeSnapshot(outputPath: Path): Path {
        val parent = outputPath.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
        Files.writeString(outputPath, snapshotJson())
        return outputPath
    }
}
