package app.gamenative.linux.infra.persistence

interface PersistenceService {
    suspend fun initialize()
    suspend fun healthCheck(): Boolean
    suspend fun backup(tag: String): String
}
