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

    suspend fun refreshFromRemote(userId: String): Result<Unit> {
        return syncRepository.fetchSnapshot(userId).mapCatching { snapshot ->
            clearAccountData(userId)
            dao.upsertExpenses(snapshot.expenses.map { it.toEntity() })
            snapshot.products.forEach { dao.upsertProduct(it.toEntity()) }
            snapshot.vehicles.forEach { dao.upsertVehicle(it.toEntity()) }
            snapshot.trips.forEach { dao.upsertTrip(it.toEntity()) }
            dao.upsertFuelEntries(snapshot.fuelEntries.map { it.toEntity() })
            snapshot.maintenanceItems.forEach { dao.upsertMaintenanceItem(it.toEntity()) }
            snapshot.categories.forEach { dao.upsertCategory(it.toEntity()) }
            snapshot.profile?.let { dao.upsertProfile(it.toEntity()) }
        }
    }

    suspend fun upsertExpense(expense: Expense) {
        require(expense.amountCents > 0)
        require(expense.merchant.isNotBlank())
        val synced = expense.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("expenses", synced.toRemotePayload())) {
            dao.upsertExpense(synced.toEntity())
        }
    }

    suspend fun upsertExpenses(expenses: List<Expense>) {
        val stored = mutableListOf<Expense>()
        expenses.map { it.copy(syncState = SyncState.Synced) }.forEach { expense ->
            if (syncRepository.upsert("expenses", expense.toRemotePayload())) {
                stored += expense
            }
        }
        if (stored.isNotEmpty()) {
            dao.upsertExpenses(stored.map { it.toEntity() })
        }
    }

    suspend fun upsertProduct(product: Product) {
        require(product.name.isNotBlank())
        require(product.purchasePriceCents > 0)
        val synced = product.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("products", synced.toRemotePayload())) {
            dao.upsertProduct(synced.toEntity())
        }
    }

    suspend fun upsertVehicle(vehicle: Vehicle) {
        require(vehicle.displayName.isNotBlank())
        val synced = vehicle.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("vehicles", synced.toRemotePayload())) {
            dao.upsertVehicle(synced.toEntity())
        }
    }

    suspend fun upsertTrip(trip: Trip) {
        require(trip.vehicleId.isNotBlank())
        require(trip.endOdometerKm >= trip.startOdometerKm)
        val synced = trip.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("trips", synced.toRemotePayload())) {
            dao.upsertTrip(synced.toEntity())
        }
    }

    suspend fun upsertFuelEntry(entry: FuelEntry) {
        require(entry.vehicleId.isNotBlank())
        require(entry.odometerKm >= 0)
        require(entry.liters > 0.0)
        require(entry.amountCents >= 0)
        val synced = entry.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("fuel_entries", synced.toRemotePayload())) {
            dao.upsertFuelEntry(synced.toEntity())
        }
    }

    suspend fun upsertFuelEntries(entries: List<FuelEntry>) {
        val stored = mutableListOf<FuelEntry>()
        entries.map { it.copy(syncState = SyncState.Synced) }.forEach { entry ->
            if (syncRepository.upsert("fuel_entries", entry.toRemotePayload())) {
                stored += entry
            }
        }
        if (stored.isNotEmpty()) {
            dao.upsertFuelEntries(stored.map { it.toEntity() })
        }
    }

    suspend fun upsertMaintenanceItem(item: MaintenanceItem) {
        require(item.vehicleId.isNotBlank())
        require(item.name.isNotBlank())
        val synced = item.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("maintenance_items", synced.toRemotePayload())) {
            dao.upsertMaintenanceItem(synced.toEntity())
        }
    }

    suspend fun upsertCategory(category: UserCategory) {
        require(category.label.isNotBlank())
        val synced = category.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("categories", synced.toRemotePayload())) {
            dao.upsertCategory(synced.toEntity())
        }
    }

    suspend fun upsertProfile(profile: UserProfile) {
        val synced = profile.copy(syncState = SyncState.Synced)
        if (syncRepository.upsert("user_profiles", synced.toRemotePayload())) {
            dao.upsertProfile(synced.toEntity())
        }
    }

    suspend fun deleteExpense(id: String) = id.takeIf(String::isNotBlank)?.let {
        val now = System.currentTimeMillis()
        val deleted = dao.expenseById(it)?.toModel()?.copy(
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncState = SyncState.Synced
        ) ?: return@let
        if (syncRepository.upsert("expenses", deleted.toRemotePayload())) {
            dao.upsertExpense(deleted.toEntity(now))
        }
    }

    suspend fun deleteProduct(id: String) = id.takeIf(String::isNotBlank)?.let {
        val now = System.currentTimeMillis()
        val deleted = dao.productById(it)?.toModel()?.copy(
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncState = SyncState.Synced
        ) ?: return@let
        if (syncRepository.upsert("products", deleted.toRemotePayload())) {
            dao.upsertProduct(deleted.toEntity(now))
        }
    }

    suspend fun deleteVehicle(id: String) = id.takeIf(String::isNotBlank)?.let {
        val now = System.currentTimeMillis()
        val deleted = dao.vehicleById(it)?.toModel()?.copy(
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncState = SyncState.Synced
        ) ?: return@let
        if (syncRepository.upsert("vehicles", deleted.toRemotePayload())) {
            dao.upsertVehicle(deleted.toEntity(now))
        }
    }

    suspend fun deleteFuelEntry(id: String) = id.takeIf(String::isNotBlank)?.let {
        val now = System.currentTimeMillis()
        val deleted = dao.fuelEntryById(it)?.toModel()?.copy(
            deletedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncState = SyncState.Synced
        ) ?: return@let
        if (syncRepository.upsert("fuel_entries", deleted.toRemotePayload())) {
            dao.upsertFuelEntry(deleted.toEntity(now))
        }
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
}
