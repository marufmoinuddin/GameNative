package app.gamenative.linux.runtime

class DefaultEnvironmentComposer : EnvironmentComposer {
    override fun compose(profile: RuntimeProfile, overrides: Map<String, String>): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env.putAll(profile.env)

        when (profile.backend) {
            RuntimeBackend.BOX64 -> {
                env.putIfAbsent("BOX64_LOG", "0")
                env.putIfAbsent("BOX64_DYNAREC", "1")
                env.putIfAbsent("BOX64_PREFER_EMULATED_FLAGS", "1")
            }
            RuntimeBackend.FEX -> {
                env.putIfAbsent("FEX_ENABLE_VIXL", "1")
            }
        }

        env.putAll(overrides)
        return env
    }
}
