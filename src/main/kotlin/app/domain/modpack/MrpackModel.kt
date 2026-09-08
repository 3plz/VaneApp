package app.domain.modpack

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthIndex(
    val formatVersion: Int = 1,
    val game: String = "minecraft",
    val versionId: String? = null,
    val name: String,
    val summary: String? = null,
    val files: List<ModrinthIndexFile> = emptyList(),
    val dependencies: Map<String, String> = emptyMap()
)

@Serializable
data class ModrinthIndexFile(
    val path: String,
    val hashes: Map<String, String> = emptyMap(),
    val env: Map<String, String> = emptyMap(),
    val downloads: List<String> = emptyList(),
    val fileSize: Long = 0L
)

data class MrpackInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val name: String,
    val versionId: String?,
    val summary: String?,
    val minecraftVersion: String,
    val modLoader: String,
    val modLoaderVersion: String,
    val totalFiles: Int,
    val totalDownloadSizeBytes: Long,
    val totalDownloadSizeFormatted: String,
    val rawJson: String,
    val iconBytes: ByteArray? = null,
    val iconUrl: String? = null
)
