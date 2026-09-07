package app.di

import app.domain.auth.AuthService
import app.domain.auth.MicrosoftAuthService

/**
 * Primary dependency injection container for WickedApp.
 * Provides abstracted singleton services with zero third-party reflection overhead.
 */
class AppContainer {
    /**
     * Authentication service instance.
     * Abstracted under [AuthService] to allow swapping implementations (e.g., Offline/Yggdrasil).
     */
    val authService: AuthService by lazy {
        MicrosoftAuthService()
    }
}
