package app.domain.modpack

import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Parser for Modrinth .mrpack archive files.
 * Extracts metadata, dependencies, file counts, bundled icon, and download sizes from modrinth.index.json.
 */
object MrpackReader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    fun parse(file: File): Result<MrpackInfo> = runCatching {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("modrinth.index.json")
                ?: throw IllegalArgumentException("Invalid .mrpack: 'modrinth.index.json' was not found inside the archive.")

            val jsonContent = zip.getInputStream(entry).bufferedReader().readText()
            val index = json.decodeFromString<ModrinthIndex>(jsonContent)

            val mcVersion = index.dependencies["minecraft"] ?: "Unknown"
            val (loaderName, loaderVersion) = when {
                index.dependencies.containsKey("fabric-loader") -> "Fabric" to (index.dependencies["fabric-loader"] ?: "")
                index.dependencies.containsKey("forge") -> "Forge" to (index.dependencies["forge"] ?: "")
                index.dependencies.containsKey("neoforge") -> "NeoForge" to (index.dependencies["neoforge"] ?: "")
                index.dependencies.containsKey("quilt-loader") -> "Quilt" to (index.dependencies["quilt-loader"] ?: "")
                else -> "Vanilla" to ""
            }

            val totalBytes = index.files.sumOf { it.fileSize }

            val localIconBytes = ModrinthIconService.extractLocalIcon(zip)

            MrpackInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeFormatted = formatBytes(file.length()),
                name = index.name,
                versionId = index.versionId,
                summary = index.summary,
                minecraftVersion = mcVersion,
                modLoader = loaderName,
                modLoaderVersion = loaderVersion,
                totalFiles = index.files.size,
                totalDownloadSizeBytes = totalBytes,
                totalDownloadSizeFormatted = formatBytes(totalBytes),
                rawJson = jsonContent,
                iconBytes = localIconBytes
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        val kilo = 1024.0
        val mega = kilo * 1024.0
        val giga = mega * 1024.0
        return when {
            bytes >= giga -> String.format(Locale.US, "%.2f GB", bytes / giga)
            bytes >= mega -> String.format(Locale.US, "%.2f MB", bytes / mega)
            bytes >= kilo -> String.format(Locale.US, "%.1f KB", bytes / kilo)
            else -> "$bytes B"
        }
    }
}
