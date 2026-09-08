package app.domain.launcher

import app.domain.storage.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

data class DownloadProgress(
    val stage: String,
    val currentItem: String,
    val completed: Int,
    val total: Int,
    val percentage: Float
)

/**
 * Service to resolve version manifests, download Vanilla and Mod Loader packages,
 * synchronize client JARs, libraries, and Minecraft assets.
 */
class GameDownloadService(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(12))
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Resolves the full VersionPackage, merging modloader metadata (Fabric/Quilt) with Mojang vanilla package.
     */
    suspend fun resolveVersionPackage(
        minecraftVersion: String,
        modLoader: String = "Vanilla",
        modLoaderVersion: String = ""
    ): VersionPackage = withContext(Dispatchers.IO) {
        // Step 1: Fetch vanilla version package from Mojang
        val vanillaPackage = fetchVanillaPackage(minecraftVersion)

        if (modLoader.equals("Fabric", ignoreCase = true)) {
            val fabricPackage = fetchFabricPackage(minecraftVersion, modLoaderVersion)
            // Merge: Fabric mainClass and Fabric libraries placed before vanilla libraries
            return@withContext VersionPackage(
                id = fabricPackage.id,
                mainClass = fabricPackage.mainClass ?: "net.fabricmc.loader.impl.launch.knot.KnotClient",
                assets = vanillaPackage.assets,
                assetIndex = vanillaPackage.assetIndex,
                downloads = vanillaPackage.downloads,
                libraries = fabricPackage.libraries + vanillaPackage.libraries,
                arguments = fabricPackage.arguments ?: vanillaPackage.arguments,
                minecraftArguments = fabricPackage.minecraftArguments ?: vanillaPackage.minecraftArguments,
                inheritsFrom = minecraftVersion
            )
        }

        vanillaPackage
    }

    private fun fetchVanillaPackage(minecraftVersion: String): VersionPackage {
        val manifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        val manifestReq = HttpRequest.newBuilder().uri(URI.create(manifestUrl)).GET().build()
        val manifestResp = httpClient.send(manifestReq, HttpResponse.BodyHandlers.ofString())
        val manifest = json.decodeFromString<MojangVersionManifest>(manifestResp.body())

        val entry = manifest.versions.firstOrNull { it.id == minecraftVersion }
            ?: manifest.versions.firstOrNull { it.id.equals(minecraftVersion, ignoreCase = true) }
            ?: error("Minecraft version '$minecraftVersion' not found in Mojang version manifest")

        val pkgReq = HttpRequest.newBuilder().uri(URI.create(entry.url)).GET().build()
        val pkgResp = httpClient.send(pkgReq, HttpResponse.BodyHandlers.ofString())
        return json.decodeFromString<VersionPackage>(pkgResp.body())
    }

    private fun fetchFabricPackage(minecraftVersion: String, loaderVersion: String): VersionPackage {
        val actualLoader = if (loaderVersion.isNotBlank()) {
            loaderVersion
        } else {
            // Get latest loader version
            val metaUrl = "https://meta.fabricmc.net/v2/versions/loader/$minecraftVersion"
            val req = HttpRequest.newBuilder().uri(URI.create(metaUrl)).GET().build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            val element = json.parseToJsonElement(resp.body())
            val first = element.toString()
            val regex = Regex(""""version":"([^"]+)"""")
            regex.find(first)?.groupValues?.get(1) ?: error("Failed to resolve Fabric loader version for $minecraftVersion")
        }

        val profileUrl = "https://meta.fabricmc.net/v2/versions/loader/$minecraftVersion/$actualLoader/profile/json"
        val req = HttpRequest.newBuilder().uri(URI.create(profileUrl)).GET().build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        return json.decodeFromString<VersionPackage>(resp.body())
    }

    /**
     * Downloads the client JAR to AppPaths.versionsDir / version / version.jar.
     */
    suspend fun downloadClientJar(
        pkg: VersionPackage,
        minecraftVersion: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): Path = withContext(Dispatchers.IO) {
        val clientDownload = pkg.downloads?.client
            ?: error("Client JAR download information missing for $minecraftVersion")

        val targetDir = AppPaths.versionsDir.resolve(minecraftVersion)
        Files.createDirectories(targetDir)
        val targetJar = targetDir.resolve("$minecraftVersion.jar")

        if (Files.exists(targetJar) && (clientDownload.size == 0L || Files.size(targetJar) == clientDownload.size)) {
            return@withContext targetJar
        }

        onProgress(
            DownloadProgress(
                stage = "Downloading Minecraft $minecraftVersion client...",
                currentItem = "$minecraftVersion.jar",
                completed = 0,
                total = 1,
                percentage = 0.5f
            )
        )

        val req = HttpRequest.newBuilder().uri(URI.create(clientDownload.url)).GET().build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream())
        resp.body().use { input ->
            FileOutputStream(targetJar.toFile()).use { output ->
                input.copyTo(output)
            }
        }

        onProgress(
            DownloadProgress(
                stage = "Client downloaded",
                currentItem = "$minecraftVersion.jar",
                completed = 1,
                total = 1,
                percentage = 1.0f
            )
        )

        targetJar
    }

    /**
     * Downloads all applicable libraries and returns their paths for the classpath.
     */
    suspend fun downloadLibraries(
        libraries: List<Library>,
        onProgress: (DownloadProgress) -> Unit = {}
    ): List<Path> = withContext(Dispatchers.IO) {
        val allowedLibs = libraries.filter { isLibraryAllowedOnWindows(it) }
        val results = mutableListOf<Path>()
        val total = allowedLibs.size
        var completed = 0

        val semaphore = Semaphore(8) // Max 8 concurrent library downloads

        val tasks = allowedLibs.map { lib ->
            async {
                val path = resolveLibraryPath(lib)
                val url = resolveLibraryUrl(lib)

                if (path != null) {
                    val file = path.toFile()
                    val expectedSize = lib.downloads?.artifact?.size ?: 0L

                    if (!file.exists() || (expectedSize > 0L && file.length() != expectedSize)) {
                        if (url != null) {
                            semaphore.withPermit {
                                val parent = path.parent
                                if (parent != null && !Files.exists(parent)) {
                                    Files.createDirectories(parent)
                                }
                                runCatching {
                                    val req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
                                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream())
                                    if (resp.statusCode() in 200..299) {
                                        resp.body().use { inStream ->
                                            FileOutputStream(file).use { outStream ->
                                                inStream.copyTo(outStream)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (Files.exists(path)) {
                        path
                    } else null
                } else null
            }
        }

        for (task in tasks) {
            val path = task.await()
            if (path != null) {
                results.add(path)
            }
            completed++
            if (completed % 5 == 0 || completed == total) {
                onProgress(
                    DownloadProgress(
                        stage = "Downloading libraries...",
                        currentItem = "${results.size} / $total",
                        completed = completed,
                        total = total,
                        percentage = completed.toFloat() / total.coerceAtLeast(1)
                    )
                )
            }
        }

        results
    }

    /**
     * Downloads the asset index and all required Minecraft assets.
     */
    suspend fun downloadAssets(
        assetIndexRef: AssetIndexReference?,
        onProgress: (DownloadProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (assetIndexRef == null) return@withContext

        val indexesDir = AppPaths.assetsDir.resolve("indexes")
        val objectsDir = AppPaths.assetsDir.resolve("objects")
        Files.createDirectories(indexesDir)
        Files.createDirectories(objectsDir)

        val indexFile = indexesDir.resolve("${assetIndexRef.id}.json")

        // 1. Download asset index JSON if missing
        if (!Files.exists(indexFile)) {
            val req = HttpRequest.newBuilder().uri(URI.create(assetIndexRef.url)).GET().build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            Files.writeString(indexFile, resp.body())
        }

        val indexContent = Files.readString(indexFile)
        val assetIndex = json.decodeFromString<AssetIndex>(indexContent)

        val totalObjects = assetIndex.objects.size
        var completed = 0

        val semaphore = Semaphore(16) // Concurrent asset object downloads

        val missingAssets = assetIndex.objects.values.filter { obj ->
            val sub = obj.hash.take(2)
            val dest = objectsDir.resolve(sub).resolve(obj.hash)
            !Files.exists(dest) || (obj.size > 0L && Files.size(dest) != obj.size)
        }

        if (missingAssets.isEmpty()) {
            return@withContext
        }

        val tasks = missingAssets.map { obj ->
            async {
                semaphore.withPermit {
                    val sub = obj.hash.take(2)
                    val subDir = objectsDir.resolve(sub)
                    if (!Files.exists(subDir)) {
                        Files.createDirectories(subDir)
                    }
                    val dest = subDir.resolve(obj.hash)
                    val objUrl = "https://resources.download.minecraft.net/$sub/${obj.hash}"

                    runCatching {
                        val req = HttpRequest.newBuilder().uri(URI.create(objUrl)).GET().build()
                        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream())
                        if (resp.statusCode() in 200..299) {
                            resp.body().use { inStream ->
                                FileOutputStream(dest.toFile()).use { outStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                        }
                    }
                }
            }
        }

        for (task in tasks) {
            task.await()
            completed++
            if (completed % 25 == 0 || completed == missingAssets.size) {
                onProgress(
                    DownloadProgress(
                        stage = "Downloading assets...",
                        currentItem = "$completed / ${missingAssets.size}",
                        completed = completed,
                        total = missingAssets.size,
                        percentage = completed.toFloat() / missingAssets.size.coerceAtLeast(1)
                    )
                )
            }
        }
    }

    private fun isLibraryAllowedOnWindows(library: Library): Boolean {
        if (library.rules.isEmpty()) return true

        var allowed = false
        for (rule in library.rules) {
            val os = rule.os
            val osMatches = os == null || os.name == null || os.name.equals("windows", ignoreCase = true)
            if (osMatches) {
                allowed = rule.action == "allow"
            }
        }
        return allowed
    }

    private fun resolveLibraryPath(library: Library): Path? {
        val directPath = library.downloads?.artifact?.path
        if (!directPath.isNullOrBlank()) {
            return AppPaths.librariesDir.resolve(directPath)
        }

        val mavenPath = mavenCoordinateToPath(library.name) ?: return null
        return AppPaths.librariesDir.resolve(mavenPath)
    }

    private fun resolveLibraryUrl(library: Library): String? {
        val directUrl = library.downloads?.artifact?.url
        if (!directUrl.isNullOrBlank()) {
            return directUrl
        }

        val baseRepo = library.url ?: "https://libraries.minecraft.net/"
        val mavenPath = mavenCoordinateToPath(library.name) ?: return null
        return "${baseRepo.trimEnd('/')}/$mavenPath"
    }

    private fun mavenCoordinateToPath(coordinate: String): String? {
        // format: group:artifact:version[:classifier][@extension]
        val parts = coordinate.split(":")
        if (parts.size < 3) return null

        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val versionWithClassifier = parts.subList(2, parts.size).joinToString(":")

        val version: String
        val classifier: String?
        val extension: String

        val atSplit = versionWithClassifier.split("@")
        extension = if (atSplit.size > 1) atSplit[1] else "jar"
        val mainPart = atSplit[0]

        val colonSplit = mainPart.split(":")
        version = colonSplit[0]
        classifier = if (colonSplit.size > 1) colonSplit[1] else null

        val fileName = if (classifier != null) {
            "$artifact-$version-$classifier.$extension"
        } else {
            "$artifact-$version.$extension"
        }

        return "$group/$artifact/$version/$fileName"
    }
}
