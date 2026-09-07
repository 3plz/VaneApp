package app.domain.auth

import app.domain.model.AuthState
import app.domain.model.UserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Implements Microsoft OAuth 2.0, Xbox Live (XBL), Xbox Security Token Service (XSTS),
 * and Minecraft Services authentication pipeline.
 */
class MicrosoftAuthService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthService {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun login(): Result<UserSession> = withContext(ioDispatcher) {
        try {
            println("\n" + "=".repeat(60))
            println("▶ [1/5] Starting WickedApp Microsoft OAuth2 Flow...")
            println("=".repeat(60))
            _authState.value = AuthState.Authenticating("Awaiting browser authorization...")

            // Step 1: Acquire authorization code via local loopback HTTP listener
            val codeResult = MicrosoftOAuthBrowserHelper.acquireAuthorizationCode()
            val code = codeResult.getOrThrow()
            println("[OK] [1/5] Authorization code received from browser!")

            _authState.value = AuthState.Authenticating("Exchanging token...")
            println("\n▶ [2/5] Exchanging code for Microsoft OAuth Access Token...")

            // Step 2: Exchange authorization code for Microsoft OAuth access token
            val msTokenBody = listOf(
                "client_id" to MicrosoftOAuthBrowserHelper.CLIENT_ID,
                "code" to code,
                "grant_type" to "authorization_code",
                "redirect_uri" to MicrosoftOAuthBrowserHelper.REDIRECT_URI
            ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

            val msTokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(msTokenBody))
                .build()

            val msTokenResponse = httpClient.send(msTokenRequest, HttpResponse.BodyHandlers.ofString())
            if (msTokenResponse.statusCode() !in 200..299) {
                error("Microsoft Token Error (${msTokenResponse.statusCode()}): ${msTokenResponse.body()}")
            }

            val msJson = json.parseToJsonElement(msTokenResponse.body()).jsonObject
            val msAccessToken = msJson["access_token"]?.jsonPrimitive?.content
                ?: error("No access_token in Microsoft response")

            println("[OK] [2/5] Microsoft Access Token acquired!")

            // Step 3: Authenticate with Xbox Live (XBL)
            _authState.value = AuthState.Authenticating("Xbox Live...")
            println("\n▶ [3/5] Authenticating with Xbox Live (user.auth.xboxlive.com)...")

            val xblRequestBody = buildJsonObject {
                putJsonObject("Properties") {
                    put("AuthMethod", "RPS")
                    put("SiteName", "user.auth.xboxlive.com")
                    put("RpsTicket", "d=$msAccessToken")
                }
                put("RelyingParty", "http://auth.xboxlive.com")
                put("TokenType", "JWT")
            }.toString()

            val xblRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(xblRequestBody))
                .build()

            val xblResponse = httpClient.send(xblRequest, HttpResponse.BodyHandlers.ofString())
            if (xblResponse.statusCode() !in 200..299) {
                error("Xbox Live Error (${xblResponse.statusCode()}): ${xblResponse.body()}")
            }

            val xblJson = json.parseToJsonElement(xblResponse.body()).jsonObject
            val xblToken = xblJson["Token"]?.jsonPrimitive?.content
                ?: error("No Token in Xbox Live response")
            val userHash = xblJson["DisplayClaims"]?.jsonObject
                ?.get("xui")?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("uhs")?.jsonPrimitive?.content
                ?: error("No userHash (uhs) in Xbox Live response")

            println("[OK] [3/5] Xbox Live Token & UserHash ($userHash) acquired!")

            // Step 4: Acquire Xbox Security Token Service (XSTS) token
            _authState.value = AuthState.Authenticating("XSTS Authorization...")
            println("\n▶ [4/5] Requesting XSTS Token (xsts.auth.xboxlive.com)...")

            val xstsRequestBody = buildJsonObject {
                putJsonObject("Properties") {
                    put("SandboxId", "RETAIL")
                    putJsonArray("UserTokens") {
                        add(xblToken)
                    }
                }
                put("RelyingParty", "rp://api.minecraftservices.com/")
                put("TokenType", "JWT")
            }.toString()

