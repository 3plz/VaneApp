package app.domain.model

import kotlinx.serialization.Serializable

/**
 * Supported account authentication types.
 */
@Serializable
enum class AccountType {
    MICROSOFT,
    OFFLINE
}

/**
 * Represents a saved user account persisted locally.
 */
@Serializable
data class Account(
    val id: String,
    val username: String,
    val type: AccountType,
    val accessToken: String,
    val refreshToken: String? = null,
    val skinUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    fun toUserSession(): UserSession = UserSession(
        userId = id,
        username = username,
        accessToken = accessToken,
        skinUrl = skinUrl,
        loggedInAt = lastUsedAt,
        isOffline = type == AccountType.OFFLINE
    )
}

/**
 * Top-level structure for accounts.json.
 */
@Serializable
data class AccountsData(
    val activeAccountId: String? = null,
    val accounts: List<Account> = emptyList()
)
