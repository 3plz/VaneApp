package app.ui.oobe

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import app.i18n.AppLanguage
import app.i18n.LocalAppLanguage
import app.i18n.LocalStrings
import app.i18n.getStrings
import app.i18n.strings
import app.theme.StudioTheme
import app.theme.ThemePreset
import app.theme.WickedTheme
import app.ui.components.CustomTitleBar
import java.awt.Dimension

@Composable
fun OobeWindow(
    currentPreset: ThemePreset,
    onPresetChanged: (ThemePreset) -> Unit,
    onFinish: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCloseRequest: () -> Unit
) {
    val windowState = rememberWindowState(
        size = DpSize(820.dp, 530.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = strings.windowTitle,
        undecorated = true,
        resizable = false,
        transparent = false
    ) {
        LaunchedEffect(window) {
            window.size = Dimension(820, 530)
            window.minimumSize = Dimension(820, 530)
            window.maximumSize = Dimension(820, 530)
        }

        CompositionLocalProvider(
            LocalAppLanguage provides currentLanguage,
            LocalStrings provides getStrings(currentLanguage)
        ) {
            WickedTheme(
                darkTheme = currentPreset.isDark,
                themePreset = currentPreset
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, StudioTheme.BorderDark),
                    color = StudioTheme.BackgroundDark
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CustomTitleBar(
                            windowState = windowState,
                            onClose = onCloseRequest
                        )

                        OobeScreen(
                            currentPreset = currentPreset,
                            onPresetChanged = onPresetChanged,
                            onFinish = onFinish,
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
