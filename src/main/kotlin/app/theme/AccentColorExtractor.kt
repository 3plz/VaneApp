package app.theme

import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image as SkiaImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Extracts vibrant dominant accent colors from instance logos to dynamically theme
 * banners, borders, and UI accents matching the modpack identity.
 */
object AccentColorExtractor {
    val DefaultFallbackColor: Color = Color(0xFF6366F1)

    private val cache = ConcurrentHashMap<String, Color>()

    /**
     * Extracts an accent color from [iconFile].
     * If file doesn't exist, is invalid, or monochrome, returns [fallback].
     */
    fun extractAccentColor(iconFile: File?, fallback: Color = DefaultFallbackColor): Color {
        if (iconFile == null || !iconFile.exists() || !iconFile.isFile) return fallback

        val cacheKey = "${iconFile.absolutePath}_${iconFile.lastModified()}"
        cache[cacheKey]?.let { return it }

        val extracted = runCatching {
            val bytes = Files.readAllBytes(iconFile.toPath())
            extractFromBytes(bytes)
        }.getOrNull() ?: fallback

        val finalColor = adjustForDarkTheme(extracted)
        cache[cacheKey] = finalColor
        return finalColor
    }

    /**
     * Analyzes image bytes and returns the most prominent vibrant chromatic color.
     */
    @Suppress("DEPRECATION")
    fun extractFromBytes(bytes: ByteArray): Color? {
        val pixels = runCatching {
            val skiaImage = SkiaImage.makeFromEncoded(bytes)
            val bitmap = Bitmap.makeFromImage(skiaImage)
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return@runCatching null

            // Sample down to a max 32x32 grid for fast processing (< 1ms)
            val stepX = (width / 32).coerceAtLeast(1)
            val stepY = (height / 32).coerceAtLeast(1)
            val sampled = ArrayList<Int>((width / stepX + 1) * (height / stepY + 1))
            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    sampled.add(bitmap.getColor(x, y))
                }
            }
            sampled
        }.getOrNull() ?: runCatching {
            val img = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching null
            val width = img.width
            val height = img.height
            val stepX = (width / 32).coerceAtLeast(1)
            val stepY = (height / 32).coerceAtLeast(1)
            val sampled = ArrayList<Int>((width / stepX + 1) * (height / stepY + 1))
            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    sampled.add(img.getRGB(x, y))
                }
            }
            sampled
        }.getOrNull() ?: return null

        return findDominantVibrantColor(pixels)
    }

    private class ColorBucket {
        var count = 0
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var score = 0.0f
    }

    private fun findDominantVibrantColor(pixels: List<Int>): Color? {
        // 18 hue buckets (each covers 20 degrees of color wheel: 0..360)
        val buckets = Array(18) { ColorBucket() }
        var chromaticPixels = 0

        for (pixel in pixels) {
            val a = (pixel ushr 24) and 0xFF
            if (a < 110) continue // Skip transparent / semi-transparent padding

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            val maxVal = max(r, max(g, b))
            val minVal = min(r, min(g, b))
            val delta = maxVal - minVal

            if (maxVal == 0) continue

            val saturation = delta.toFloat() / maxVal.toFloat()
            val value = maxVal.toFloat() / 255.0f

            // Filter out neutral grays, pure blacks, and washed out whites
            if (saturation < 0.18f || value < 0.16f || (value > 0.95f && saturation < 0.20f)) {
                continue
            }

            // Calculate Hue in degrees [0, 360)
            var hue = when {
                delta == 0 -> 0f
                maxVal == r -> 60f * (((g - b).toFloat() / delta) % 6f)
                maxVal == g -> 60f * (((b - r).toFloat() / delta) + 2f)
                else -> 60f * (((r - g).toFloat() / delta) + 4f)
            }
            if (hue < 0f) hue += 360f

            val bucketIndex = ((hue / 20f).toInt()).coerceIn(0, 17)
            val bucket = buckets[bucketIndex]

            bucket.count++
            bucket.sumR += r
            bucket.sumG += g
            bucket.sumB += b

            // Score: higher saturation and comfortable brightness (0.35..0.90) get prioritized
            val brightnessFactor = if (value in 0.35f..0.88f) 1.25f else 0.85f
            bucket.score += (saturation * saturation) * brightnessFactor
            chromaticPixels++
        }

        if (chromaticPixels == 0) {
            return null // No vibrant colors found; let caller use fallback theme
        }

        val bestBucket = buckets.maxByOrNull { it.score } ?: return null
        if (bestBucket.count == 0) return null

        val avgR = (bestBucket.sumR / bestBucket.count).toInt().coerceIn(0, 255)
        val avgG = (bestBucket.sumG / bestBucket.count).toInt().coerceIn(0, 255)
        val avgB = (bestBucket.sumB / bestBucket.count).toInt().coerceIn(0, 255)

        return Color(avgR, avgG, avgB)
    }

    /**
     * Adjusts the extracted color so that it looks vibrant and readable on dark UI surfaces.
     * Prevents muddy or near-invisible dark accents by lifting luminance and saturation if needed.
     */
    fun adjustForDarkTheme(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue

        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        val delta = maxVal - minVal

        if (maxVal == 0f || delta == 0f) {
            return DefaultFallbackColor
        }

        var hue = when {
            maxVal == r -> 60f * (((g - b) / delta) % 6f)
            maxVal == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (hue < 0f) hue += 360f

        var saturation = delta / maxVal
        var value = maxVal

        // Ensure pleasing saturation (minimum 0.55 so it has strong identity)
        if (saturation < 0.55f) {
            saturation = 0.55f
        }

        // Ensure pleasing brightness on dark theme (minimum 0.65, max 0.95)
        if (value < 0.65f) {
            value = 0.65f
        } else if (value > 0.95f) {
            value = 0.95f
        }

        return hsvToColor(hue, saturation, value)
    }

    private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val c = value * saturation
        val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
        val m = value - c

        val (rPrime, gPrime, bPrime) = when ((hue / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return Color(r, g, b)
    }
}
