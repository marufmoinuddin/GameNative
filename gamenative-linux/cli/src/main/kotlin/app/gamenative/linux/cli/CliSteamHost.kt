package app.gamenative.linux.cli

import java.nio.file.Files
import java.nio.file.Path

interface CliSteamHost {
    fun isInstalled(appId: Int): Boolean
    fun requestInstall(appId: Int): CliActionResult
    fun launchApp(appId: Int): CliActionResult
}

class DefaultCliSteamHost : CliSteamHost {
    override fun isInstalled(appId: Int): Boolean {
        val manifestName = "appmanifest_${appId}.acf"
        return steamLibraryRoots().any { root ->
            Files.exists(root.resolve("steamapps").resolve(manifestName))
        }
    }

    override fun requestInstall(appId: Int): CliActionResult {
        val installUri = "steam://install/$appId"
        val attempts = listOf(
            listOf("steam", installUri),
            listOf("xdg-open", installUri),
            listOf("flatpak", "run", "com.valvesoftware.Steam", installUri),
        )
        return tryCommands(attempts, successMessage = "Install request sent to Steam for appId $appId")
    }

    override fun launchApp(appId: Int): CliActionResult {
        if (!isInstalled(appId)) {
            return CliActionResult(
                success = false,
                message = "Game is not installed yet. Start install first and wait for Steam to finish.",
            )
        }

        val runUri = "steam://run/$appId"
        val attempts = listOf(
            listOf("steam", "-applaunch", appId.toString()),
            listOf("steam", runUri),
            listOf("xdg-open", runUri),
            listOf("flatpak", "run", "com.valvesoftware.Steam", "-applaunch", appId.toString()),
        )
        return tryCommands(attempts, successMessage = "Launch request sent to Steam for appId $appId")
    }

    private fun tryCommands(attempts: List<List<String>>, successMessage: String): CliActionResult {
        val errors = mutableListOf<String>()
        for (command in attempts) {
            val result = runCommand(command)
            if (result.success) {
                return CliActionResult(success = true, message = "$successMessage via `${command.joinToString(" ")}`")
            }
            errors += "${command.first()}: ${result.message}"
        }

        return CliActionResult(
            success = false,
            message = "Failed to reach Steam client (${errors.joinToString("; ")})",
        )
    }

    private fun runCommand(command: List<String>): CliActionResult {
        return runCatching {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val exitedQuickly = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            if (exitedQuickly && process.exitValue() != 0) {
                val output = process.inputStream.bufferedReader().readText().trim()
                CliActionResult(
                    success = false,
                    message = output.ifBlank { "exit ${process.exitValue()}" },
                )
            } else {
                CliActionResult(success = true, message = "ok")
            }
        }.getOrElse { error ->
            CliActionResult(success = false, message = error.message ?: "command failed")
        }
    }

    private fun steamLibraryRoots(): Set<Path> {
        val home = Path.of(System.getProperty("user.home"))
        val roots = linkedSetOf(
            home.resolve(".local/share/Steam"),
            home.resolve(".steam/steam"),
            home.resolve(".var/app/com.valvesoftware.Steam/.local/share/Steam"),
        )

        val discovered = roots
            .filter { Files.exists(it.resolve("steamapps/libraryfolders.vdf")) }
            .flatMap { readLibraryFolders(it.resolve("steamapps/libraryfolders.vdf")) }

        roots += discovered
        return roots.filter { Files.exists(it.resolve("steamapps")) }.toSet()
    }

    private fun readLibraryFolders(file: Path): List<Path> {
        val text = runCatching { Files.readString(file) }.getOrNull() ?: return emptyList()
        val pathRegex = Regex("\"path\"\\s*\"([^\"]+)\"")
        return pathRegex.findAll(text)
            .map { match ->
                val raw = match.groupValues[1]
                Path.of(raw.replace("\\\\", "\\"))
            }
            .filter { path -> Files.exists(path.resolve("steamapps")) }
            .toList()
    }
}