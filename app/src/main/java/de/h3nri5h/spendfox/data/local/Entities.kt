package de.h3nri5h.spendfox.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.h3nri5h.spendfox.data.CategoryScope
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelEntry
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.LOCAL_USER_ID
import de.h3nri5h.spendfox.data.MaintenanceItem
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.SyncState
import de.h3nri5h.spendfox.data.Trip
import de.h3nri5h.spendfox.data.UserCategory
import de.h3nri5h.spendfox.data.UserProfile
import de.h3nri5h.spendfox.data.Vehicle

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val merchant: String,
    val category: String,
    @ColumnInfo(name = "occurred_at") val occurredAtEpochMillis: Long,
    val note: String = "",
    @ColumnInfo(name = "custom_category") val customCategory: String = "",
    @ColumnInfo(name = "payment_account") val paymentAccount: String = "",
    @ColumnInfo(name = "booking_text") val bookingText: String = "",
    val purpose: String = "",
    val tags: String = "",
    @ColumnInfo(name = "source_type") val sourceType: String = "",
    @ColumnInfo(name = "source_hash") val sourceHash: String = "",
    @ColumnInfo(name = "imported_at") val importedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = Expense(
        id = id,
        amountCents = amountCents,
        merchant = merchant,
        category = enumValue(category, ExpenseCategory.Other),
        occurredAtEpochMillis = occurredAtEpochMillis,
        note = note,
        customCategory = customCategory,
        paymentAccount = paymentAccount,
        bookingText = bookingText,
        purpose = purpose,
        tags = tags,
        sourceType = sourceType,
        sourceHash = sourceHash,
        importedAtEpochMillis = importedAtEpochMillis,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturer: String,
    @ColumnInfo(name = "purchase_price_cents") val purchasePriceCents: Long,
    @ColumnInfo(name = "purchased_at") val purchasedAtEpochMillis: Long,
    val category: String,
    val note: String = "",
    @ColumnInfo(name = "custom_category") val customCategory: String = "",
    @ColumnInfo(name = "model_name") val modelName: String = "",
    @ColumnInfo(name = "serial_reference") val serialReference: String = "",
    @ColumnInfo(name = "usage_duration_months") val usageDurationMonths: Int = 0,
    @ColumnInfo(name = "warranty_until") val warrantyUntilEpochMillis: Long? = null,
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = Product(
        id = id,
        name = name,
        manufacturer = manufacturer,
        purchasePriceCents = purchasePriceCents,
        purchasedAtEpochMillis = purchasedAtEpochMillis,
        category = enumValue(category, ProductCategory.Other),
        note = note,
        customCategory = customCategory,
        modelName = modelName,
        serialReference = serialReference,
        usageDurationMonths = usageDurationMonths,
        warrantyUntilEpochMillis = warrantyUntilEpochMillis,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val manufacturer: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "license_plate") val licensePlate: String,
    @ColumnInfo(name = "fuel_type") val fuelType: String,
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = Vehicle(
        id = id,
        displayName = displayName,
        manufacturer = manufacturer,
        modelName = modelName,
        licensePlate = licensePlate,
        fuelType = enumValue(fuelType, FuelType.Other),
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "date_at") val dateEpochMillis: Long,
    @ColumnInfo(name = "start_odometer_km") val startOdometerKm: Long,
    @ColumnInfo(name = "end_odometer_km") val endOdometerKm: Long,
    val purpose: String,
    @ColumnInfo(name = "start_location") val startLocation: String = "",
    @ColumnInfo(name = "end_location") val endLocation: String = "",
    val note: String = "",
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = Trip(
        id = id,
        vehicleId = vehicleId,
        dateEpochMillis = dateEpochMillis,
        startOdometerKm = startOdometerKm,
        endOdometerKm = endOdometerKm,
        purpose = purpose,
        startLocation = startLocation,
        endLocation = endLocation,
        note = note,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "fuel_entries")
data class FuelEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "date_at") val dateEpochMillis: Long,
    @ColumnInfo(name = "odometer_km") val odometerKm: Long,
    @ColumnInfo(name = "distance_km") val distanceKm: Long = 0,
    val liters: Double,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "fuel_station") val fuelStation: String = "",
    @ColumnInfo(name = "fuel_type_label") val fuelTypeLabel: String = "",
    val note: String = "",
    @ColumnInfo(name = "source_type") val sourceType: String = "",
    @ColumnInfo(name = "source_hash") val sourceHash: String = "",
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = FuelEntry(
        id = id,
        vehicleId = vehicleId,
        dateEpochMillis = dateEpochMillis,
        odometerKm = odometerKm,
        distanceKm = distanceKm,
        liters = liters,
        amountCents = amountCents,
        fuelStation = fuelStation,
        fuelTypeLabel = fuelTypeLabel,
        note = note,
        sourceType = sourceType,
        sourceHash = sourceHash,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "maintenance_items")
