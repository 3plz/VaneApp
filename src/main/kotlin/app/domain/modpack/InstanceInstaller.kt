package app.domain.modpack

import app.domain.storage.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.zip.ZipFile

@Serializable
data class InstanceManifest(
    val id: String,
    val name: String,
    val minecraftVersion: String,
    val modLoader: String,
    val modLoaderVersion: String,
    val totalFiles: Int,
    val description: String? = null,
    val iconPath: String? = null,
    val bannerPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val totalPlayTimeSeconds: Long = 0L,
    val lastPlayedTimestamp: Long = 0L
)

data class InstallProgress(
    val stage: String,
    val currentItem: String,
    val completedItems: Int,
    val totalItems: Int,
    val percentage: Float
)

/**
 * Downloads and sets up an instance from a Modrinth .mrpack archive.
 * Extracts overrides and downloads mod dependencies into an isolated instance directory.
 */
class InstanceInstaller {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun installFromMrpack(
        mrpackFile: File,
        packInfo: MrpackInfo,
        instanceName: String = packInfo.name,
        onProgress: (InstallProgress) -> Unit
    ): Result<Path> = withContext(Dispatchers.IO) {
        runCatching {
            val safeFolderName = instanceName
                .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                .trim()
                .ifBlank { "instance_${System.currentTimeMillis()}" }

            val instancesBase = AppPaths.instancesDir
            var targetDir = instancesBase.resolve(safeFolderName)
            var counter = 1
            while (Files.exists(targetDir)) {
                targetDir = instancesBase.resolve("${safeFolderName}_$counter")
                counter++
            }
            Files.createDirectories(targetDir)

            onProgress(
                InstallProgress(
                    stage = "Extracting overrides...",
                    currentItem = "Preparing archive",
                    completedItems = 0,
                    totalItems = 1,
                    percentage = 0.05f
                )
            )

            var extractedIconPath: String? = null

            // Step 1: Extract overrides and client-overrides, as well as root icon if present
            ZipFile(mrpackFile).use { zip ->
                val entries = zip.entries().toList()

                // Check for root icon
                val rootIconEntry = entries.firstOrNull {
                    it.name.equals("icon.png", ignoreCase = true) ||
                    it.name.equals("icon.webp", ignoreCase = true)
                }
                if (rootIconEntry != null) {
                    val iconDest = targetDir.resolve("icon.png")
                    zip.getInputStream(rootIconEntry).use { input ->
                        FileOutputStream(iconDest.toFile()).use { output ->
                            input.copyTo(output)
                        }
                    }
                    extractedIconPath = "icon.png"
                }

                val overrideEntries = entries.filter {
                    !it.isDirectory && (it.name.startsWith("overrides/") || it.name.startsWith("client-overrides/"))
                }

                var extractedCount = 0
                for (entry in overrideEntries) {
                    val relativePath = when {
                        entry.name.startsWith("overrides/") -> entry.name.removePrefix("overrides/")
                        entry.name.startsWith("client-overrides/") -> entry.name.removePrefix("client-overrides/")
                        else -> entry.name
                    }

                    if (relativePath.isNotBlank()) {
                        val destination = targetDir.resolve(relativePath)
                        val parent = destination.parent
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent)
                        }

                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destination.toFile()).use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (relativePath.equals("icon.png", ignoreCase = true) || relativePath.equals("icon.webp", ignoreCase = true)) {
                            extractedIconPath = "icon.png"
                        }
                    }

                    extractedCount++
                    val pct = 0.05f + (extractedCount.toFloat() / overrideEntries.size.coerceAtLeast(1)) * 0.15f
                    onProgress(
                        InstallProgress(
                            stage = "Extracting overrides...",
                            currentItem = relativePath,
                            completedItems = extractedCount,
                            totalItems = overrideEntries.size,
                            percentage = pct
                        )
                    )
                }
            }

            // Step 1.5: If icon was not inside the archive, fetch it from Modrinth API or cached packInfo
            if (extractedIconPath == null) {
                val iconBytes = packInfo.iconBytes ?: run {
                    val iconUrl = packInfo.iconUrl ?: ModrinthIconService.resolveIconUrl(packInfo.versionId, packInfo.name)
                    if (iconUrl != null) {
                        ModrinthIconService.downloadIconBytes(iconUrl)
                    } else null
                }

                if (iconBytes != null && iconBytes.isNotEmpty()) {
                    try {
                        val iconDest = targetDir.resolve("icon.png")
                        Files.write(iconDest, iconBytes)
                        extractedIconPath = "icon.png"
                    } catch (e: Exception) {
                        System.err.println("Warning: failed to save modpack icon: ${e.message}")
                    }
                }
            }

            // Step 2: Parse files list and download client-required mods
            val index = ZipFile(mrpackFile).use { zip ->
                val entry = zip.getEntry("modrinth.index.json")
                    ?: error("Missing modrinth.index.json in mrpack")
                val content = zip.getInputStream(entry).bufferedReader().readText()
                json.decodeFromString<ModrinthIndex>(content)
            }

            val filesToDownload = index.files.filter { fileEntry ->
                val clientEnv = fileEntry.env["client"]
                clientEnv == null || clientEnv != "unsupported"
            }

            val totalDownloads = filesToDownload.size
            var downloadedCount = 0

            for (fileEntry in filesToDownload) {
                val destination = targetDir.resolve(fileEntry.path)
                val parent = destination.parent
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent)
                }

                // If file exists and size matches, skip redownload
                val fileObj = destination.toFile()
                val fileName = destination.fileName.toString()

                if (!fileObj.exists() || (fileEntry.fileSize > 0 && fileObj.length() != fileEntry.fileSize)) {
                    var downloaded = false
                    var lastError: Exception? = null

                    for (url in fileEntry.downloads) {
                        try {
                            val request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(30))
                                .header("User-Agent", "WickedApp/1.0.0 (contact: support@wickedapp.local)")
                                .GET()
                                .build()

                            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
                            if (response.statusCode() in 200..299) {
                                response.body().use { stream ->
                                    FileOutputStream(fileObj).use { outStream ->
                                        stream.copyTo(outStream)
                                    }
                                }
                                downloaded = true
                                break
                            }
                        } catch (e: Exception) {
                            lastError = e
                        }
                    }

                    if (!downloaded) {
                        System.err.println("Warning: failed to download $fileName: ${lastError?.message}")
                    }
                }

                downloadedCount++
                val pct = 0.20f + (downloadedCount.toFloat() / totalDownloads.coerceAtLeast(1)) * 0.75f
                onProgress(
                    InstallProgress(
                        stage = "Downloading mods and assets...",
                        currentItem = fileName,
                        completedItems = downloadedCount,
                        totalItems = totalDownloads,
                        percentage = pct
                    )
                )
            }

            // Step 3: Write instance manifest
            val manifest = InstanceManifest(
                id = UUID.randomUUID().toString(),
                name = instanceName,
                minecraftVersion = packInfo.minecraftVersion,
                modLoader = packInfo.modLoader,
                modLoaderVersion = packInfo.modLoaderVersion,
                totalFiles = downloadedCount,
                description = packInfo.summary,
                iconPath = extractedIconPath,
                createdTimestamp = System.currentTimeMillis(),
                totalPlayTimeSeconds = 0L,
                lastPlayedTimestamp = 0L
            )

            val manifestFile = targetDir.resolve("instance.json")
            Files.writeString(manifestFile, json.encodeToString(manifest))

            onProgress(
                InstallProgress(
                    stage = "Installation complete!",
                    currentItem = manifest.name,
                    completedItems = totalDownloads,
                    totalItems = totalDownloads,
                    percentage = 1.0f
                )
            )

            targetDir
        }
    }
}
