package de.h3nri5h.spendfox.data.supabase

import de.h3nri5h.spendfox.data.CategoryScope
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelEntry
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.MaintenanceItem
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.SyncState
import de.h3nri5h.spendfox.data.Trip
import de.h3nri5h.spendfox.data.UserCategory
import de.h3nri5h.spendfox.data.UserProfile
import de.h3nri5h.spendfox.data.Vehicle
import org.json.JSONObject

fun Expense.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("amount_cents", amountCents)
    .put("merchant", merchant)
    .put("category", category.name)
    .put("occurred_at", occurredAtEpochMillis)
    .put("note", note)
    .put("custom_category", customCategory)
    .put("payment_account", paymentAccount)
    .put("booking_text", bookingText)
    .put("purpose", purpose)
    .put("tags", tags)
    .put("source_type", sourceType)
    .put("source_hash", sourceHash)
    .putNullable("imported_at", importedAtEpochMillis)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun Product.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("name", name)
    .put("manufacturer", manufacturer)
    .put("purchase_price_cents", purchasePriceCents)
    .put("purchased_at", purchasedAtEpochMillis)
    .put("category", category.name)
    .put("note", note)
    .put("custom_category", customCategory)
    .put("model_name", modelName)
    .put("serial_reference", serialReference)
    .put("usage_duration_months", usageDurationMonths)
    .putNullable("warranty_until", warrantyUntilEpochMillis)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun Vehicle.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("display_name", displayName)
    .put("manufacturer", manufacturer)
    .put("model_name", modelName)
    .put("license_plate", licensePlate)
    .put("fuel_type", fuelType.name)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun Trip.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("vehicle_id", vehicleId)
    .put("date_at", dateEpochMillis)
    .put("start_odometer_km", startOdometerKm)
    .put("end_odometer_km", endOdometerKm)
    .put("purpose", purpose)
    .put("start_location", startLocation)
    .put("end_location", endLocation)
    .put("note", note)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun FuelEntry.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("vehicle_id", vehicleId)
    .put("date_at", dateEpochMillis)
    .put("odometer_km", odometerKm)
    .put("distance_km", distanceKm)
    .put("liters", liters)
    .put("amount_cents", amountCents)
    .put("fuel_station", fuelStation)
    .put("fuel_type_label", fuelTypeLabel)
    .put("note", note)
    .put("source_type", sourceType)
    .put("source_hash", sourceHash)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun MaintenanceItem.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("vehicle_id", vehicleId)
    .put("name", name)
    .put("interval_km", intervalKm)
    .put("interval_months", intervalMonths)
    .putNullable("last_service_date", lastServiceDateEpochMillis)
    .putNullable("last_service_odometer_km", lastServiceOdometerKm)
    .putNullable("next_due_date", nextDueDateEpochMillis)
    .putNullable("next_due_odometer_km", nextDueOdometerKm)
    .put("stock_quantity", stockQuantity)
    .put("note", note)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun UserCategory.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("scope", scope.name)
    .put("label", label)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .putNullable("deleted_at", deletedAtEpochMillis)
    .put("sync_state", syncState.name)

fun UserProfile.toRemotePayload() = JSONObject()
    .put("id", id)
    .put("user_id", userId)
    .put("email", email)
    .put("display_name", displayName)
    .put("notifications_enabled", notificationsEnabled)
    .put("created_at", createdAtEpochMillis)
    .put("updated_at", updatedAtEpochMillis)
    .put("sync_state", syncState.name)

fun JSONObject.toExpenseModel() = Expense(
    id = optString("id"),
    amountCents = optLong("amount_cents"),
    merchant = optString("merchant"),
    category = enumValue(optString("category"), ExpenseCategory.Other),
    occurredAtEpochMillis = optLong("occurred_at"),
    note = optString("note"),
    customCategory = optString("custom_category"),
    paymentAccount = optString("payment_account"),
    bookingText = optString("booking_text"),
    purpose = optString("purpose"),
    tags = optString("tags"),
    sourceType = optString("source_type"),
    sourceHash = optString("source_hash"),
    importedAtEpochMillis = optNullableLong("imported_at"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toProductModel() = Product(
    id = optString("id"),
    name = optString("name"),
    manufacturer = optString("manufacturer"),
    purchasePriceCents = optLong("purchase_price_cents"),
    purchasedAtEpochMillis = optLong("purchased_at"),
    category = enumValue(optString("category"), ProductCategory.Other),
    note = optString("note"),
    customCategory = optString("custom_category"),
    modelName = optString("model_name"),
    serialReference = optString("serial_reference"),
    usageDurationMonths = optInt("usage_duration_months"),
    warrantyUntilEpochMillis = optNullableLong("warranty_until"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toVehicleModel() = Vehicle(
    id = optString("id"),
    displayName = optString("display_name"),
    manufacturer = optString("manufacturer"),
    modelName = optString("model_name"),
    licensePlate = optString("license_plate"),
    fuelType = enumValue(optString("fuel_type"), FuelType.Other),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toTripModel() = Trip(
    id = optString("id"),
    vehicleId = optString("vehicle_id"),
    dateEpochMillis = optLong("date_at"),
    startOdometerKm = optLong("start_odometer_km"),
    endOdometerKm = optLong("end_odometer_km"),
    purpose = optString("purpose"),
    startLocation = optString("start_location"),
    endLocation = optString("end_location"),
    note = optString("note"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toFuelEntryModel() = FuelEntry(
    id = optString("id"),
    vehicleId = optString("vehicle_id"),
    dateEpochMillis = optLong("date_at"),
    odometerKm = optLong("odometer_km"),
    distanceKm = optLong("distance_km"),
    liters = optDouble("liters"),
    amountCents = optLong("amount_cents"),
    fuelStation = optString("fuel_station"),
    fuelTypeLabel = optString("fuel_type_label"),
    note = optString("note"),
    sourceType = optString("source_type"),
    sourceHash = optString("source_hash"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toMaintenanceItemModel() = MaintenanceItem(
    id = optString("id"),
    vehicleId = optString("vehicle_id"),
    name = optString("name"),
    intervalKm = optLong("interval_km"),
    intervalMonths = optInt("interval_months"),
    lastServiceDateEpochMillis = optNullableLong("last_service_date"),
    lastServiceOdometerKm = optNullableLong("last_service_odometer_km"),
    nextDueDateEpochMillis = optNullableLong("next_due_date"),
    nextDueOdometerKm = optNullableLong("next_due_odometer_km"),
    stockQuantity = optInt("stock_quantity"),
    note = optString("note"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toUserCategoryModel() = UserCategory(
    id = optString("id"),
    scope = enumValue(optString("scope"), CategoryScope.Expense),
    label = optString("label"),
    userId = optString("user_id"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    deletedAtEpochMillis = optNullableLong("deleted_at"),
    syncState = SyncState.Synced
)

fun JSONObject.toUserProfileModel() = UserProfile(
    id = optString("id"),
    userId = optString("user_id"),
    email = optString("email"),
    displayName = optString("display_name"),
    notificationsEnabled = optBoolean("notifications_enabled"),
    createdAtEpochMillis = optLong("created_at"),
    updatedAtEpochMillis = optLong("updated_at"),
    syncState = SyncState.Synced
)

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    return put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.optNullableLong(key: String): Long? {
    return if (!has(key) || isNull(key)) null else optLong(key)
}

private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T {
    return enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
