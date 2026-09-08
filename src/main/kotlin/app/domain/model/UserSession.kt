package app.domain.model

data class UserSession(
    val userId: String,
    val username: String,
    val accessToken: String,
    val skinUrl: String? = null,
    val loggedInAt: Long = System.currentTimeMillis(),
    val isOffline: Boolean = false
)
