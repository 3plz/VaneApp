package app.ui.login.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.AppLanguage
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

@Suppress("DEPRECATION")
@Composable
fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val langInteraction = remember { MutableInteractionSource() }
    val isLangHovered by langInteraction.collectIsHoveredAsState()

    val langBg by animateColorAsState(
        targetValue = if (isLangHovered || isMenuExpanded) Color(0xFF262B38) else Color(0xFF1E222E),
        animationSpec = tween(150)
    )
    val langBorder by animateColorAsState(
        targetValue = if (isLangHovered || isMenuExpanded) Color(0xFF434A5E) else Color(0xFF2E3342),
        animationSpec = tween(150)
    )
    val langTint by animateColorAsState(
        targetValue = if (isLangHovered || isMenuExpanded) Color.White else Color(0xFF9AA0A6),
        animationSpec = tween(150)
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(langBg)
                .border(1.dp, langBorder, RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = langInteraction,
                    indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                    onClick = { isMenuExpanded = !isMenuExpanded }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource("translate-bold-svgrepo-com.svg"),
                contentDescription = "Language",
                tint = langTint,
                modifier = Modifier.size(17.dp)
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp),
            modifier = Modifier
                .width(165.dp)
                .background(Color(0xFF1B1E28))
                .border(1.dp, Color(0xFF2E3342), RoundedCornerShape(10.dp))
                .padding(4.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = currentLanguage == lang
                val itemInteraction = remember { MutableInteractionSource() }
                val isItemHovered by itemInteraction.collectIsHoveredAsState()

                val itemBg by animateColorAsState(
                    targetValue = when {
                        isSelected -> Color(0xFF262B3A)
                        isItemHovered -> Color(0xFF222634)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(120)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(itemBg)
                        .clickable(
                            interactionSource = itemInteraction,
                            indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                            onClick = {
                                onLanguageSelected(lang)
                                isMenuExpanded = false
                            }
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang.code.uppercase(),
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF9AA0A6),
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF60A5FA).copy(alpha = 0.15f) else Color(0xFF282D3C),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = lang.title,
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = if (isSelected) Color.White else StudioTheme.TextPrimary
                    )

                    if (isSelected) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
