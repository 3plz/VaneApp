package app.domain.modpack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.zip.ZipFile

/**
 * Service to discover and retrieve the official icon for a Modrinth modpack.
 * Supports:
 * 1. Extracting bundled icons from the .mrpack ZIP archive (icon.png, icon.webp, overrides/icon.png).
 * 2. Fetching the official icon via Modrinth REST API using versionId or project search query.
 */
object ModrinthIconService {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Checks if the .mrpack archive contains a bundled icon.
     */
    fun extractLocalIcon(zip: ZipFile): ByteArray? {
        val candidates = listOf(
            "icon.png", "icon.webp", "icon.jpg", "icon.jpeg",
            "overrides/icon.png", "overrides/icon.webp",
            "client-overrides/icon.png", "client-overrides/icon.webp"
        )

        for (candidate in candidates) {
            val entry = zip.getEntry(candidate) ?: zip.entries().asSequence().firstOrNull {
                it.name.equals(candidate, ignoreCase = true)
            }
            if (entry != null && !entry.isDirectory) {
                return zip.getInputStream(entry).use { it.readBytes() }
            }
        }
        return null
    }

    /**
     * Resolves the icon URL from Modrinth API using either the versionId or pack name search.
     */
    suspend fun resolveIconUrl(versionId: String?, packName: String): String? = withContext(Dispatchers.IO) {
        // Step 1: If versionId is present and looks like a Modrinth version ID, try direct lookup
        if (!versionId.isNullOrBlank() && versionId.matches(Regex("^[a-zA-Z0-9_-]{4,16}$"))) {
            val urlFromVersion = fetchIconUrlViaVersion(versionId)
            if (!urlFromVersion.isNullOrBlank()) {
                return@withContext urlFromVersion
            }
        }

        // Step 2: Fallback to Modrinth Project Search
        val urlFromSearch = fetchIconUrlViaSearch(packName)
        if (!urlFromSearch.isNullOrBlank()) {
            return@withContext urlFromSearch
        }

        null
    }

    /**
     * Downloads image bytes from a direct icon URL.
     */
    suspend fun downloadIconBytes(iconUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(iconUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "WickedApp/1.0.0 (contact: support@wickedapp.local)")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
            if (response.statusCode() in 200..299) {
                response.body()
            } else {
                null
            }
        }.getOrNull()
    }

    private fun fetchIconUrlViaVersion(versionId: String): String? {
        return try {
            val versionUrl = "https://api.modrinth.com/v2/version/$versionId"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(versionUrl))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "WickedApp/1.0.0 (contact: support@wickedapp.local)")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return null

            val root = json.parseToJsonElement(response.body()).jsonObject
            val projectId = root["project_id"]?.jsonPrimitive?.content ?: return null

            fetchIconUrlViaProject(projectId)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchIconUrlViaProject(projectId: String): String? {
        return try {
            val projectUrl = "https://api.modrinth.com/v2/project/$projectId"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(projectUrl))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "WickedApp/1.0.0 (contact: support@wickedapp.local)")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return null

            val root = json.parseToJsonElement(response.body()).jsonObject
            root["icon_url"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchIconUrlViaSearch(packName: String): String? {
        return try {
            val encodedQuery = URLEncoder.encode(packName.trim(), StandardCharsets.UTF_8)
            val searchUrl = "https://api.modrinth.com/v2/search?query=$encodedQuery&limit=3"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "WickedApp/1.0.0 (contact: support@wickedapp.local)")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return null

            val root = json.parseToJsonElement(response.body()).jsonObject
            val hits = root["hits"]?.jsonArray ?: return null

            for (hit in hits) {
                val hitObj = hit.jsonObject
                val iconUrl = hitObj["icon_url"]?.jsonPrimitive?.content
                if (!iconUrl.isNullOrBlank()) {
                    return iconUrl
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
