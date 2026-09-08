package app.domain.launcher

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

data class JavaInstallation(
    val binaryPath: Path,
    val majorVersion: Int,
    val versionString: String,
    val vendor: String? = null,
    val is64Bit: Boolean = true
)

/**
 * Discovers and inspects Java runtimes on the system.
 * Reads metadata from the JRE 'release' file for zero-overhead parsing,
 * with process execution as a reliable fallback.
 */
object JavaDetector {

    /**
     * Finds the best Java binary suitable for the target Minecraft version.
     * Prioritizes custom user path, then the highest available version matching [minVersion].
     */
    fun findBestJava(minVersion: Int = 21, customPath: String? = null): Path? {
        if (!customPath.isNullOrBlank()) {
            val customFile = Paths.get(customPath)
            val binary = if (Files.isDirectory(customFile)) {
                customFile.resolve("bin").resolve(if (isWindows()) "java.exe" else "java")
            } else {
                customFile
            }
            if (Files.isExecutable(binary)) {
                return binary
            }
        }

        val allJava = scanAllJava()
        // First look for matching major version >= minVersion
        val suitable = allJava.filter { it.majorVersion >= minVersion }
            .maxByOrNull { it.majorVersion }

        if (suitable != null) {
            return suitable.binaryPath
        }

        // Fallback: any detected Java
        val anyJava = allJava.maxByOrNull { it.majorVersion }
        if (anyJava != null) {
            return anyJava.binaryPath
        }

        // Last resort: standard system PATH command
        return Paths.get(if (isWindows()) "java.exe" else "java")
    }

    /**
     * Scans all known locations on the machine for Java installations.
     */
    fun scanAllJava(): List<JavaInstallation> {
        val candidates = mutableSetOf<Path>()

        // 1. Current runtime java.home
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            resolveJavaBinary(Paths.get(javaHome))?.let { candidates.add(it) }
        }

        // 2. PATH environment variable
        val pathEnv = System.getenv("PATH") ?: ""
        val pathSeparator = if (isWindows()) ";" else ":"
        for (dir in pathEnv.split(pathSeparator)) {
            if (dir.isNotBlank()) {
                val p = Paths.get(dir)
                resolveJavaBinary(p)?.let { candidates.add(it) }
            }
        }

        // 3. User Scoop installations (Windows)
        val userHome = System.getProperty("user.home")
        if (!userHome.isNullOrBlank()) {
            val scoopApps = Paths.get(userHome, "scoop", "apps")
            if (Files.exists(scoopApps) && Files.isDirectory(scoopApps)) {
                try {
                    Files.list(scoopApps).use { stream ->
                        stream.forEach { appDir ->
                            val currentBin = appDir.resolve("current")
                            resolveJavaBinary(currentBin)?.let { candidates.add(it) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Windows Standard Program Files
        if (isWindows()) {
            val standardRoots = listOf(
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)"),
                System.getenv("LOCALAPPDATA")?.let { "$it\\Programs" }
            ).filterNotNull()

            val javaFolders = listOf("Java", "Eclipse Adoptium", "BellSoft", "Zulu", "Microsoft", "Semeru")

            for (root in standardRoots) {
                val rootPath = Paths.get(root)
                if (Files.exists(rootPath)) {
                    for (folder in javaFolders) {
                        val vendorDir = rootPath.resolve(folder)
                        if (Files.exists(vendorDir) && Files.isDirectory(vendorDir)) {
                            try {
                                Files.list(vendorDir).use { stream ->
                                    stream.forEach { jdkDir ->
                                        resolveJavaBinary(jdkDir)?.let { candidates.add(it) }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        return candidates.mapNotNull { inspectJava(it) }
            .distinctBy { it.binaryPath.toAbsolutePath().normalize() }
            .sortedByDescending { it.majorVersion }
    }

    private fun resolveJavaBinary(path: Path): Path? {
        val exeName = if (isWindows()) "java.exe" else "java"
        val direct = if (path.endsWith(exeName)) path else path.resolve(exeName)
        if (Files.isRegularFile(direct)) return direct

        val binExe = path.resolve("bin").resolve(exeName)
        if (Files.isRegularFile(binExe)) return binExe

        return null
    }

    /**
     * Inspects a Java binary by reading the JRE 'release' file or falling back to 'java -version'.
     */
    fun inspectJava(binary: Path): JavaInstallation? {
        if (!Files.isRegularFile(binary)) return null

        // 1. Try reading the 'release' file in parent (if binary is in bin/)
        val jreHome = binary.parent?.parent
        if (jreHome != null) {
            val releaseFile = jreHome.resolve("release")
            if (Files.isRegularFile(releaseFile)) {
                try {
                    val lines = Files.readAllLines(releaseFile)
                    var versionStr = ""
                    var vendor: String? = null

                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("JAVA_VERSION=")) {
                            versionStr = trimmed.removePrefix("JAVA_VERSION=").trim('"', '\'')
                        } else if (trimmed.startsWith("IMPLEMENTOR=")) {
                            vendor = trimmed.removePrefix("IMPLEMENTOR=").trim('"', '\'')
                        }
                    }

                    if (versionStr.isNotBlank()) {
                        val major = parseMajorVersion(versionStr)
                        return JavaInstallation(
                            binaryPath = binary,
                            majorVersion = major,
                            versionString = versionStr,
                            vendor = vendor,
                            is64Bit = true
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Fallback: run 'java -version'
        return try {
            val process = ProcessBuilder(binary.toString(), "-version")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(2, TimeUnit.SECONDS)

            val firstLine = output.lineSequence().firstOrNull() ?: ""
            val regex = Regex("""version "([^"]+)"""")
            val match = regex.find(firstLine)
            val ver = match?.groupValues?.get(1) ?: "Unknown"
            val major = parseMajorVersion(ver)

            JavaInstallation(
                binaryPath = binary,
                majorVersion = major,
                versionString = ver,
                vendor = null,
                is64Bit = output.contains("64-Bit", ignoreCase = true)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMajorVersion(versionStr: String): Int {
        val parts = versionStr.split(".", "-", "_", "+")
        val first = parts.firstOrNull()?.toIntOrNull() ?: 0
        return if (first == 1 && parts.size > 1) {
            // Legacy Java 1.8 -> 8
            parts[1].toIntOrNull() ?: 8
        } else {
            first
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }
}
