package app.gamenative.enums

enum class PathType {
    GameInstall,
    SteamUserData,
    WinMyDocuments,
    WinAppDataLocal,
    WinAppDataLocalLow,
    WinAppDataRoaming,
    WinSavedGames,
    LinuxHome,
    LinuxXdgDataHome,
    LinuxXdgConfigHome,
    MacHome,
    MacAppSupport,
    None,
    Root,
    ;

    val isWindows: Boolean
        get() = when (this) {
            GameInstall,
            SteamUserData,
            WinMyDocuments,
            WinAppDataLocal,
            WinAppDataLocalLow,
            WinAppDataRoaming,
            WinSavedGames,
            -> true
            else -> false
        }

    companion object {
        val DEFAULT = SteamUserData

        /**
         * Resolve GOG path placeholders (<?VARIABLE?>) into Windows environment variables.
         */
        fun resolveGOGPathVariables(location: String, installPath: String): String {
            var resolved = location
            val variableMap = mapOf(
                "INSTALL" to installPath,
                "SAVED_GAMES" to "%USERPROFILE%/Saved Games",
                "APPLICATION_DATA_LOCAL" to "%LOCALAPPDATA%",
                "APPLICATION_DATA_LOCAL_LOW" to "%APPDATA%\\..\\LocalLow",
                "APPLICATION_DATA_ROAMING" to "%APPDATA%",
                "DOCUMENTS" to "%USERPROFILE%\\Documents",
            )

            val pattern = Regex("<\\?(\\w+)\\?>")
            val matches = pattern.findAll(resolved)
            for (match in matches) {
                val variableName = match.groupValues[1]
                val replacement = variableMap[variableName] ?: continue
                resolved = resolved.replace(match.value, replacement)
            }

            return resolved
        }

        fun from(keyValue: String?): PathType {
            return when (keyValue?.lowercase()) {
                "%${GameInstall.name.lowercase()}%", GameInstall.name.lowercase() -> GameInstall
                "%${SteamUserData.name.lowercase()}%", SteamUserData.name.lowercase() -> SteamUserData
                "%${WinMyDocuments.name.lowercase()}%", WinMyDocuments.name.lowercase() -> WinMyDocuments
                "%${WinAppDataLocal.name.lowercase()}%", WinAppDataLocal.name.lowercase() -> WinAppDataLocal
                "%${WinAppDataLocalLow.name.lowercase()}%", WinAppDataLocalLow.name.lowercase() -> WinAppDataLocalLow
                "%${WinAppDataRoaming.name.lowercase()}%", WinAppDataRoaming.name.lowercase() -> WinAppDataRoaming
                "%${WinSavedGames.name.lowercase()}%", WinSavedGames.name.lowercase() -> WinSavedGames
                "%${LinuxHome.name.lowercase()}%", LinuxHome.name.lowercase() -> LinuxHome
                "%${LinuxXdgDataHome.name.lowercase()}%", LinuxXdgDataHome.name.lowercase() -> LinuxXdgDataHome
                "%${LinuxXdgConfigHome.name.lowercase()}%", LinuxXdgConfigHome.name.lowercase() -> LinuxXdgConfigHome
                "%${MacHome.name.lowercase()}%", MacHome.name.lowercase() -> MacHome
                "%${MacAppSupport.name.lowercase()}%", MacAppSupport.name.lowercase() -> MacAppSupport
                "%ROOT_MOD%", "root_mod" -> Root
                else -> None
            }
        }
    }
}
