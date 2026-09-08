package app.domain.storage

import app.domain.modpack.InstanceManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

data class InstalledInstance(
    val directory: Path,
    val manifest: InstanceManifest
)

/**
 * Manages discovery, loading, renaming, deletion, and playtime tracking of installed Minecraft instances
 * located in `%APPDATA%\WickedApp\instances`.
 */
class InstanceRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val _instances = MutableStateFlow<List<InstalledInstance>>(emptyList())
    val instances: StateFlow<List<InstalledInstance>> = _instances.asStateFlow()

    init {
        refreshSync()
    }

    fun refreshSync() {
        val baseDir = AppPaths.instancesDir
        if (!Files.exists(baseDir)) {
            _instances.value = emptyList()
            return
        }

        val list = mutableListOf<InstalledInstance>()
        try {
            Files.list(baseDir).use { stream ->
                stream.filter { Files.isDirectory(it) }.forEach { dir ->
                    val manifestFile = dir.resolve("instance.json")
                    if (Files.exists(manifestFile)) {
                        try {
                            val content = Files.readString(manifestFile)
                            val manifest = json.decodeFromString<InstanceManifest>(content)
                            list.add(InstalledInstance(dir, manifest))
                        } catch (e: Exception) {
                            System.err.println("Error reading instance manifest at $manifestFile: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort by creation time descending (newest first)
        _instances.value = list.sortedByDescending { it.manifest.createdTimestamp }
    }

    suspend fun refresh() = withContext(ioDispatcher) {
        refreshSync()
    }

    fun addPlayTime(instanceId: String, additionalSeconds: Long): Boolean {
        if (additionalSeconds <= 0L) return false
        val current = _instances.value.firstOrNull { it.manifest.id == instanceId } ?: return false
        val manifestFile = current.directory.resolve("instance.json")
        if (!Files.exists(manifestFile)) return false
        return try {
            val updatedManifest = current.manifest.copy(
                totalPlayTimeSeconds = current.manifest.totalPlayTimeSeconds + additionalSeconds,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            Files.writeString(manifestFile, json.encodeToString(updatedManifest))

            // Fast in-memory state update to avoid scanning whole filesystem every tick
            _instances.value = _instances.value.map { inst ->
                if (inst.manifest.id == instanceId) {
                    inst.copy(manifest = updatedManifest)
                } else inst
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renameInstance(instance: InstalledInstance, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return false
        val manifestFile = instance.directory.resolve("instance.json")
        if (!Files.exists(manifestFile)) return false
        return try {
            val updatedManifest = instance.manifest.copy(name = trimmed)
            Files.writeString(manifestFile, json.encodeToString(updatedManifest))
            refreshSync()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteInstance(instance: InstalledInstance): Boolean {
        return try {
            if (Files.exists(instance.directory)) {
                // Delete directory recursively
                Files.walk(instance.directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
            refreshSync()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
