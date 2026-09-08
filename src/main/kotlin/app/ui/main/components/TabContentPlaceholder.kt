package app.ui.main.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.launcher.LaunchStatus
import app.domain.storage.InstalledInstance
import app.i18n.AppLanguage
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

@Composable
fun TabContentPlaceholder(
    activeTab: LauncherTab,
    currentLanguage: AppLanguage,
    instances: List<InstalledInstance> = emptyList(),
    onCreateInstanceClick: () -> Unit = {},
    onInstanceClick: (InstalledInstance) -> Unit = {},
    onPlayInstance: (InstalledInstance) -> Unit = {},
    onRenameInstance: (InstalledInstance, String) -> Unit = { _, _ -> },
    onDeleteInstance: (InstalledInstance) -> Unit = {},
    launchStatus: LaunchStatus = LaunchStatus.Idle,
    launchingInstanceId: String? = null,
    runningInstanceId: String? = null,
    onDismissLaunchError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRu = currentLanguage == AppLanguage.RU

    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            fadeIn(tween(200)).togetherWith(fadeOut(tween(150)))
        },
        modifier = modifier
    ) { tab ->
        if (tab == LauncherTab.INSTANCES) {
            InstancesTabContent(
                instances = instances,
                onCreateInstanceClick = onCreateInstanceClick,
                onInstanceClick = onInstanceClick,
                onPlayInstance = onPlayInstance,
                onRenameInstance = onRenameInstance,
                onDeleteInstance = onDeleteInstance,
                launchStatus = launchStatus,
                launchingInstanceId = launchingInstanceId,
                runningInstanceId = runningInstanceId,
                onDismissLaunchError = onDismissLaunchError,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioTheme.SurfaceCardDark)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRu) tab.titleRu else tab.titleEn,
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = StudioTheme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu) "Раздел находится в разработке" else "This section is under development",
                        fontFamily = GoogleSansFontFamily,
                        fontSize = 13.sp,
                        color = StudioTheme.TextMuted
                    )
                }
            }
        }
    }
}
