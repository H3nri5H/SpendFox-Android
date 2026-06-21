package de.h3nri5h.spendfox.data.supabase

import de.h3nri5h.spendfox.data.SpendFoxSnapshot
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class SupabaseAuthResponse(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

class SupabaseRestClient(
    private val supabaseUrl: String,
    private val supabaseKey: String
) {
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()

    fun signUp(email: String, password: String): SupabaseAuthResponse {
        return authRequest(
            path = "/auth/v1/signup",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
    }

    fun verifySignupOtp(email: String, token: String): SupabaseAuthResponse {
        return authRequest(
            path = "/auth/v1/verify",
            body = JSONObject()
                .put("email", email)
                .put("token", token)
                .put("type", "email")
        )
    }

    fun resendSignupOtp(email: String) {
        request(
            method = "POST",
            path = "/auth/v1/resend",
            body = JSONObject()
                .put("email", email)
                .put("type", "signup")
                .toString(),
            accessToken = null
        )
    }

    fun signIn(email: String, password: String): SupabaseAuthResponse {
        return authRequest(
            path = "/auth/v1/token?grant_type=password",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
    }

    fun recoverPassword(email: String) {
        request(
            method = "POST",
            path = "/auth/v1/recover",
            body = JSONObject().put("email", email).toString(),
            accessToken = null
        )
    }

    fun signOut(accessToken: String?) {
        if (accessToken.isNullOrBlank()) return
        runCatching {
            request(method = "POST", path = "/auth/v1/logout", body = "{}", accessToken = accessToken)
        }
    }

    fun upsert(table: String, payload: JSONObject, accessToken: String?) {
        if (!isConfigured || accessToken.isNullOrBlank()) return
        request(
            method = "POST",
            path = "/rest/v1/$table?on_conflict=id",
            body = payload.toString(),
            accessToken = accessToken,
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    fun fetchSnapshot(userId: String, accessToken: String?): SpendFoxSnapshot {
        if (!isConfigured) throw IOException("Supabase ist noch nicht konfiguriert.")
        if (accessToken.isNullOrBlank()) throw IOException("Keine aktive Supabase-Session.")
        if (userId.isBlank()) throw IOException("Keine Nutzerkennung für Supabase-Abfrage.")
        return SpendFoxSnapshot(
            expenses = fetchRows("expenses", userId, accessToken).map { it.toExpenseModel() },
            products = fetchRows("products", userId, accessToken).map { it.toProductModel() },
            vehicles = fetchRows("vehicles", userId, accessToken).map { it.toVehicleModel() },
            trips = fetchRows("trips", userId, accessToken).map { it.toTripModel() },
            fuelEntries = fetchRows("fuel_entries", userId, accessToken).map { it.toFuelEntryModel() },
            maintenanceItems = fetchRows("maintenance_items", userId, accessToken).map { it.toMaintenanceItemModel() },
            categories = fetchRows("categories", userId, accessToken).map { it.toUserCategoryModel() },
            profile = fetchRows("user_profiles", userId, accessToken, includeDeletedFilter = false)
                .firstOrNull()
                ?.toUserProfileModel()
        )
    }

    private fun authRequest(path: String, body: JSONObject): SupabaseAuthResponse {
        val json = JSONObject(request(method = "POST", path = path, body = body.toString(), accessToken = null))
        val user = json.optJSONObject("user")
        val userId = user?.optString("id").orEmpty()
        val email = user?.optString("email").orEmpty()
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        if (userId.isBlank()) {
            throw IOException("Supabase hat keinen Nutzer zurückgegeben.")
        }
        return SupabaseAuthResponse(
            userId = userId,
            email = email.ifBlank { body.optString("email") },
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun fetchRows(
        table: String,
        userId: String,
        accessToken: String,
        includeDeletedFilter: Boolean = true
    ): List<JSONObject> {
        val deletedFilter = if (includeDeletedFilter) "&deleted_at=is.null" else ""
        val response = request(
            method = "GET",
            path = "/rest/v1/$table?select=*&user_id=eq.${urlEncode(userId)}$deletedFilter&order=updated_at.desc",
            body = "",
            accessToken = accessToken
        )
        val rows = JSONArray(response)
        return (0 until rows.length()).map { index -> rows.getJSONObject(index) }
    }

    private fun request(
        method: String,
        path: String,
        body: String,
        accessToken: String?,
        prefer: String? = null
    ): String {
        check(isConfigured) { "Supabase ist noch nicht konfiguriert." }
        val connection = (URL(supabaseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doInput = true
            doOutput = method != "GET"
            setRequestProperty("apikey", supabaseKey)
            setRequestProperty("Authorization", "Bearer ${accessToken.takeUnless { it.isNullOrBlank() } ?: supabaseKey}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
        }
        if (connection.doOutput) {
            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }
        }
        val status = connection.responseCode
        val response = runCatching {
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        if (status !in 200..299) {
            val message = runCatching { JSONObject(response).optString("message") }.getOrNull().orEmpty()
            throw IOException(message.ifBlank { "Supabase Anfrage fehlgeschlagen ($status)." })
        }
        return response
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
