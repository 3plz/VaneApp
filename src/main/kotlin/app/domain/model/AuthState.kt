package app.domain.model

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticating(
        val progressMessage: String? = null,
        val userCode: String? = null
    ) : AuthState
    data class Authenticated(val session: UserSession) : AuthState
    data class Error(val message: String) : AuthState
}