            val xstsRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(xstsRequestBody))
                .build()

            val xstsResponse = httpClient.send(xstsRequest, HttpResponse.BodyHandlers.ofString())
            if (xstsResponse.statusCode() !in 200..299) {
                val errorBody = xstsResponse.body()
                println("[ERROR] XSTS Error: $errorBody")
                if (errorBody.contains("2148916238")) {
                    error("Child account: requires Xbox family consent.")
                } else if (errorBody.contains("2148916233")) {
                    error("Account does not have an active Xbox Live profile.")
                }
                error("XSTS Error (${xstsResponse.statusCode()}): $errorBody")
            }

            val xstsJson = json.parseToJsonElement(xstsResponse.body()).jsonObject
            val xstsToken = xstsJson["Token"]?.jsonPrimitive?.content
                ?: error("No Token in XSTS response")

            println("[OK] [4/5] XSTS Token acquired!")

            // Step 5: Authenticate with Mojang Minecraft Services and obtain profile
            _authState.value = AuthState.Authenticating("Minecraft Services...")
            println("\n▶ [5/5] Logging into Minecraft Services (api.minecraftservices.com)...")

            val mcLoginBody = buildJsonObject {
                put("identityToken", "XBL3.0 x=$userHash;$xstsToken")
            }.toString()

            val mcLoginRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcLoginBody))
                .build()

            val mcLoginResponse = httpClient.send(mcLoginRequest, HttpResponse.BodyHandlers.ofString())
            if (mcLoginResponse.statusCode() !in 200..299) {
                val errBody = mcLoginResponse.body()
                if (errBody.contains("Invalid app registration")) {
                    println("\n" + "!".repeat(60))
                    println("[NOTICE] WickedApp application approval is pending review by Mojang.")
                    println("Form URL: https://aka.ms/AppRegInfo")
                    println("!".repeat(60) + "\n")
                }
                error("Minecraft Services Error (${mcLoginResponse.statusCode()}): $errBody")
            }

            val mcLoginJson = json.parseToJsonElement(mcLoginResponse.body()).jsonObject
            val mcAccessToken = mcLoginJson["access_token"]?.jsonPrimitive?.content
                ?: error("No access_token in Minecraft login response")

            println("[OK] [5/5] Minecraft Services Bearer Token acquired!")

            // Retrieve player's Minecraft profile (UUID, username, and active skin)
            val profileRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer $mcAccessToken")
                .GET()
                .build()

            val profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString())

            val session = if (profileResponse.statusCode() in 200..299) {
                val profileJson = json.parseToJsonElement(profileResponse.body()).jsonObject
                val playerUuid = profileJson["id"]?.jsonPrimitive?.content ?: "unknown-uuid"
                val playerName = profileJson["name"]?.jsonPrimitive?.content ?: "Player"
                val skinUrl = profileJson["skins"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

                UserSession(
                    userId = playerUuid,
                    username = playerName,
                    accessToken = mcAccessToken,
                    skinUrl = skinUrl
                )
            } else {
                println("[WARN] No Minecraft Java profile found (${profileResponse.statusCode()})")
                UserSession(
                    userId = userHash,
                    username = "XboxPlayer_$userHash",
                    accessToken = mcAccessToken
                )
            }

            // Output debug session info to console
            println("\n" + "=".repeat(60))
            println(">>> [SUCCESS] MINECRAFT AUTHENTICATION COMPLETED! <<<")
            println("=".repeat(60))
            println("Player Name (Username) : ${session.username}")
            println("Player UUID            : ${session.userId}")
            println("Skin Texture URL       : ${session.skinUrl ?: "Default (Steve/Alex)"}")
            println("Minecraft Bearer Token : ${session.accessToken.take(30)}...[TRUNCATED]")
            println("Session Timestamp      : ${java.time.LocalDateTime.now()}")
            println("=".repeat(60) + "\n")

            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown authentication error"
            println("\n[ERROR] Authentication failed: $errorMsg")
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun restoreSession(): Boolean = withContext(ioDispatcher) {
        false
    }
}
