package app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.theme.StudioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

// In-memory cache for downloaded avatar heads to prevent redundant network calls
private val avatarHeadCache = ConcurrentHashMap<String, ImageBitmap>()

/**
 * Modern player avatar component that dynamically fetches the player's 3D/2D head
 * from https://mc-heads.net/avatar/{username} with debounce and in-memory cache.
 */
@Composable
fun PlayerAvatar(
    username: String,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val targetName = remember(username) {
        val trimmed = username.trim()
        if (trimmed.length in 3..16) trimmed else "MHF_Steve"
    }

    var bitmap by remember(targetName) {
        mutableStateOf(avatarHeadCache[targetName.lowercase()])
    }

    LaunchedEffect(targetName) {
        if (bitmap != null) return@LaunchedEffect

        // Debounce input (300ms) to avoid spamming the API while typing fast
        delay(300)

        val cached = avatarHeadCache[targetName.lowercase()]
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URI("https://mc-heads.net/avatar/$targetName/64").toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.setRequestProperty("User-Agent", "WickedApp-Minecraft-Launcher")
                connection.connect()

                if (connection.responseCode == 200) {
                    val stream: InputStream = connection.inputStream
                    val bytes = stream.readBytes()
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    val composeBitmap = skiaImage.toComposeImageBitmap()

                    avatarHeadCache[targetName.lowercase()] = composeBitmap
                    bitmap = composeBitmap
                }
            } catch (_: Exception) {
                // Network or API failure fallback: keep default avatar icon
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioTheme.SurfaceContainerDark),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = bitmap, animationSpec = tween(180)) { currentBitmap ->
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = "$targetName head",
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = StudioTheme.TextMuted,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
        }
    }
}
