package app.gamenative.linux.runtime

class InMemoryProfileRepository(
    profiles: List<RuntimeProfile> = emptyList(),
) : ProfileRepository {
    private val store = linkedMapOf<String, RuntimeProfile>()

    init {
        profiles.forEach { store[it.id] = it }
    }

    override fun listProfiles(): List<RuntimeProfile> = store.values.toList()

    override fun getProfile(id: String): RuntimeProfile? = store[id]

    override fun saveProfile(profile: RuntimeProfile) {
        store[profile.id] = profile
    }
}
