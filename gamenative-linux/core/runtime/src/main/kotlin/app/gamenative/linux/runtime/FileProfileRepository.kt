package app.gamenative.linux.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class FileProfileRepository(
    private val profilesFile: Path,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : ProfileRepository {
    private val profiles = linkedMapOf<String, RuntimeProfile>()

    init {
        load()
    }

    override fun listProfiles(): List<RuntimeProfile> = profiles.values.toList()

    override fun getProfile(id: String): RuntimeProfile? = profiles[id]

    override fun saveProfile(profile: RuntimeProfile) {
        profiles[profile.id] = profile
        persist()
    }

    private fun load() {
        if (!Files.exists(profilesFile)) {
            return
        }

        val payload = try {
            Files.readString(profilesFile)
        } catch (_: Exception) {
            return
        }

        val parsed = runCatching {
            json.decodeFromString(PersistedProfiles.serializer(), payload)
        }.getOrNull() ?: return

        profiles.clear()
        parsed.profiles.forEach { dto ->
            profiles[dto.id] = dto.toRuntimeProfile()
        }
    }

    private fun persist() {
        val parent = profilesFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val dto = PersistedProfiles(
            profiles = profiles.values.map { RuntimeProfileDto.from(it) },
        )
        val payload = json.encodeToString(PersistedProfiles.serializer(), dto)
        Files.writeString(profilesFile, payload)
    }
}

@Serializable
private data class PersistedProfiles(
    val profiles: List<RuntimeProfileDto> = emptyList(),
)

@Serializable
private data class RuntimeProfileDto(
    val id: String,
    val name: String,
    val wineBinary: String,
    val backend: String,
    val env: Map<String, String> = emptyMap(),
    val supervisionPolicy: RuntimeSupervisionPolicyDto = RuntimeSupervisionPolicyDto(),
) {
    fun toRuntimeProfile(): RuntimeProfile {
        val backendValue = runCatching { RuntimeBackend.valueOf(backend) }.getOrDefault(RuntimeBackend.BOX64)
        return RuntimeProfile(
            id = id,
            name = name,
            wineBinary = wineBinary,
            backend = backendValue,
            env = env,
            supervisionPolicy = supervisionPolicy.toModel(),
        )
    }

    companion object {
        fun from(profile: RuntimeProfile): RuntimeProfileDto {
            return RuntimeProfileDto(
                id = profile.id,
                name = profile.name,
                wineBinary = profile.wineBinary,
                backend = profile.backend.name,
                env = profile.env,
                supervisionPolicy = RuntimeSupervisionPolicyDto.from(profile.supervisionPolicy),
            )
        }
    }
}

@Serializable
private data class RuntimeSupervisionPolicyDto(
    val lookbackMinutes: Int = 15,
    val manualInterventionThreshold: Int = 3,
    val retryBackoffSecondsPerFailure: Int = 10,
    val recoveryRetryBackoffSecondsPerFailure: Int = 15,
    val incidentWarningRetryThreshold: Int = 2,
    val incidentCriticalRetryThreshold: Int = 4,
) {
    fun toModel(): RuntimeSupervisionPolicy {
        return RuntimeSupervisionPolicy(
            lookbackMinutes = lookbackMinutes,
            manualInterventionThreshold = manualInterventionThreshold,
            retryBackoffSecondsPerFailure = retryBackoffSecondsPerFailure,
            recoveryRetryBackoffSecondsPerFailure = recoveryRetryBackoffSecondsPerFailure,
            incidentWarningRetryThreshold = incidentWarningRetryThreshold,
            incidentCriticalRetryThreshold = incidentCriticalRetryThreshold,
        )
    }

    companion object {
        fun from(policy: RuntimeSupervisionPolicy): RuntimeSupervisionPolicyDto {
            return RuntimeSupervisionPolicyDto(
                lookbackMinutes = policy.lookbackMinutes,
                manualInterventionThreshold = policy.manualInterventionThreshold,
                retryBackoffSecondsPerFailure = policy.retryBackoffSecondsPerFailure,
                recoveryRetryBackoffSecondsPerFailure = policy.recoveryRetryBackoffSecondsPerFailure,
                incidentWarningRetryThreshold = policy.incidentWarningRetryThreshold,
                incidentCriticalRetryThreshold = policy.incidentCriticalRetryThreshold,
            )
        }
    }
}
