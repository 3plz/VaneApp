package app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val title: String) {
    EN("en", "English"),
    RU("ru", "Русский")
}

data class AppStrings(
    val loginWithMicrosoft: String,
    val windowTitle: String = "WickedApp",
    val minimize: String = "Minimize",
    val close: String = "Close",
    val waitingInBrowser: String = "Waiting in browser...",
    val codeCopiedHint: String = "Code copied to clipboard • Enter in browser",
    val loggedInAs: String = "Logged in as: ",
    val createOfflineAccount: String = "Create an offline account",
    val offlineNicknamePlaceholder: String = "Minecraft username",
    val continueButton: String = "Continue",
    val back: String = "Back",
    val invalidUsernameHint: String = "3-16 chars: a-z, 0-9, _",
    val logout: String = "Logout",
    val switchAccount: String = "Switch Account",
    val play: String = "Play",
    val welcomeBack: String = "Welcome back",
    val addAccount: String = "Add Account",
    val oobeTitle: String = "Welcome to WickedApp",
    val oobeSubtitle: String = "Choose your preferred theme to get started",
    val darkThemeTab: String = "Dark Themes",
    val lightThemeTab: String = "Light Themes",
    val getStarted: String = "Get Started",
    val settings: String = "Settings",
    val backToLauncher: String = "Back to Launcher",
    val settingsAppearance: String = "Appearance",
    val settingsLanguage: String = "Language",
    val settingsJavaMemory: String = "Java & Memory",
    val settingsGameLauncher: String = "Launcher",
    val settingsAbout: String = "About",
    val selectThemeTitle: String = "Theme & Color Scheme",
    val selectLanguageTitle: String = "Interface Language",
    val englishTitle: String = "English",
    val russianTitle: String = "Русский",
    val allocatedRam: String = "Allocated Memory (RAM)",
    val javaPathTitle: String = "Java Runtime Path",
    val underDevelopment: String = "This section is under development",
    val createInstance: String = "Create Instance",
    val createInstanceHint: String = "Add a new Minecraft version, modpack, or custom instance",
    val newInstanceDialogTitle: String = "New Instance",
    val newInstanceDialogSubtitle: String = "Choose how you want to add an instance",
    val importInstanceTitle: String = "Import",
    val importInstanceSubtitle: String = "From archive (.zip, .mrpack), CurseForge, or folder",
    val createFromScratchTitle: String = "Create from scratch",
    val createFromScratchSubtitle: String = "Choose Minecraft version (Vanilla, Fabric, Forge, NeoForge)",
    val modpackInspectorTitle: String = "Modpack Info (.mrpack)",
    val modpackInspectorSubtitle: String = "Parsed package metadata and dependencies",
    val chooseAnotherFile: String = "Choose another file",
    val rawJsonTitle: String = "Raw modrinth.index.json",
    val copiedToClipboard: String = "Copied!",
    val copyJson: String = "Copy JSON",
    val invalidFile: String = "Error reading modpack",
    val installInstance: String = "Install Instance",
    val installingInstance: String = "Installing Instance...",
    val installationComplete: String = "Installation Complete!",
    val openFolder: String = "Open Folder",
    val done: String = "Done",
    val rename: String = "Rename",
    val renameInstanceTitle: String = "Rename Instance",
    val renameInstancePrompt: String = "Enter a new name for this instance",
    val delete: String = "Delete",
    val deleteInstanceTitle: String = "Delete Instance?",
    val deleteInstancePrompt: String = "Are you sure you want to delete this instance? This action cannot be undone.",
    val cancel: String = "Cancel",
    val save: String = "Save",
    val backToInstances: String = "Back to instances",
    val modsTab: String = "Mods",
    val overviewTab: String = "Overview",
    val screenshotsTab: String = "Screenshots",
    val noModsFound: String = "No mods found in mods folder",
    val searchMods: String = "Search mods...",
    val launching: String = "Launching...",
    val gameRunning: String = "In Game",
    val launchFailed: String = "Launch Failed",
    val playtime: String = "Playtime",
    val totalPlaytime: String = "Total Playtime",
    val lastPlayed: String = "Last Played"
)

