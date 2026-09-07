package app.domain.auth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Handles the browser-based OAuth 2.0 loopback flow for WickedApp.
 * Spins up an ephemeral local HTTP server on port 28456 to intercept the authorization code.
 */
object MicrosoftOAuthBrowserHelper {

    private const val PORT = 28456
    private const val REDIRECT_PATH = "/auth"
    const val REDIRECT_URI = "http://localhost:$PORT$REDIRECT_PATH"

    // Official Application (Client) ID for WickedApp (Author: 3plz)
    const val CLIENT_ID = "a65847a9-fd27-4442-ad64-f29eeee6c0a3"

    /**
     * Starts a local loopback HTTP server, launches the user's default browser to Microsoft's
     * authorization endpoint, and awaits the returned authorization code.
     */
    suspend fun acquireAuthorizationCode(): Result<String> = withContext(Dispatchers.IO) {
        val authCodeDeferred = CompletableDeferred<Result<String>>()
        var server: HttpServer? = null

        try {
            server = HttpServer.create(InetSocketAddress("localhost", PORT), 0)

            server.createContext(REDIRECT_PATH) { exchange ->
                val query = exchange.requestURI.query ?: ""
                val params = query.split("&").associate { param ->
                    val parts = param.split("=")
                    val key = parts.getOrNull(0) ?: ""
                    val value = parts.getOrNull(1) ?: ""
                    key to value
                }

                val code = params["code"]
                val error = params["error"]
                val errorDescription = params["error_description"]

                val htmlResponse: String
                val statusCode: Int

                if (code != null) {
                    statusCode = 200
                    htmlResponse = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <title>WickedApp - Login Success</title>
                            <style>
                                body {
                                    background: #0B0D13;
                                    color: #F8FAFC;
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    height: 100vh;
                                    margin: 0;
                                }
                                .card {
                                    background: #191D2B;
                                    border: 1px solid #282D3C;
                                    padding: 36px 48px;
                                    border-radius: 16px;
                                    text-align: center;
                                    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                                    max-width: 420px;
                                }
                                .badge {
                                    background: rgba(52, 211, 153, 0.15);
                                    color: #34D399;
                                    font-size: 13px;
                                    font-weight: 600;
                                    padding: 6px 14px;
                                    border-radius: 20px;
                                    display: inline-block;
                                    margin-bottom: 16px;
                                }
                                h1 { margin: 0 0 10px; font-size: 22px; }
                                p { color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 0; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <div class="badge">&#10003; Success</div>
                                <h1>Authentication Complete!</h1>
                                <p>You have successfully authenticated with WickedApp.<br>You may now close this tab and return to the launcher.</p>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    authCodeDeferred.complete(Result.success(code))
                } else {
                    statusCode = 400
                    val msg = errorDescription ?: error ?: "User canceled login"
                    htmlResponse = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <title>WickedApp - Error</title>
                            <body style="background:#0B0D13;color:#F87171;font-family:sans-serif;padding:40px;text-align:center;">
                                <h2>Authentication Error: $msg</h2>
                            </body>
                        </html>
                    """.trimIndent()
                    authCodeDeferred.complete(Result.failure(Exception("Microsoft Auth Error: $msg")))
                }

                val bytes = htmlResponse.toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
                exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
                exchange.responseBody.write(bytes)
                exchange.responseBody.close()
            }

            server.executor = null
            server.start()

            val encodedScope = URLEncoder.encode("XboxLive.signin offline_access", "UTF-8")
            val encodedRedirect = URLEncoder.encode(REDIRECT_URI, "UTF-8")

            val loginUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize" +
                    "?client_id=$CLIENT_ID" +
                    "&response_type=code" +
                    "&redirect_uri=$encodedRedirect" +
                    "&scope=$encodedScope" +
                    "&prompt=select_account"

            println("\n[WickedApp] Opening Microsoft OAuth2 authorization in browser...")
            println("[WickedApp] URL: $loginUrl\n")

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(loginUrl))
            } else {
                val os = System.getProperty("os.name").lowercase()
                when {
                    "win" in os -> Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", loginUrl))
                    "mac" in os -> Runtime.getRuntime().exec(arrayOf("open", loginUrl))
                    else -> Runtime.getRuntime().exec(arrayOf("xdg-open", loginUrl))
                }
            }

            authCodeDeferred.await()
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            server?.stop(1)
        }
    }
}
