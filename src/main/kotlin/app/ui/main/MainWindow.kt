package app.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import app.domain.launcher.GameLauncher
import app.domain.launcher.LaunchStatus
import app.domain.model.Account
import app.domain.model.UserSession
import app.domain.storage.InstalledInstance
import app.domain.storage.InstanceRepository
import app.domain.storage.SettingsRepository
import app.i18n.AppLanguage
import app.i18n.LocalAppLanguage
import app.i18n.LocalStrings
import app.i18n.getStrings
import app.i18n.strings
import app.theme.StudioTheme
import app.theme.ThemePreset
import app.theme.WickedTheme
import app.ui.components.CustomTitleBar
import app.ui.main.components.*
import app.ui.settings.SettingsView
import kotlinx.coroutines.launch
import java.awt.Dimension

/**
 * Primary launcher dashboard window with reactive account dropdown, settings button,
 * left-hand sidebar navigation tabs, integrated Settings screen, and Create Instance modal.
 * Supports window minimization, maximization, and restoring.
 */
@Composable
fun MainWindow(
    session: UserSession,
    accounts: List<Account>,
    themePreset: ThemePreset,
    onThemeChanged: (ThemePreset) -> Unit,
    onSelectAccount: (Account) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddNewAccount: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onCloseRequest: () -> Unit
) {
    val windowState = rememberWindowState(
        size = DpSize(960.dp, 600.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    val coroutineScope = rememberCoroutineScope()
    val instanceRepository = remember { InstanceRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val instances by instanceRepository.instances.collectAsState()

    val gameLauncher = remember { GameLauncher() }
    var launchStatus by remember { mutableStateOf<LaunchStatus>(LaunchStatus.Idle) }
    var launchingInstanceId by remember { mutableStateOf<String?>(null) }
    var runningInstanceId by remember { mutableStateOf<String?>(null) }

    var isSettingsOpen by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(LauncherTab.INSTANCES) }
    var showCreateInstanceModal by remember { mutableStateOf(false) }

    val handlePlayInstance: (InstalledInstance) -> Unit = { instance ->
        if (launchStatus is LaunchStatus.Preparing || runningInstanceId != null) {
            // Already preparing or running
        } else {
            launchingInstanceId = instance.manifest.id
            coroutineScope.launch {
                val settings = settingsRepository.settings.value
                gameLauncher.launch(
                    instance = instance,
                    session = session,
                    allocatedRamMb = settings.allocatedRamMb,
                    customJavaPath = settings.customJavaPath,
                    coroutineScope = coroutineScope,
                    onStatusChange = { status ->
                        launchStatus = status
                        if (status is LaunchStatus.Running) {
                            runningInstanceId = instance.manifest.id
                            launchingInstanceId = null
                        }
                    },
                    onGameStarted = {
                        // User preference: Minimize launcher window when game starts
                        windowState.isMinimized = true
                    },
                    onGameExited = { exitCode ->
                        // User preference: Restore launcher window when game exits
                        windowState.isMinimized = false
                        runningInstanceId = null
                        launchingInstanceId = null
                    },
                    onPlayTimeTick = { deltaSeconds ->
                        instanceRepository.addPlayTime(instance.manifest.id, deltaSeconds)
                    }
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalStrings provides getStrings(currentLanguage)
    ) {
        Window(
            onCloseRequest = onCloseRequest,
            title = strings.windowTitle,
            state = windowState,
            undecorated = true,
            resizable = true
        ) {
            window.minimumSize = Dimension(820, 520)

            WickedTheme(
                darkTheme = themePreset.isDark,
                themePreset = themePreset
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, StudioTheme.BorderDark),
                    color = StudioTheme.BackgroundDark
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Window top bar with minimize, maximize/restore, and close
                            CustomTitleBar(
                                windowState = windowState,
                                onClose = onCloseRequest,
                                canMaximize = true,
                                canMinimize = true
                            )

                            // Main body with animated transition between launcher view and settings view
                            AnimatedContent(
                                targetState = isSettingsOpen,
                                transitionSpec = {
                                    fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) { showSettings ->
                                if (showSettings) {
                                    SettingsView(
                                        activePreset = themePreset,
                                        onThemeChanged = onThemeChanged,
                                        currentLanguage = currentLanguage,
                                        onLanguageChanged = onLanguageChanged,
                                        onBack = { isSettingsOpen = false }
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp)
                                    ) {
                                        // Top header: account selector popup & settings button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // User profile dropdown listing all registered accounts on click
                                            AccountSelectorDropdown(
                                                currentSession = session,
                                                accounts = accounts,
                                                onSelectAccount = onSelectAccount,
                                                onRemoveAccount = onRemoveAccount,
                                                onAddNewAccount = onAddNewAccount
                                            )

                                            // Right actions: Settings button with SVG icon and label
                                            SettingsButton(
                                                onClick = { isSettingsOpen = true }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Dashboard layout: Left sidebar + Main tab content area
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                                        ) {
                                            // Left-hand sidebar navigation tabs
                                            SidebarTabs(
                                                selectedTab = activeTab,
                                                onTabSelected = { activeTab = it },
                                                currentLanguage = currentLanguage
                                            )

                                            // Right content area responding to the active tab
                                            TabContentPlaceholder(
                                                activeTab = activeTab,
                                                currentLanguage = currentLanguage,
                                                instances = instances,
                                                onCreateInstanceClick = {
                                                    showCreateInstanceModal = true
                                                },
                                                onPlayInstance = handlePlayInstance,
                                                onRenameInstance = { inst, newName ->
                                                    instanceRepository.renameInstance(inst, newName)
                                                },
                                                onDeleteInstance = { inst ->
                                                    instanceRepository.deleteInstance(inst)
                                                },
                                                launchStatus = launchStatus,
                                                launchingInstanceId = launchingInstanceId,
                                                runningInstanceId = runningInstanceId,
                                                onDismissLaunchError = {
                                                    launchStatus = LaunchStatus.Idle
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Modal dialog for adding or importing an instance
                        CreateInstanceModal(
                            visible = showCreateInstanceModal,
                            onDismiss = { showCreateInstanceModal = false },
                            onCreateFromScratchClick = {
                                // Will handle scratch flow
                            },
                            onInstanceCreated = {
                                instanceRepository.refreshSync()
                            }
                        )
                    }
                }
            }
        }
    }
}
