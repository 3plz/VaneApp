package app.domain.storage

import app.domain.model.Account
import app.domain.model.AccountType
import app.domain.model.AccountsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Manages local persistence and active state for Minecraft accounts.
 */
class AccountRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _accountsData = MutableStateFlow(AccountsData())
    val accountsData: StateFlow<AccountsData> = _accountsData.asStateFlow()

    val hasMicrosoftAccount: StateFlow<Boolean> = _accountsData
        .map { data -> data.accounts.any { it.type == AccountType.MICROSOFT } }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            _accountsData.value = readAccountsDataFromDisk()
        }
    }

    suspend fun loadAccountsData(): AccountsData = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAccountsDataFromDisk().also {
                _accountsData.value = it
            }
        }
    }

    suspend fun saveAccount(account: Account, makeActive: Boolean = true): AccountsData = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readAccountsDataFromDisk()
            val updatedList = current.accounts.filterNot { it.id == account.id } + account
            val updatedActive = if (makeActive || current.activeAccountId == null) account.id else current.activeAccountId
            val newData = AccountsData(activeAccountId = updatedActive, accounts = updatedList)

            writeAccountsDataInternal(newData)
            _accountsData.value = newData
            newData
        }
    }

    suspend fun getActiveAccount(): Account? = withContext(Dispatchers.IO) {
        val data = loadAccountsData()
        val activeId = data.activeAccountId ?: return@withContext data.accounts.firstOrNull()
        data.accounts.find { it.id == activeId } ?: data.accounts.firstOrNull()
    }

    suspend fun setActiveAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val data = readAccountsDataFromDisk()
            if (data.accounts.none { it.id == accountId }) return@withLock false

            val updated = data.copy(activeAccountId = accountId)
            writeAccountsDataInternal(updated)
            _accountsData.value = updated
            true
        }
    }

    suspend fun removeAccount(accountId: String): AccountsData = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readAccountsDataFromDisk()
            val updatedList = current.accounts.filterNot { it.id == accountId }
            val updatedActive = if (current.activeAccountId == accountId) {
                updatedList.firstOrNull()?.id
            } else {
                current.activeAccountId
            }

            val updated = AccountsData(activeAccountId = updatedActive, accounts = updatedList)
            writeAccountsDataInternal(updated)
            _accountsData.value = updated
            updated
        }
    }

    suspend fun hasMicrosoftAccount(): Boolean = withContext(Dispatchers.IO) {
        loadAccountsData().accounts.any { it.type == AccountType.MICROSOFT }
    }

    suspend fun hasAnyAccount(): Boolean = withContext(Dispatchers.IO) {
        loadAccountsData().accounts.isNotEmpty()
    }

    private fun readAccountsDataFromDisk(): AccountsData {
        val file = AppPaths.accountsFile
        if (!Files.exists(file)) {
            return AccountsData()
        }
        return try {
            val content = Files.readString(file, StandardCharsets.UTF_8)
            json.decodeFromString<AccountsData>(content)
        } catch (e: Exception) {
            println("[WARN] Failed to parse accounts.json: ${e.message}")
            AccountsData()
        }
    }

    private fun writeAccountsDataInternal(data: AccountsData) {
        val target = AppPaths.accountsFile
        val temp = AppPaths.appDataDir.resolve("accounts.json.tmp")
        val content = json.encodeToString(data)

        Files.writeString(temp, content, StandardCharsets.UTF_8)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
