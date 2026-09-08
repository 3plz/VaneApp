package app.domain.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Cross-platform path provider for WickedApp application data.
 */
object AppPaths {

    val appDataDir: Path by lazy {
        val os = System.getProperty("os.name").lowercase()
        val path = when {
            "win" in os -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) {
                    Paths.get(appData, "WickedApp")
                } else {
                    Paths.get(System.getProperty("user.home"), ".wickedapp")
                }
            }
            "mac" in os -> {
                Paths.get(System.getProperty("user.home"), "Library", "Application Support", "WickedApp")
            }
            else -> {
                val xdgData = System.getenv("XDG_DATA_HOME")
                if (!xdgData.isNullOrBlank()) {
                    Paths.get(xdgData, "WickedApp")
                } else {
                    Paths.get(System.getProperty("user.home"), ".wickedapp")
                }
            }
        }
        Files.createDirectories(path)
        path
    }

    val accountsFile: Path
        get() = appDataDir.resolve("accounts.json")

    val settingsFile: Path
        get() = appDataDir.resolve("settings.json")

    val instancesDir: Path
        get() {
            val dir = appDataDir.resolve("instances")
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            return dir
        }

    val assetsDir: Path
        get() {
            val dir = appDataDir.resolve("assets")
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            return dir
        }

    val librariesDir: Path
        get() {
            val dir = appDataDir.resolve("libraries")
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            return dir
        }

    val versionsDir: Path
        get() {
            val dir = appDataDir.resolve("versions")
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            return dir
        }
}
