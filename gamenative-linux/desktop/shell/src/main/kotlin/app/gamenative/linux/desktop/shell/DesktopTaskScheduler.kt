package app.gamenative.linux.desktop.shell

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties

enum class DesktopTaskStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELED,
    FAILED,
}

data class DesktopTaskEntry(
    val id: String,
    val type: String,
    val payload: String,
    val status: DesktopTaskStatus,
    val updatedAt: String,
)

class DesktopTaskScheduler(
    private val storeFile: Path,
) {
    private val tasks = linkedMapOf<String, DesktopTaskEntry>()

    init {
        load()
    }

    fun enqueue(type: String, payload: String): DesktopTaskEntry {
        val id = "task-${Instant.now().toEpochMilli()}-${tasks.size + 1}"
        val entry = DesktopTaskEntry(
            id = id,
            type = type,
            payload = payload,
            status = DesktopTaskStatus.QUEUED,
            updatedAt = Instant.now().toString(),
        )
        tasks[id] = entry
        persist()
        return entry
    }

    fun updateStatus(taskId: String, status: DesktopTaskStatus) {
        val existing = tasks[taskId] ?: return
        tasks[taskId] = existing.copy(
            status = status,
            updatedAt = Instant.now().toString(),
        )
        persist()
    }

    fun markLatestByPayload(type: String, payload: String, status: DesktopTaskStatus) {
        val latest = tasks.values
            .filter { it.type == type && it.payload == payload }
            .maxByOrNull { it.updatedAt }
            ?: return
        updateStatus(latest.id, status)
    }

    fun listTasks(limit: Int = 50): List<DesktopTaskEntry> {
        return tasks.values
            .sortedByDescending { it.updatedAt }
            .take(limit)
    }

    fun resumePendingTasks(): Int {
        var resumed = 0
        tasks.values.forEach { task ->
            if (task.status == DesktopTaskStatus.RUNNING) {
                tasks[task.id] = task.copy(
                    status = DesktopTaskStatus.QUEUED,
                    updatedAt = Instant.now().toString(),
                )
                resumed += 1
            }
        }
        if (resumed > 0) {
            persist()
        }
        return resumed
    }

    private fun load() {
        if (!Files.exists(storeFile)) {
            return
        }

        val props = Properties()
        Files.newInputStream(storeFile).use { props.load(it) }

        val count = props.getProperty("count")?.toIntOrNull() ?: 0
        repeat(count) { index ->
            val prefix = "task.$index"
            val id = props.getProperty("$prefix.id") ?: return@repeat
            val type = props.getProperty("$prefix.type") ?: "unknown"
            val payload = props.getProperty("$prefix.payload") ?: ""
            val status = runCatching {
                DesktopTaskStatus.valueOf(props.getProperty("$prefix.status") ?: "QUEUED")
            }.getOrDefault(DesktopTaskStatus.QUEUED)
            val updatedAt = props.getProperty("$prefix.updatedAt") ?: Instant.EPOCH.toString()
            tasks[id] = DesktopTaskEntry(
                id = id,
                type = type,
                payload = payload,
                status = status,
                updatedAt = updatedAt,
            )
        }
    }

    private fun persist() {
        val parent = storeFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        val props = Properties()
        val entries = tasks.values.sortedBy { it.updatedAt }
        props.setProperty("count", entries.size.toString())
        entries.forEachIndexed { index, task ->
            val prefix = "task.$index"
            props.setProperty("$prefix.id", task.id)
            props.setProperty("$prefix.type", task.type)
            props.setProperty("$prefix.payload", task.payload)
            props.setProperty("$prefix.status", task.status.name)
            props.setProperty("$prefix.updatedAt", task.updatedAt)
        }

        Files.newOutputStream(storeFile).use { props.store(it, "GameNative desktop tasks") }
    }
}
