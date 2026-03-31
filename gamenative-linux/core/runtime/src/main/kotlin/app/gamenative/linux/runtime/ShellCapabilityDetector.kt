package app.gamenative.linux.runtime

class ShellCapabilityDetector(
    private val commandExists: (String) -> Boolean = ::defaultCommandExists,
) : CapabilityDetector {
    override fun detect(): CapabilityReport {
        val wine = commandExists("wine") || commandExists("wine64")
        val box64 = commandExists("box64")
        val fex = commandExists("fex-emu") || commandExists("FEXInterpreter")
        val vulkan = commandExists("vulkaninfo")

        val diagnostics = buildList {
            if (!wine) add("wine not found in PATH")
            if (!box64) add("box64 not found in PATH")
            if (!vulkan) add("vulkaninfo not found in PATH")
        }

        return CapabilityReport(
            wineAvailable = wine,
            box64Available = box64,
            fexAvailable = fex,
            vulkanAvailable = vulkan,
            diagnostics = diagnostics,
        )
    }

    companion object {
        private fun defaultCommandExists(command: String): Boolean {
            val process = ProcessBuilder("sh", "-c", "command -v $command >/dev/null 2>&1")
                .start()
            return process.waitFor() == 0
        }
    }
}
