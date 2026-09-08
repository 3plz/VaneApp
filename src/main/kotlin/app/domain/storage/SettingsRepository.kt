package app.domain.storage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable
data class AppSettings(
    val oobeCompleted: Boolean = false,
    val selectedThemeId: String = "dark_default",
    val isDarkTheme: Boolean = true,
    val language: String = "en",
    val allocatedRamMb: Int = 4096,
    val customJavaPath: String? = null
)

/**
 * Manages persistence for WickedApp configuration, theme choices, language, and OOBE status in settings.json.
 */
class SettingsRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val mutex = Mutex()
    private val _settings = MutableStateFlow(loadSettingsSync())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettingsSync(): AppSettings {
        val path = AppPaths.settingsFile
        if (!Files.exists(path)) return AppSettings()
        return try {
            val content = Files.readString(path)
            json.decodeFromString<AppSettings>(content)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = withContext(ioDispatcher) {
        mutex.withLock {
            val newSettings = transform(_settings.value)
            saveSettingsSync(newSettings)
            _settings.value = newSettings
        }
    }

    suspend fun completeOobe(themeId: String, isDark: Boolean) = updateSettings {
        it.copy(
            oobeCompleted = true,
            selectedThemeId = themeId,
            isDarkTheme = isDark
        )
    }

    suspend fun setTheme(themeId: String, isDark: Boolean) = updateSettings {
        it.copy(
            selectedThemeId = themeId,
            isDarkTheme = isDark
        )
    }

    suspend fun setLanguage(languageCode: String) = updateSettings {
        it.copy(language = languageCode)
    }

    suspend fun setJavaAndMemory(ramMb: Int, javaPath: String?) = updateSettings {
        it.copy(
            allocatedRamMb = ramMb,
            customJavaPath = javaPath
        )
    }

    private fun saveSettingsSync(data: AppSettings) {
        val path = AppPaths.settingsFile
        val temp = path.resolveSibling("${path.fileName}.tmp")
        val content = json.encodeToString(data)
        Files.writeString(temp, content)
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
