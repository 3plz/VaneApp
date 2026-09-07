package app.domain.auth

import app.domain.model.AuthState
import app.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthService {
    val authState: StateFlow<AuthState>

    suspend fun login(): Result<UserSession>
    suspend fun logout()
    suspend fun restoreSession(): Boolean
}
