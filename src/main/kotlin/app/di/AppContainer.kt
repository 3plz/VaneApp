package app.di

import app.domain.auth.AuthService
import app.domain.auth.MicrosoftAuthService

/**
 * Главный DI-контейнер приложения.
 * Предоставляет абстрагированные синглтоны сервисов без тяжелых сторонних фреймворков.
 */
class AppContainer {
    // Абстрагированный сервис аутентификации (легко подменить на OfflineAuthService, YggdrasilAuthService и т.д.)
    val authService: AuthService by lazy {
        MicrosoftAuthService()
    }
}
