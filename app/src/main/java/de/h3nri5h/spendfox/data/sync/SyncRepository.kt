package de.h3nri5h.spendfox.data.sync

import de.h3nri5h.spendfox.data.SyncState
import de.h3nri5h.spendfox.data.supabase.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class SyncStatus(
    val isConfigured: Boolean,
    val isSyncing: Boolean = false,
    val message: String = if (isConfigured) "Supabase verbunden" else "Supabase noch nicht konfiguriert"
)

class SyncRepository(
    supabaseUrl: String,
    supabaseKey: String,
    private val accessTokenProvider: () -> String?
) {
    private val supabase = SupabaseRestClient(supabaseUrl, supabaseKey)
    private val _status = MutableStateFlow(SyncStatus(isConfigured = supabase.isConfigured))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun pendingState(): SyncState = SyncState.PendingUpsert

    suspend fun upsert(table: String, payload: JSONObject) {
        if (!supabase.isConfigured) {
            _status.value = SyncStatus(isConfigured = false, message = "Supabase noch nicht konfiguriert")
            return
        }
        val token = accessTokenProvider()
        if (token.isNullOrBlank()) {
            _status.value = SyncStatus(isConfigured = true, message = "Angemeldet, aber noch ohne Supabase-Session")
            return
        }
        _status.value = SyncStatus(isConfigured = true, isSyncing = true, message = "Sync läuft")
        runCatching { supabase.upsert(table, payload, token) }
            .onSuccess {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Mit Supabase synchronisiert")
            }
            .onFailure {
                _status.value = SyncStatus(isConfigured = true, isSyncing = false, message = "Lokale Änderung wartet auf Sync")
            }
    }

    suspend fun syncNow() {
        _status.value = SyncStatus(
            isConfigured = supabase.isConfigured,
            isSyncing = false,
            message = if (supabase.isConfigured) "Supabase-Verbindung bereit" else "Supabase URL und Publishable Key fehlen"
        )
    }
}