val StringsEn = AppStrings(
    loginWithMicrosoft = "Login with Microsoft",
    windowTitle = "WickedApp",
    minimize = "Minimize",
    close = "Close",
    waitingInBrowser = "Waiting in browser...",
    codeCopiedHint = "Code copied to clipboard • Enter in browser",
    loggedInAs = "Logged in as: ",
    createOfflineAccount = "Create an offline account",
    offlineNicknamePlaceholder = "Minecraft username",
    continueButton = "Continue",
    back = "Back",
    invalidUsernameHint = "3-16 chars: a-z, 0-9, _",
    logout = "Logout",
    switchAccount = "Switch Account",
    play = "Play",
    welcomeBack = "Welcome back",
    addAccount = "Add Account",
    oobeTitle = "Welcome to WickedApp",
    oobeSubtitle = "Choose your preferred theme to get started",
    darkThemeTab = "Dark Themes",
    lightThemeTab = "Light Themes",
    getStarted = "Get Started",
    settings = "Settings",
    backToLauncher = "Back to Launcher",
    settingsAppearance = "Appearance",
    settingsLanguage = "Language",
    settingsJavaMemory = "Java & Memory",
    settingsGameLauncher = "Launcher",
    settingsAbout = "About",
    selectThemeTitle = "Theme & Color Scheme",
    selectLanguageTitle = "Interface Language",
    englishTitle = "English",
    russianTitle = "Русский",
    allocatedRam = "Allocated Memory (RAM)",
    javaPathTitle = "Java Runtime Path",
    underDevelopment = "This section is under development",
    createInstance = "Create Instance",
    createInstanceHint = "Add a new Minecraft version, modpack, or custom instance",
    newInstanceDialogTitle = "New Instance",
    newInstanceDialogSubtitle = "Choose how you want to add an instance",
    importInstanceTitle = "Import",
    importInstanceSubtitle = "From archive (.zip, .mrpack), CurseForge, or folder",
    createFromScratchTitle = "Create from scratch",
    createFromScratchSubtitle = "Choose Minecraft version (Vanilla, Fabric, Forge, NeoForge)",
    modpackInspectorTitle = "Modpack Info (.mrpack)",
    modpackInspectorSubtitle = "Parsed package metadata and dependencies",
    chooseAnotherFile = "Choose another file",
    rawJsonTitle = "Raw modrinth.index.json",
    copiedToClipboard = "Copied!",
    copyJson = "Copy JSON",
    invalidFile = "Error reading modpack",
    installInstance = "Install Instance",
    installingInstance = "Installing Instance...",
    installationComplete = "Installation Complete!",
    openFolder = "Open Folder",
    done = "Done",
    rename = "Rename",
    renameInstanceTitle = "Rename Instance",
    renameInstancePrompt = "Enter a new name for this instance",
    delete = "Delete",
    deleteInstanceTitle = "Delete Instance?",
    deleteInstancePrompt = "Are you sure you want to delete this instance? This action cannot be undone.",
    cancel = "Cancel",
    save = "Save",
    backToInstances = "Back to instances",
    modsTab = "Mods",
    overviewTab = "Overview",
    screenshotsTab = "Screenshots",
    noModsFound = "No mods found in mods folder",
    searchMods = "Search mods...",
    launching = "Launching...",
    gameRunning = "In Game",
    launchFailed = "Launch Failed",
    playtime = "Playtime",
    totalPlaytime = "Total Playtime",
    lastPlayed = "Last Played"
)

