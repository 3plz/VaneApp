package app.domain.launcher

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MojangVersionManifest(
    val latest: LatestVersions? = null,
    val versions: List<MojangVersionEntry> = emptyList()
)

@Serializable
data class LatestVersions(
    val release: String,
    val snapshot: String
)

@Serializable
data class MojangVersionEntry(
    val id: String,
    val type: String,
    val url: String,
    val time: String? = null,
    val releaseTime: String? = null,
    val sha1: String? = null
)

@Serializable
data class VersionPackage(
    val id: String,
    val mainClass: String? = null,
    val assets: String? = null,
    val assetIndex: AssetIndexReference? = null,
    val downloads: VersionDownloads? = null,
    val libraries: List<Library> = emptyList(),
    val arguments: VersionArguments? = null,
    val minecraftArguments: String? = null,
    val inheritsFrom: String? = null
)

@Serializable
data class AssetIndexReference(
    val id: String,
    val sha1: String? = null,
    val size: Long = 0L,
    val totalSize: Long = 0L,
    val url: String
)

@Serializable
data class VersionDownloads(
    val client: DownloadFile? = null,
    val server: DownloadFile? = null
)

@Serializable
data class DownloadFile(
    val path: String? = null,
    val sha1: String? = null,
    val size: Long = 0L,
    val url: String
)

@Serializable
data class Library(
    val name: String,
    val downloads: LibraryDownloads? = null,
    val rules: List<Rule> = emptyList(),
    val natives: Map<String, String> = emptyMap(),
    val url: String? = null
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadFile? = null,
    val classifiers: Map<String, DownloadFile> = emptyMap()
)

@Serializable
data class Rule(
    val action: String, // "allow" or "disallow"
    val os: OsRule? = null
)

@Serializable
data class OsRule(
    val name: String? = null, // "windows", "osx", "linux"
    val version: String? = null,
    val arch: String? = null
)

@Serializable
data class VersionArguments(
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList()
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject> = emptyMap()
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long = 0L
)
