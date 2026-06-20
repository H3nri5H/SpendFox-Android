package de.h3nri5h.spendfox.data

import de.h3nri5h.spendfox.data.local.SpendFoxDao
import de.h3nri5h.spendfox.data.local.toEntity
import de.h3nri5h.spendfox.data.sync.SyncRepository
import de.h3nri5h.spendfox.data.supabase.toRemotePayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SpendFoxRepository(
    private val dao: SpendFoxDao,
    private val syncRepository: SyncRepository
) {
    fun observeSnapshot(userId: String): Flow<SpendFoxSnapshot> {
        return combine(
            dao.observeExpenses(userId).map { it as Any? },
            dao.observeProducts(userId).map { it as Any? },
            dao.observeVehicles(userId).map { it as Any? },
            dao.observeTrips(userId).map { it as Any? },
            dao.observeFuelEntries(userId).map { it as Any? },
            dao.observeMaintenanceItems(userId).map { it as Any? },
            dao.observeCategories(userId).map { it as Any? },
            dao.observeProfile(userId).map { it as Any? }
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            SpendFoxSnapshot(
                expenses = (values[0] as List<de.h3nri5h.spendfox.data.local.ExpenseEntity>).map { it.toModel() },
                products = (values[1] as List<de.h3nri5h.spendfox.data.local.ProductEntity>).map { it.toModel() },
                vehicles = (values[2] as List<de.h3nri5h.spendfox.data.local.VehicleEntity>).map { it.toModel() },
                trips = (values[3] as List<de.h3nri5h.spendfox.data.local.TripEntity>).map { it.toModel() },
                fuelEntries = (values[4] as List<de.h3nri5h.spendfox.data.local.FuelEntryEntity>).map { it.toModel() },
                maintenanceItems = (values[5] as List<de.h3nri5h.spendfox.data.local.MaintenanceItemEntity>).map { it.toModel() },
                categories = (values[6] as List<de.h3nri5h.spendfox.data.local.CategoryEntity>).map { it.toModel() },
                profile = (values[7] as de.h3nri5h.spendfox.data.local.UserProfileEntity?)?.toModel()
            )
        }
    }

    fun observeExpenseSourceHashes(userId: String): Flow<Set<String>> {
        return observeSnapshot(userId).map { snapshot ->
            snapshot.expenses.mapNotNull { it.sourceHash.takeIf(String::isNotBlank) }.toSet()
        }
    }

    suspend fun seedIfEmpty(userId: String) {
        if (dao.expenseCount() > 0) return
        val now = System.currentTimeMillis()
        upsertExpense(
            Expense(
                amountCents = 6420,
                merchant = "Wocheneinkauf",
                category = ExpenseCategory.Groceries,
                occurredAtEpochMillis = now - ONE_DAY,
                userId = userId,
                syncState = SyncState.Synced
            )
        )
        upsertExpense(
            Expense(
                amountCents = 4900,
                merchant = "Monatsticket",
                category = ExpenseCategory.Mobility,
                occurredAtEpochMillis = now - 3 * ONE_DAY,
                userId = userId,
                syncState = SyncState.Synced
            )
        )
        upsertProduct(
            Product(
                name = "Notebook",
                manufacturer = "Beispiel",
                purchasePriceCents = 129900,
                purchasedAtEpochMillis = now - 180 * ONE_DAY,
                category = ProductCategory.Technology,
                usageDurationMonths = 48,
                userId = userId,
                syncState = SyncState.Synced
            )
        )
        upsertVehicle(
            Vehicle(
                displayName = "Alltagsauto",
                manufacturer = "Beispiel",
                modelName = "Kompakt",
                licensePlate = "XX SF 2026",
                fuelType = FuelType.Petrol,
                userId = userId,
                syncState = SyncState.Synced
            )
        )
    }

    suspend fun attachLocalDataToUser(userId: String) {
        val now = System.currentTimeMillis()
        val state = SyncState.PendingUpsert.name
        dao.attachLocalExpenses(userId, now, state)
        dao.attachLocalProducts(userId, now, state)
        dao.attachLocalVehicles(userId, now, state)
        dao.attachLocalFuelEntries(userId, now, state)
        dao.attachLocalTrips(userId, now, state)
        dao.attachLocalMaintenanceItems(userId, now, state)
        dao.attachLocalCategories(userId, now, state)
    }

    suspend fun upsertExpense(expense: Expense) {
        require(expense.amountCents > 0)
        require(expense.merchant.isNotBlank())
        val pending = expense.copy(syncState = syncRepository.pendingState())
        dao.upsertExpense(pending.toEntity())
        syncRepository.upsert("expenses", pending.toRemotePayload())
    }

    suspend fun upsertExpenses(expenses: List<Expense>) {
        val pending = expenses.map { it.copy(syncState = syncRepository.pendingState()) }
        dao.upsertExpenses(pending.map { it.toEntity() })
        pending.forEach { syncRepository.upsert("expenses", it.toRemotePayload()) }
    }

    suspend fun upsertProduct(product: Product) {
        require(product.name.isNotBlank())
        require(product.purchasePriceCents > 0)
        val pending = product.copy(syncState = syncRepository.pendingState())
        dao.upsertProduct(pending.toEntity())
        syncRepository.upsert("products", pending.toRemotePayload())
    }

    suspend fun upsertVehicle(vehicle: Vehicle) {
        require(vehicle.displayName.isNotBlank())
        val pending = vehicle.copy(syncState = syncRepository.pendingState())
        dao.upsertVehicle(pending.toEntity())
        syncRepository.upsert("vehicles", pending.toRemotePayload())
    }

    suspend fun upsertTrip(trip: Trip) {
        require(trip.vehicleId.isNotBlank())
        require(trip.endOdometerKm >= trip.startOdometerKm)
        dao.upsertTrip(trip.copy(syncState = syncRepository.pendingState()).toEntity())
    }

    suspend fun upsertFuelEntry(entry: FuelEntry) {
        require(entry.vehicleId.isNotBlank())
        require(entry.odometerKm >= 0)
        require(entry.liters > 0.0)
        require(entry.amountCents >= 0)
        val pending = entry.copy(syncState = syncRepository.pendingState())
        dao.upsertFuelEntry(pending.toEntity())
        syncRepository.upsert("fuel_entries", pending.toRemotePayload())
    }

    suspend fun upsertFuelEntries(entries: List<FuelEntry>) {
        val pending = entries.map { it.copy(syncState = syncRepository.pendingState()) }
        dao.upsertFuelEntries(pending.map { it.toEntity() })
        pending.forEach { syncRepository.upsert("fuel_entries", it.toRemotePayload()) }
    }

    suspend fun upsertMaintenanceItem(item: MaintenanceItem) {
        require(item.vehicleId.isNotBlank())
        require(item.name.isNotBlank())
        val pending = item.copy(syncState = syncRepository.pendingState())
        dao.upsertMaintenanceItem(pending.toEntity())
        syncRepository.upsert("maintenance_items", pending.toRemotePayload())
    }

    suspend fun upsertCategory(category: UserCategory) {
        require(category.label.isNotBlank())
        val pending = category.copy(syncState = syncRepository.pendingState())
        dao.upsertCategory(pending.toEntity())
        syncRepository.upsert("categories", pending.toRemotePayload())
    }

    suspend fun upsertProfile(profile: UserProfile) {
        val pending = profile.copy(syncState = syncRepository.pendingState())
        dao.upsertProfile(pending.toEntity())
        syncRepository.upsert("user_profiles", pending.toRemotePayload())
    }

    suspend fun deleteExpense(id: String) = id.takeIf(String::isNotBlank)?.let {
        dao.softDeleteExpense(it, System.currentTimeMillis(), SyncState.PendingDelete.name)
    }

    suspend fun deleteProduct(id: String) = id.takeIf(String::isNotBlank)?.let {
        dao.softDeleteProduct(it, System.currentTimeMillis(), SyncState.PendingDelete.name)
    }

    suspend fun deleteVehicle(id: String) = id.takeIf(String::isNotBlank)?.let {
        dao.softDeleteVehicle(it, System.currentTimeMillis(), SyncState.PendingDelete.name)
    }

    suspend fun deleteFuelEntry(id: String) = id.takeIf(String::isNotBlank)?.let {
        dao.softDeleteFuelEntry(it, System.currentTimeMillis(), SyncState.PendingDelete.name)
    }

    suspend fun clearAccountData(userId: String) {
        dao.hardDeleteExpensesForUser(userId)
        dao.hardDeleteProductsForUser(userId)
        dao.hardDeleteVehiclesForUser(userId)
        dao.hardDeleteTripsForUser(userId)
        dao.hardDeleteFuelEntriesForUser(userId)
        dao.hardDeleteMaintenanceItemsForUser(userId)
        dao.hardDeleteCategoriesForUser(userId)
        dao.hardDeleteProfileForUser(userId)
    }

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1000L
    }
}
