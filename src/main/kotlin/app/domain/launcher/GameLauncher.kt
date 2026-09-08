package app.domain.launcher

import app.domain.model.UserSession
import app.domain.storage.AppPaths
import app.domain.storage.InstalledInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

sealed class LaunchStatus {
    object Idle : LaunchStatus()
    data class Preparing(val stage: String, val detail: String = "", val percentage: Float = 0f) : LaunchStatus()
    data class Running(val process: Process, val pid: Long) : LaunchStatus()
    data class Exited(val exitCode: Int) : LaunchStatus()
    data class Failed(val error: String) : LaunchStatus()
}

/**
 * Orchestrates the full lifecycle of launching a Minecraft instance:
 * 1. Resolves suitable Java 21+ runtime
 * 2. Fetches and parses Minecraft & Fabric/Mod Loader manifests
 * 3. Downloads client JAR, dependencies, and assets
 * 4. Assembles JVM and game arguments
 * 5. Spawns the game process, pipes logs, and tracks process lifecycle and playtime
 */
class GameLauncher(
    private val downloadService: GameDownloadService = GameDownloadService()
) {

    suspend fun launch(
        instance: InstalledInstance,
        session: UserSession,
        allocatedRamMb: Int = 4096,
        customJavaPath: String? = null,
        coroutineScope: CoroutineScope,
        onStatusChange: (LaunchStatus) -> Unit,
        onGameStarted: () -> Unit = {},
        onGameExited: (Int) -> Unit = {},
        onPlayTimeTick: (Long) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Detect suitable Java runtime (Java 21+ required for modern Minecraft)
            onStatusChange(LaunchStatus.Preparing("Detecting Java runtime...", "", 0.05f))
            val javaPath = JavaDetector.findBestJava(minVersion = 21, customPath = customJavaPath)
                ?: error("Could not find a suitable Java 21+ installation on your system. Please configure Java in Settings.")

            println("[GameLauncher] Using Java: $javaPath")

            // Step 2: Resolve version metadata & libraries
            onStatusChange(LaunchStatus.Preparing("Resolving version metadata...", instance.manifest.minecraftVersion, 0.15f))
            val pkg = downloadService.resolveVersionPackage(
                minecraftVersion = instance.manifest.minecraftVersion,
                modLoader = instance.manifest.modLoader,
                modLoaderVersion = instance.manifest.modLoaderVersion
            )

            // Step 3: Download Minecraft client JAR
            onStatusChange(LaunchStatus.Preparing("Verifying client JAR...", instance.manifest.minecraftVersion, 0.35f))
            val clientJar = downloadService.downloadClientJar(pkg, instance.manifest.minecraftVersion) { progress ->
                onStatusChange(LaunchStatus.Preparing(
                    stage = progress.stage,
                    detail = progress.currentItem,
                    percentage = 0.35f + (progress.percentage * 0.10f)
                ))
            }

            // Step 4: Download libraries and natives
            onStatusChange(LaunchStatus.Preparing("Downloading libraries...", "", 0.45f))
            val libraryPaths = downloadService.downloadLibraries(pkg.libraries) { progress ->
                onStatusChange(LaunchStatus.Preparing(
                    stage = progress.stage,
                    detail = progress.currentItem,
                    percentage = 0.45f + (progress.percentage * 0.35f)
                ))
            }

            // Step 5: Download assets
            onStatusChange(LaunchStatus.Preparing("Downloading assets...", "", 0.82f))
            downloadService.downloadAssets(pkg.assetIndex) { progress ->
                onStatusChange(LaunchStatus.Preparing(
                    stage = progress.stage,
                    detail = progress.currentItem,
                    percentage = 0.82f + (progress.percentage * 0.14f)
                ))
            }
            val assetsDir = AppPaths.assetsDir

            // Step 6: Assemble command line
            onStatusChange(LaunchStatus.Preparing("Constructing launch arguments...", "", 0.98f))

            // Build classpath explicitly without flattening Path components (Path implements Iterable<Path>)
            val allClasspathElements = ArrayList<Path>(libraryPaths.size + 1)
            allClasspathElements.addAll(libraryPaths)
            allClasspathElements.add(clientJar)
            val classpath = allClasspathElements.joinToString(File.pathSeparator) { it.toAbsolutePath().toString() }

            val command = mutableListOf<String>()
            command.add(javaPath.toAbsolutePath().toString())

            // JVM arguments
            command.add("-Xms${(allocatedRamMb / 2).coerceAtLeast(1024)}M")
            command.add("-Xmx${allocatedRamMb}M")
            command.add("-Djava.library.path=${AppPaths.librariesDir.toAbsolutePath()}")
            command.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=${AppPaths.librariesDir.toAbsolutePath()}")
            command.add("-Dfabric.gameJarPath=${clientJar.toAbsolutePath()}")
            command.add("--enable-native-access=ALL-UNNAMED")
            command.add("--sun-misc-unsafe-memory-access=allow")
            command.add("-cp")
            command.add(classpath)
            command.add(pkg.mainClass ?: "net.fabricmc.loader.impl.launch.knot.KnotClient")

            // Minecraft Game arguments
            command.add("--version")
            command.add(instance.manifest.minecraftVersion)

            command.add("--gameDir")
            command.add(instance.directory.toAbsolutePath().toString())

            command.add("--assetsDir")
            command.add(assetsDir.toAbsolutePath().toString())

            command.add("--assetIndex")
            command.add(pkg.assetIndex?.id ?: "legacy")

            command.add("--uuid")
            command.add(session.userId.replace("-", ""))

            command.add("--accessToken")
            command.add(session.accessToken.ifBlank { "0" })

            command.add("--username")
            command.add(session.username)

            command.add("--userType")
            command.add(if (session.isOffline) "legacy" else "msa")

            command.add("--versionType")
            command.add("release")

            println("[GameLauncher] Launching: ${command.joinToString(" ")}")

            // Step 7: Spawn process
            val logsDir = instance.directory.resolve("logs")
            Files.createDirectories(logsDir)
            val logFile = logsDir.resolve("latest.log").toFile()

            val processBuilder = ProcessBuilder(command)
                .directory(instance.directory.toFile())
                .redirectErrorStream(true)

            val process = processBuilder.start()
            val pid = process.pid()
            println("[GameLauncher] Process started with PID: $pid")

            onStatusChange(LaunchStatus.Running(process, pid))
            onGameStarted()

            // Periodically track active playtime while game process runs
            val playTimeJob = coroutineScope.launch(Dispatchers.IO) {
                var lastTick = System.currentTimeMillis()
                while (process.isAlive) {
                    delay(5000)
                    val now = System.currentTimeMillis()
                    val deltaSeconds = (now - lastTick) / 1000
                    if (deltaSeconds > 0) {
                        onPlayTimeTick(deltaSeconds)
                        lastTick = now
                    }
                }
                val finalNow = System.currentTimeMillis()
                val finalDelta = (finalNow - lastTick) / 1000
                if (finalDelta > 0) {
                    onPlayTimeTick(finalDelta)
                }
            }

            // Stream logs to console & instance latest.log in background
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    FileOutputStream(logFile).use { logStream ->
                        process.inputStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                println("[Minecraft] $line")
                                logStream.write("$line\n".toByteArray())
                                logStream.flush()
                            }
                        }
                    }
                } catch (_: Exception) {}

                val exitCode = process.waitFor()
                playTimeJob.join()
                println("[GameLauncher] Process exited with code: $exitCode")
                onStatusChange(LaunchStatus.Exited(exitCode))
                onGameExited(exitCode)
            }

        }.onFailure { err ->
            err.printStackTrace()
            onStatusChange(LaunchStatus.Failed(err.message ?: "Unknown launch error"))
        }
    }
}