val StringsRu = AppStrings(
    loginWithMicrosoft = "Войти через Microsoft",
    windowTitle = "WickedApp",
    minimize = "Свернуть",
    close = "Закрыть",
    waitingInBrowser = "Ожидание в браузере...",
    codeCopiedHint = "Код скопирован в буфер • Вставьте в браузере",
    loggedInAs = "Выполнен вход: ",
    createOfflineAccount = "Создать оффлайн-аккаунт",
    offlineNicknamePlaceholder = "Никнейм в Minecraft",
    continueButton = "Продолжить",
    back = "Назад",
    invalidUsernameHint = "3-16 симв: a-z, 0-9, _",
    logout = "Выйти",
    switchAccount = "Сменить аккаунт",
    play = "Играть",
    welcomeBack = "С возвращением",
    addAccount = "Добавить аккаунт",
    oobeTitle = "Добро пожаловать в WickedApp",
    oobeSubtitle = "Выберите оформление по душе перед началом игры",
    darkThemeTab = "Тёмные темы",
    lightThemeTab = "Светлые темы",
    getStarted = "Начать игру",
    settings = "Настройки",
    backToLauncher = "Назад в лаунчер",
    settingsAppearance = "Внешний вид",
    settingsLanguage = "Язык",
    settingsJavaMemory = "Java и память",
    settingsGameLauncher = "Лаунчер",
    settingsAbout = "О программе",
    selectThemeTitle = "Оформление и цветовая схема",
    selectLanguageTitle = "Язык интерфейса",
    englishTitle = "English",
    russianTitle = "Русский",
    allocatedRam = "Выделение оперативной памяти (RAM)",
    javaPathTitle = "Путь к среде Java",
    underDevelopment = "Раздел находится в разработке",
    createInstance = "Создать сборку",
    createInstanceHint = "Установить новую версию Minecraft, модпак или сборку",
    newInstanceDialogTitle = "Новая сборка",
    newInstanceDialogSubtitle = "Выберите способ добавления сборки",
    importInstanceTitle = "Импортировать",
    importInstanceSubtitle = "Из архива (.zip, .mrpack), CurseForge или папки",
    createFromScratchTitle = "Создать с нуля",
    createFromScratchSubtitle = "Выбрать версию Minecraft (Vanilla, Fabric, Forge, NeoForge)",
    modpackInspectorTitle = "Информация о модпаке (.mrpack)",
    modpackInspectorSubtitle = "Распознанные метаданные и зависимости",
    chooseAnotherFile = "Выбрать другой файл",
    rawJsonTitle = "Исходный modrinth.index.json",
    copiedToClipboard = "Скопировано!",
    copyJson = "Копировать JSON",
    invalidFile = "Ошибка чтения модпака",
    installInstance = "Установить сборку",
    installingInstance = "Установка сборки...",
    installationComplete = "Сборка успешно установлена!",
    openFolder = "Открыть папку",
    done = "Готово",
    rename = "Переименовать",
    renameInstanceTitle = "Переименование сборки",
    renameInstancePrompt = "Введите новое название для сборки",
    delete = "Удалить",
    deleteInstanceTitle = "Удалить сборку?",
    deleteInstancePrompt = "Вы уверены, что хотите удалить эту сборку? Это действие нельзя отменить.",
    cancel = "Отмена",
    save = "Сохранить",
    backToInstances = "Назад к сборкам",
    modsTab = "Моды",
    overviewTab = "Обзор",
    screenshotsTab = "Скриншоты",
    noModsFound = "В папке mods нет файлов",
    searchMods = "Поиск модов...",
    launching = "Запуск...",
    gameRunning = "В игре",
    launchFailed = "Ошибка запуска",
    playtime = "Время в игре",
    totalPlaytime = "Всего в игре",
    lastPlayed = "Последний запуск"
)

val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }
val LocalStrings = staticCompositionLocalOf { StringsEn }

fun getStrings(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.EN -> StringsEn
    AppLanguage.RU -> StringsRu
}

val strings: AppStrings
    @Composable
    get() = LocalStrings.current

/**
 * Formats a duration in seconds into human-readable playtime (e.g. "2 ч. 15 мин." or "2h 15m").
 */
fun formatPlayTime(seconds: Long, language: AppLanguage): String {
    if (seconds <= 0L) {
        return if (language == AppLanguage.RU) "0 мин." else "0 min"
    }
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (language == AppLanguage.RU) {
        when {
            hours > 0 && remainingMinutes > 0 -> "$hours ч. $remainingMinutes мин."
            hours > 0 -> "$hours ч."
            minutes > 0 -> "$minutes мин."
            else -> "< 1 мин."
        }
    } else {
        when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
