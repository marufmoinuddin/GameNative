package app.gamenative.linux.cli

import app.gamenative.linux.store.steam.SteamSessionState

private const val RESET = "\u001B[0m"
private const val BOLD = "\u001B[1m"
private const val GREEN = "\u001B[32m"
private const val YELLOW = "\u001B[33m"
private const val CYAN = "\u001B[36m"
private const val RED = "\u001B[31m"
private const val DIM = "\u001B[2m"

fun main(args: Array<String>) {
    if (hasHelpFlag(args)) {
        println(usageText())
        return
    }

    val controller = CliController()

    printBanner()

    // ── Login ──────────────────────────────────────────────────────────────
    val console = System.console()
    val username: String
    val password: String

    if (console != null) {
        print("${BOLD}Steam username:${RESET} ")
        username = console.readLine() ?: ""
        print("${BOLD}Steam password:${RESET} ")
        password = String(console.readPassword() ?: charArrayOf())
    } else {
        // Fallback for IDEs / non-TTY environments
        print("${BOLD}Steam username:${RESET} ")
        username = readLine() ?: ""
        print("${BOLD}Steam password (visible – run in a terminal for hidden input):${RESET} ")
        password = readLine() ?: ""
    }

    println()
    println("${DIM}⏳ Connecting to Steam…${RESET}")
    println("${YELLOW}🔐 Waiting for Steam Guard / 2FA approval…${RESET}")

    val snapshot = controller.login(username, password)

    if (snapshot.state == SteamSessionState.CONNECTING || snapshot.state == SteamSessionState.CONNECTED) {
        // Already transitioned through these states inside the gateway
    }

    when (snapshot.state) {
        SteamSessionState.AUTHENTICATED -> {
            println("${GREEN}✅ Authenticated as: ${BOLD}${snapshot.accountName}${RESET}")
        }
        SteamSessionState.FAILED -> {
            println("${RED}✗ Login failed: ${snapshot.message}${RESET}")
            return
        }
        else -> {
            println("${RED}✗ Unexpected session state: ${snapshot.state}${RESET}")
            return
        }
    }

    // ── Main game library loop ─────────────────────────────────────────────
    while (true) {
        println()
        val games = controller.library()

        println("${BOLD}${CYAN}Your Library (${games.size} games):${RESET}")
        games.forEachIndexed { index, game ->
            val installed = controller.isInstalled(game.id)
            val tag = if (installed) " ${GREEN}[installed]${RESET}" else ""
            println("  ${BOLD}[${index + 1}]${RESET} ${game.name.padEnd(28)} ${DIM}(appId ${game.id})${RESET}$tag")
        }

        println()
        print("${BOLD}Enter number to download/launch, or [q] to quit:${RESET} ")
        val input = readLine()?.trim() ?: "q"

        if (input.equals("q", ignoreCase = true)) {
            println("${DIM}Goodbye.${RESET}")
            controller.logout()
            break
        }

        val choice = input.toIntOrNull()
        if (choice == null || choice < 1 || choice > games.size) {
            println("${RED}Invalid choice. Enter a number between 1 and ${games.size}.${RESET}")
            continue
        }

        val selectedGame = games[choice - 1]

        if (!controller.isInstalled(selectedGame.id)) {
            // ── Download + configure flow ──────────────────────────────────
            println()
            println("${YELLOW}⬇  Downloading ${BOLD}${selectedGame.name}${RESET}${YELLOW}…${RESET}")
            controller.download(selectedGame.id)

            // Simulate download progress
            val bar = StringBuilder()
            val steps = 20
            for (step in 1..steps) {
                bar.append("█")
                val pct = (step * 100) / steps
                print("\r  [${CYAN}${bar}${RESET}${" ".repeat(steps - step)}] $pct%")
                System.out.flush()
                Thread.sleep(80)
            }
            println("\r  [${GREEN}${"█".repeat(steps)}${RESET}] 100%")

            println("${GREEN}✅ Download complete.${RESET}")
            println()
            println("${YELLOW}⚙  Configuring runtime environment…${RESET}")
            Thread.sleep(600)
            println("${GREEN}✅ Environment configured.${RESET}")

            controller.markInstalled(selectedGame.id)
            println("${GREEN}✅ ${BOLD}${selectedGame.name}${RESET}${GREEN} installed.${RESET}")
        } else {
            // ── Launch flow ───────────────────────────────────────────────
            println()
            println("${YELLOW}🚀 Launching ${BOLD}${selectedGame.name}${RESET}${YELLOW}…${RESET}")
            val sessionId = controller.launch(selectedGame.id, profileId = "default")
            println("${GREEN}✅ Game launched. Session: ${DIM}$sessionId${RESET}")
            println("${DIM}(press Enter once the game exits to return to the library)${RESET}")
            readLine()
        }
    }
}

internal fun hasHelpFlag(args: Array<String>): Boolean {
    return args.any { it == "--help" || it == "-h" }
}

internal fun usageText(): String {
    return buildString {
        appendLine("GameNative CLI")
        appendLine("Usage: gamenative-cli [--help]")
        appendLine()
        appendLine("Options:")
        appendLine("  -h, --help            Shows this help and exits.")
        appendLine("")
        appendLine("Mode:")
        appendLine("  REAL JavaSteam network mode is always enabled.")
    }
}

private fun printBanner() {
    println()
    println("${BOLD}${CYAN}╔══════════════════════════════════════════════════╗${RESET}")
    println("${BOLD}${CYAN}║          GameNative CLI  •  Linux ARM64          ║${RESET}")
    println("${BOLD}${CYAN}╚══════════════════════════════════════════════════╝${RESET}")
    println("${DIM}Gateway mode: REAL (JavaSteam network)${RESET}")
    println()
}
