package de.h3nri5h.spendfox.data.sync

import de.h3nri5h.spendfox.data.SpendFoxSnapshot
import de.h3nri5h.spendfox.data.supabase.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class SyncStatus(
    val isConfigured: Boolean,
    val isSyncing: Boolean = false,
    val message: String = if (isConfigured) "Supabase verbunden" else "Supabase erforderlich"
)

class SyncRepository(
    supabaseUrl: String,
    supabaseKey: String,
    private val accessTokenProvider: () -> String?
) {
    private val supabase = SupabaseRestClient(supabaseUrl, supabaseKey)
    private val _status = MutableStateFlow(SyncStatus(isConfigured = supabase.isConfigured))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun upsert(table: String, payload: JSONObject): Boolean {
        if (!supabase.isConfigured) {
            _status.value = SyncStatus(isConfigured = false, message = "Supabase URL und Publishable Key fehlen")
            return false
        }
        val token = accessTokenProvider()
        if (token.isNullOrBlank()) {
            _status.value = SyncStatus(isConfigured = true, message = "Angemeldet, aber noch ohne Supabase-Session")
            return false
        }
        _status.value = SyncStatus(isConfigured = true, isSyncing = true, message = "Sync läuft")
        return runCatching { supabase.upsert(table, payload, token) }
            .onSuccess {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Mit Supabase synchronisiert")
            }
            .onFailure {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Supabase-Speichern fehlgeschlagen")
            }
            .isSuccess
    }

    suspend fun syncNow() {
        _status.value = SyncStatus(
            isConfigured = supabase.isConfigured,
            isSyncing = false,
            message = if (supabase.isConfigured) "Supabase-Verbindung bereit" else "Supabase URL und Publishable Key fehlen"
        )
    }

    suspend fun fetchSnapshot(userId: String): Result<SpendFoxSnapshot> {
        if (!supabase.isConfigured) {
            _status.value = SyncStatus(isConfigured = false, message = "Supabase URL und Publishable Key fehlen")
            return Result.failure(IllegalStateException("Supabase ist nicht konfiguriert."))
        }
        val token = accessTokenProvider()
        if (token.isNullOrBlank()) {
            _status.value = SyncStatus(isConfigured = true, message = "Angemeldet, aber noch ohne Supabase-Session")
            return Result.failure(IllegalStateException("Keine aktive Supabase-Session."))
        }
        _status.value = SyncStatus(isConfigured = true, isSyncing = true, message = "Daten werden aus Supabase geladen")
        return runCatching { supabase.fetchSnapshot(userId, token) }
            .onSuccess {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Aus Supabase geladen")
            }
            .onFailure {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Supabase-Laden fehlgeschlagen")
            }
    }
}
