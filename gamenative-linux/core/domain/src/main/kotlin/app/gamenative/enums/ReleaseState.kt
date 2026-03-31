package app.gamenative.enums

enum class ReleaseState(val code: Int) {
    disabled(0),
    released(1),
    prerelease(2),
    ;

    companion object {
        fun from(keyValue: String?): ReleaseState {
            return when (keyValue?.lowercase()) {
                disabled.name -> disabled
                released.name -> released
                prerelease.name -> prerelease
                else -> disabled
            }
        }

        fun from(code: Int): ReleaseState {
            ReleaseState.entries.forEach { appType ->
                if (code == appType.code) {
                    return appType
                }
            }
            return disabled
        }
    }
}
