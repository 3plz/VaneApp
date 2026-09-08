package app.domain.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Handles Microsoft Device Authorization Grant flow for Minecraft accounts.
 * Uses community-allowlisted Client ID until custom Azure app registration is reviewed by Mojang.
 */
object MicrosoftDeviceCodeAuthHelper {

    const val CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb"

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Initiates Device Code OAuth flow, copies user code to clipboard, opens browser with ?otc=,
     * and polls for completion.
     */
    suspend fun startDeviceCodeFlow(
        onCodeReady: (userCode: String, verificationUri: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Request device code from Microsoft OAuth endpoint
            val requestBody = "client_id=$CLIENT_ID&scope=XboxLive.signin%20offline_access"
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                return@withContext Result.failure(Exception("Device code request failed (${response.statusCode()}): ${response.body()}"))
            }

            val respJson = json.parseToJsonElement(response.body()).jsonObject
            val deviceCode = respJson["device_code"]?.jsonPrimitive?.content ?: error("Missing device_code")
            val userCode = respJson["user_code"]?.jsonPrimitive?.content ?: error("Missing user_code")
            val rawVerificationUri = respJson["verification_uri"]?.jsonPrimitive?.content ?: "https://www.microsoft.com/link"
            val interval = respJson["interval"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5

            // Auto-fill URL with user code pre-populated
            val autoFillUri = "$rawVerificationUri?otc=$userCode"

            // Copy code to system clipboard
            try {
                val selection = StringSelection(userCode)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                println("[Clipboard] Authorization code $userCode copied to system clipboard.")
            } catch (_: Exception) {}

            // Open user's default browser
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(autoFillUri))
                } else {
                    Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", autoFillUri))
                }
            } catch (_: Exception) {}

            onCodeReady(userCode, autoFillUri)

            // 2. Poll token endpoint until user approves in browser
            val tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
            val pollBody = "client_id=$CLIENT_ID" +
                    "&grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                    "&device_code=$deviceCode"

            val pollDelay = (interval.coerceAtLeast(3)) * 1000L

            while (true) {
                delay(pollDelay)

                val pollReq = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(pollBody))
                    .build()

                val pollResp = httpClient.send(pollReq, HttpResponse.BodyHandlers.ofString())
                val pollJson = json.parseToJsonElement(pollResp.body()).jsonObject

                if (pollResp.statusCode() in 200..299) {
                    val msAccessToken = pollJson["access_token"]?.jsonPrimitive?.content
                        ?: error("Missing access_token in token response")
                    return@withContext Result.success(msAccessToken)
                }

                val err = pollJson["error"]?.jsonPrimitive?.content
                if (err == "authorization_pending") {
                    // Still waiting for user confirmation in browser
                    continue
                } else if (err == "slow_down") {
                    delay(5000)
                    continue
                } else {
                    return@withContext Result.failure(Exception("Microsoft authorization error: ${pollResp.body()}"))
                }
            }

            @Suppress("UNREACHABLE_CODE")
            Result.failure(Exception("Authentication timed out"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