data class MaintenanceItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    val name: String,
    @ColumnInfo(name = "interval_km") val intervalKm: Long = 0,
    @ColumnInfo(name = "interval_months") val intervalMonths: Int = 0,
    @ColumnInfo(name = "last_service_date") val lastServiceDateEpochMillis: Long? = null,
    @ColumnInfo(name = "last_service_odometer_km") val lastServiceOdometerKm: Long? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDateEpochMillis: Long? = null,
    @ColumnInfo(name = "next_due_odometer_km") val nextDueOdometerKm: Long? = null,
    @ColumnInfo(name = "stock_quantity") val stockQuantity: Int = 0,
    val note: String = "",
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = MaintenanceItem(
        id = id,
        vehicleId = vehicleId,
        name = name,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        lastServiceDateEpochMillis = lastServiceDateEpochMillis,
        lastServiceOdometerKm = lastServiceOdometerKm,
        nextDueDateEpochMillis = nextDueDateEpochMillis,
        nextDueOdometerKm = nextDueOdometerKm,
        stockQuantity = stockQuantity,
        note = note,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val scope: String,
    val label: String,
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = UserCategory(
        id = id,
        scope = enumValue(scope, CategoryScope.Expense),
        label = label,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String = "",
    @ColumnInfo(name = "display_name") val displayName: String = "",
    @ColumnInfo(name = "notifications_enabled") val notificationsEnabled: Boolean = false,
    @ColumnInfo(name = "user_id") val userId: String = LOCAL_USER_ID,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.PendingUpsert.name
) {
    fun toModel() = UserProfile(
        id = id,
        email = email,
        displayName = displayName,
        notificationsEnabled = notificationsEnabled,
        userId = userId,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        syncState = enumValue(syncState, SyncState.PendingUpsert)
    )
}

fun Expense.toEntity(now: Long = System.currentTimeMillis()) = ExpenseEntity(
    id = id,
    amountCents = amountCents,
    merchant = merchant,
    category = category.name,
    occurredAtEpochMillis = occurredAtEpochMillis,
    note = note,
    customCategory = customCategory,
    paymentAccount = paymentAccount,
    bookingText = bookingText,
    purpose = purpose,
    tags = tags,
    sourceType = sourceType,
    sourceHash = sourceHash,
    importedAtEpochMillis = importedAtEpochMillis,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun Product.toEntity(now: Long = System.currentTimeMillis()) = ProductEntity(
    id = id,
    name = name,
    manufacturer = manufacturer,
    purchasePriceCents = purchasePriceCents,
    purchasedAtEpochMillis = purchasedAtEpochMillis,
    category = category.name,
    note = note,
    customCategory = customCategory,
    modelName = modelName,
    serialReference = serialReference,
    usageDurationMonths = usageDurationMonths,
    warrantyUntilEpochMillis = warrantyUntilEpochMillis,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun Vehicle.toEntity(now: Long = System.currentTimeMillis()) = VehicleEntity(
    id = id,
    displayName = displayName,
    manufacturer = manufacturer,
    modelName = modelName,
    licensePlate = licensePlate,
    fuelType = fuelType.name,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun Trip.toEntity(now: Long = System.currentTimeMillis()) = TripEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochMillis = dateEpochMillis,
    startOdometerKm = startOdometerKm,
    endOdometerKm = endOdometerKm,
    purpose = purpose,
    startLocation = startLocation,
    endLocation = endLocation,
    note = note,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun FuelEntry.toEntity(now: Long = System.currentTimeMillis()) = FuelEntryEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochMillis = dateEpochMillis,
    odometerKm = odometerKm,
    distanceKm = distanceKm,
    liters = liters,
    amountCents = amountCents,
    fuelStation = fuelStation,
    fuelTypeLabel = fuelTypeLabel,
    note = note,
    sourceType = sourceType,
    sourceHash = sourceHash,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun MaintenanceItem.toEntity(now: Long = System.currentTimeMillis()) = MaintenanceItemEntity(
    id = id,
    vehicleId = vehicleId,
    name = name,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    lastServiceDateEpochMillis = lastServiceDateEpochMillis,
    lastServiceOdometerKm = lastServiceOdometerKm,
    nextDueDateEpochMillis = nextDueDateEpochMillis,
    nextDueOdometerKm = nextDueOdometerKm,
    stockQuantity = stockQuantity,
    note = note,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun UserCategory.toEntity(now: Long = System.currentTimeMillis()) = CategoryEntity(
    id = id,
    scope = scope.name,
    label = label,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    deletedAtEpochMillis = deletedAtEpochMillis,
    syncState = syncState.name
)

fun UserProfile.toEntity(now: Long = System.currentTimeMillis()) = UserProfileEntity(
    id = id,
    email = email,
    displayName = displayName,
    notificationsEnabled = notificationsEnabled,
    userId = userId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = now,
    syncState = syncState.name
)

private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T {
    return enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
