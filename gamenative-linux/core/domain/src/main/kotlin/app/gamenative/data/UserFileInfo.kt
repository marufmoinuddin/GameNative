package app.gamenative.data

import app.gamenative.enums.PathType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.text.replace

/**
 * @param timestamp the value in milliseconds, since the epoch (1970-01-01T00:00:00Z)
 */
@Serializable
data class UserFileInfo(
    val root: PathType,
    val path: String,
    val filename: String,
    val timestamp: Long,
    val sha: ByteArray,
    val cloudRoot: PathType = root,
    val cloudPath: String = path,
) {
    private fun replaceSteamPlaceholders(value: String): String {
        val steamId64 = System.getProperty("gamenative.steamId64", "0")
        val steam3AccountId = System.getProperty("gamenative.steam3AccountId", "0")
        return value
            .replace("{64BitSteamID}", steamId64)
            .replace("{Steam3AccountID}", steam3AccountId)
    }

    // "." and blank path both mean "root of path type" per Steam manifest.
    val prefix: String
        get() {
            val pathForPrefix = when {
                cloudPath.isBlank() || cloudPath == "." -> ""
                else -> cloudPath
            }
            return replaceSteamPlaceholders(Paths.get("%${cloudRoot.name}%$pathForPrefix").pathString)
        }

    // Bare placeholder (%GameInstall%) expects no slash before filename; path with folder uses Paths.get.
    val prefixPath: String
        get() = when {
            cloudPath.isBlank() || cloudPath == "." -> "$prefix$filename"
            else -> Paths.get(prefix, filename).pathString
        }.let(::replaceSteamPlaceholders)

    val substitutedPath: String
        get() = path
            .let(::replaceSteamPlaceholders)
            .replace("\\", File.separator)

    fun getAbsPath(prefixToPath: (String) -> String): Path {
        return Paths.get(prefixToPath(root.toString()), substitutedPath, filename)
    }
}
