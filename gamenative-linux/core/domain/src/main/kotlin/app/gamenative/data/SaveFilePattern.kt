package app.gamenative.data

import app.gamenative.enums.PathType
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SaveFilePattern(
    val root: PathType,
    val path: String,
    val pattern: String,
    val recursive: Int = 0,
    val uploadRoot: PathType = root,
    val uploadPath: String = path,
) {
    private fun replaceSteamPlaceholders(value: String): String {
        val steamId64 = System.getProperty("gamenative.steamId64", "0")
        val steam3AccountId = System.getProperty("gamenative.steam3AccountId", "0")
        return value
            .replace("{64BitSteamID}", steamId64)
            .replace("{Steam3AccountID}", steam3AccountId)
    }

    val prefix: String
        get() = replaceSteamPlaceholders("%${root.name}%$path")

    val substitutedPath: String
        get() = path
            .let(::replaceSteamPlaceholders)
            .replace("\\", File.separator)
}
