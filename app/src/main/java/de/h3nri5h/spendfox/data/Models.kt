package de.h3nri5h.spendfox.data

import java.util.UUID

const val LOCAL_USER_ID = "local"

enum class SyncState {
    Synced,
    PendingUpsert,
    PendingDelete,
    Failed
}

enum class ExpenseCategory(val label: String) {
    Groceries("Lebensmittel"),
    Housing("Wohnen"),
    Mobility("Mobilität"),
    Leisure("Freizeit"),
    Health("Gesundheit"),
    Other("Sonstiges")
}

enum class ProductCategory(val label: String) {
    Technology("Technik"),
    Furniture("Möbel"),
    Household("Haushalt"),
    Sports("Sport"),
    Other("Sonstiges")
}

enum class FuelType(val label: String) {
    Petrol("Benzin"),
    Diesel("Diesel"),
    Hybrid("Hybrid"),
    Electric("Elektro"),
    Other("Sonstiges")
}

enum class CategoryScope(val label: String) {
    Expense("Ausgaben"),
    Product("Produkte"),
    VehicleMaintenance("Fahrzeugwartung")
}

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amountCents: Long,
    val merchant: String,
    val category: ExpenseCategory,
    val occurredAtEpochMillis: Long,
    val note: String = "",
    val customCategory: String = "",
    val paymentAccount: String = "",
    val bookingText: String = "",
    val purpose: String = "",
    val tags: String = "",
    val sourceType: String = "",
    val sourceHash: String = "",
    val importedAtEpochMillis: Long? = null,
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
)

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val manufacturer: String,
    val purchasePriceCents: Long,
    val purchasedAtEpochMillis: Long,
    val category: ProductCategory,
    val note: String = "",
    val customCategory: String = "",
    val modelName: String = "",
    val serialReference: String = "",
    val usageDurationMonths: Int = 0,
    val warrantyUntilEpochMillis: Long? = null,
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
) {
    val monthlyCostCents: Long?
        get() = usageDurationMonths.takeIf { it > 0 }?.let { purchasePriceCents / it }
}

data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val manufacturer: String,
    val modelName: String,
    val licensePlate: String,
    val fuelType: FuelType,
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
)

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val dateEpochMillis: Long,
    val startOdometerKm: Long,
    val endOdometerKm: Long,
    val purpose: String,
    val startLocation: String = "",
    val endLocation: String = "",
    val note: String = "",
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
) {
    val distanceKm: Long
        get() = (endOdometerKm - startOdometerKm).coerceAtLeast(0)
}

data class FuelEntry(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val dateEpochMillis: Long,
    val odometerKm: Long,
    val distanceKm: Long = 0,
    val liters: Double,
    val amountCents: Long,
    val fuelStation: String = "",
    val fuelTypeLabel: String = "",
    val note: String = "",
    val sourceType: String = "",
    val sourceHash: String = "",
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
) {
    val consumptionLitersPer100Km: Double?
        get() = distanceKm.takeIf { it > 0 }?.let { liters / (it / 100.0) }

    val pricePerLiterCents: Long?
        get() = liters.takeIf { it > 0.0 }?.let { (amountCents / it).toLong() }
}

data class MaintenanceItem(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val name: String,
    val intervalKm: Long = 0,
    val intervalMonths: Int = 0,
    val lastServiceDateEpochMillis: Long? = null,
    val lastServiceOdometerKm: Long? = null,
    val nextDueDateEpochMillis: Long? = null,
    val nextDueOdometerKm: Long? = null,
    val stockQuantity: Int = 0,
    val note: String = "",
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
)

data class UserCategory(
    val id: String = UUID.randomUUID().toString(),
    val scope: CategoryScope,
    val label: String,
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val deletedAtEpochMillis: Long? = null,
    val syncState: SyncState = SyncState.PendingUpsert
)

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val displayName: String = "",
    val notificationsEnabled: Boolean = false,
    val userId: String = LOCAL_USER_ID,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val syncState: SyncState = SyncState.PendingUpsert
)

data class SpendFoxSnapshot(
    val expenses: List<Expense> = emptyList(),
    val products: List<Product> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val fuelEntries: List<FuelEntry> = emptyList(),
    val maintenanceItems: List<MaintenanceItem> = emptyList(),
    val categories: List<UserCategory> = emptyList(),
    val profile: UserProfile? = null
)
