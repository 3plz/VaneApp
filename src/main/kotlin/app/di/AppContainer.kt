package app.di

import app.domain.auth.AuthService
import app.domain.auth.MicrosoftAuthService
import app.domain.storage.AccountRepository
import app.domain.storage.SettingsRepository

/**
 * Primary dependency injection container for WickedApp.
 * Provides abstracted singleton services with zero third-party reflection overhead.
 */
class AppContainer {

    /**
     * Account persistence repository for local storage (accounts.json).
     */
    val accountRepository: AccountRepository by lazy {
        AccountRepository()
    }

    /**
     * Application settings repository for configuration and theme state (settings.json).
     */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository()
    }

    /**
     * Authentication service instance.
     * Abstracted under [AuthService] to allow swapping implementations (e.g., Offline/Yggdrasil).
     */
    val authService: AuthService by lazy {
        MicrosoftAuthService(accountRepository = accountRepository)
    }
}
