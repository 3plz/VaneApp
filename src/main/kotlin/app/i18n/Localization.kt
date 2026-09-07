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
    val loggedInAs: String = "Logged in as: "
)

val StringsEn = AppStrings(
    loginWithMicrosoft = "Login with Microsoft",
    windowTitle = "WickedApp",
    minimize = "Minimize",
    close = "Close",
    waitingInBrowser = "Waiting in browser...",
    codeCopiedHint = "Code copied to clipboard • Enter in browser",
    loggedInAs = "Logged in as: "
)

val StringsRu = AppStrings(
    loginWithMicrosoft = "Войти через Microsoft",
    windowTitle = "WickedApp",
    minimize = "Свернуть",
    close = "Закрыть",
    waitingInBrowser = "Ожидание в браузере...",
    codeCopiedHint = "Код скопирован в буфер • Вставьте в браузере",
    loggedInAs = "Выполнен вход: "
)

fun getStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.RU -> StringsRu
        AppLanguage.EN -> StringsEn
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }
val LocalStrings = staticCompositionLocalOf { StringsEn }

val strings: AppStrings
    @Composable
    get() = LocalStrings.current
