package app.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import app.domain.model.Account
import app.domain.model.AccountType
import app.domain.model.UserSession
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import app.ui.components.PlayerAvatar

/**
 * Profile dropdown selector showing active user account and listing all registered accounts on click.
 */
@Suppress("DEPRECATION")
@Composable
fun AccountSelectorDropdown(
    currentSession: UserSession,
    accounts: List<Account>,
    onSelectAccount: (Account) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200)
    )

    val pillInteraction = remember { MutableInteractionSource() }
    val isPillHovered by pillInteraction.collectIsHoveredAsState()

    val pillBg by animateColorAsState(
        targetValue = if (isPillHovered || expanded) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark,
        animationSpec = tween(150)
    )

    Box(modifier = modifier) {
        // Active Profile Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(pillBg)
                .border(
                    1.dp,
                    if (expanded) StudioTheme.PrimaryIndigo else StudioTheme.BorderDark,
                    RoundedCornerShape(24.dp)
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = pillInteraction,
                    indication = ripple(color = StudioTheme.TextPrimary.copy(alpha = 0.15f)),
                    onClick = { expanded = !expanded }
                )
                .padding(start = 6.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
        ) {
            PlayerAvatar(
                username = currentSession.username,
                size = 32.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = currentSession.username,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = StudioTheme.TextPrimary
                )
                Text(
                    text = if (currentSession.accessToken.startsWith("offline_")) "Offline Profile" else "Microsoft Account",
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 11.sp,
                    color = if (currentSession.accessToken.startsWith("offline_")) StudioTheme.TextMuted else Color(0xFF34D399)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = StudioTheme.TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }

        // Popup with all saved accounts
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 52),
                onDismissRequest = { expanded = false }
            ) {
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp)),
                    color = StudioTheme.SurfaceCardDark,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        // All accounts list
                        accounts.forEach { account ->
                            val isCurrent = account.username.equals(currentSession.username, ignoreCase = true)
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemHovered by itemInteraction.collectIsHoveredAsState()

                            val itemBg by animateColorAsState(
                                targetValue = when {
                                    isItemHovered -> StudioTheme.HoverDark
                                    isCurrent -> StudioTheme.SurfaceContainerDark
                                    else -> Color.Transparent
                                },
                                animationSpec = tween(120)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(itemBg)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = ripple(color = StudioTheme.TextPrimary.copy(alpha = 0.1f)),
                                        onClick = {
                                            if (!isCurrent) {
                                                onSelectAccount(account)
                                            }
                                            expanded = false
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(
                                    username = account.username,
                                    size = 28.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.username,
                                        fontFamily = GoogleSansFontFamily,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isCurrent) StudioTheme.TextPrimary else StudioTheme.TextSecondary
                                    )
                                    Text(
                                        text = if (account.type == AccountType.MICROSOFT) "Microsoft" else "Offline",
                                        fontFamily = GoogleSansFontFamily,
                                        fontSize = 10.5.sp,
                                        color = if (account.type == AccountType.MICROSOFT) Color(0xFF34D399) else StudioTheme.TextMuted
                                    )
                                }

                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else if (accounts.size > 1) {
                                    // Remove account icon
                                    val deleteInteraction = remember { MutableInteractionSource() }
                                    val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isDeleteHovered) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable(
                                                interactionSource = deleteInteraction,
                                                indication = ripple(color = Color(0xFFEF4444)),
                                                onClick = {
                                                    onRemoveAccount(account.id)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = if (isDeleteHovered) Color(0xFFEF4444) else StudioTheme.TextMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = StudioTheme.BorderDark,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Add Account action row
                        val addInteraction = remember { MutableInteractionSource() }
                        val isAddHovered by addInteraction.collectIsHoveredAsState()

                        val addBg by animateColorAsState(
                            targetValue = if (isAddHovered) StudioTheme.HoverDark else Color.Transparent,
                            animationSpec = tween(120)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(addBg)
                                .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = addInteraction,
                                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.15f)),
                                onClick = {
                                    expanded = false
                                    onAddNewAccount()
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.addAccount,
                                    tint = StudioTheme.PrimaryIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = strings.addAccount,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = StudioTheme.PrimaryIndigo
                            )
                        }
                    }
                }
            }
        }
    }
}
