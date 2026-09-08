package app

import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import app.di.AppContainer
import app.domain.model.AuthState
import app.i18n.AppLanguage
import app.theme.ThemePresets
import app.ui.login.LoginWindow
import app.ui.main.MainWindow
import app.ui.oobe.OobeWindow
import kotlinx.coroutines.launch

fun main() = application {
    val appContainer = remember { AppContainer() }
    val authService = appContainer.authService
    val accountRepository = appContainer.accountRepository
    val settingsRepository = appContainer.settingsRepository

    val authState by authService.authState.collectAsState()
    val hasMicrosoftAccount by authService.hasMicrosoftAccount.collectAsState()
    val accountsData by accountRepository.accountsData.collectAsState()
    val appSettings by settingsRepository.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Active UI locale (loaded from settings.json, English by default)
    var currentLanguage by remember(appSettings.language) {
        mutableStateOf(if (appSettings.language.equals("ru", ignoreCase = true)) AppLanguage.RU else AppLanguage.EN)
    }

    // Active theme preset (loaded from settings.json)
    var activePreset by remember(appSettings.selectedThemeId) {
        mutableStateOf(ThemePresets.findById(appSettings.selectedThemeId))
    }

    // Restore active account from accounts.json on startup
    LaunchedEffect(Unit) {
        authService.restoreSession()
    }

    when (val state = authState) {
        is AuthState.Authenticated -> {
            if (!appSettings.oobeCompleted) {
                // First-time setup (OOBE): Theme selection
                OobeWindow(
                    currentPreset = activePreset,
                    onPresetChanged = { newPreset ->
                        activePreset = newPreset
                    },
                    onFinish = {
                        coroutineScope.launch {
                            settingsRepository.completeOobe(activePreset.id, activePreset.isDark)
                        }
                    },
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { newLang ->
                        currentLanguage = newLang
                        coroutineScope.launch {
                            settingsRepository.setLanguage(newLang.code)
                        }
                    },
                    onCloseRequest = ::exitApplication
                )
            } else {
                // Show primary launcher dashboard when an account is active and OOBE is complete
                MainWindow(
                    session = state.session,
                    accounts = accountsData.accounts,
                    themePreset = activePreset,
                    onThemeChanged = { newPreset ->
                        activePreset = newPreset
                        coroutineScope.launch {
                            settingsRepository.setTheme(newPreset.id, newPreset.isDark)
                        }
                    },
                    onSelectAccount = { account ->
                        coroutineScope.launch {
                            accountRepository.setActiveAccount(account.id)
                            authService.restoreSession()
                        }
                    },
                    onRemoveAccount = { accountId ->
                        coroutineScope.launch {
                            accountRepository.removeAccount(accountId)
                            authService.restoreSession()
                        }
                    },
                    onAddNewAccount = {
                        coroutineScope.launch {
                            authService.logout()
                        }
                    },
                    currentLanguage = currentLanguage,
                    onLanguageChanged = { newLang ->
                        currentLanguage = newLang
                        coroutineScope.launch {
                            settingsRepository.setLanguage(newLang.code)
                        }
                    },
                    onCloseRequest = ::exitApplication
                )
            }
        }
        else -> {
            // Show login window when no registered account is present or adding a new account
            LoginWindow(
                authState = state,
                themePreset = activePreset,
                canCreateOffline = hasMicrosoftAccount,
                canReturnToMain = accountsData.accounts.isNotEmpty(),
                onReturnToMain = {
                    coroutineScope.launch {
                        authService.restoreSession()
                    }
                },
                onLoginClick = {
                    coroutineScope.launch {
                        authService.login()
                    }
                },
                onOfflineLoginClick = { username ->
                    coroutineScope.launch {
                        authService.loginOffline(username)
                    }
                },
                currentLanguage = currentLanguage,
                onLanguageSelected = { newLang ->
                    currentLanguage = newLang
                    coroutineScope.launch {
                        settingsRepository.setLanguage(newLang.code)
                    }
                },
                onCloseRequest = ::exitApplication
            )
        }
    }
}
