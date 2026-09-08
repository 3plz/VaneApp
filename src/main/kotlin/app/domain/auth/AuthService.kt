package app.domain.auth

import app.domain.model.AuthState
import app.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthService {
    val authState: StateFlow<AuthState>
    val hasMicrosoftAccount: StateFlow<Boolean>

    suspend fun login(): Result<UserSession>
    suspend fun loginOffline(username: String): Result<UserSession>
    suspend fun logout()
    suspend fun restoreSession(): Boolean
}
