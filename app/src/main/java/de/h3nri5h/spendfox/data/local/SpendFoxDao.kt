package de.h3nri5h.spendfox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendFoxDao {
    @Query("SELECT * FROM expenses WHERE deleted_at IS NULL AND user_id = :userId ORDER BY occurred_at DESC")
    fun observeExpenses(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM products WHERE deleted_at IS NULL AND user_id = :userId ORDER BY purchased_at DESC")
    fun observeProducts(userId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM vehicles WHERE deleted_at IS NULL AND user_id = :userId ORDER BY display_name ASC")
    fun observeVehicles(userId: String): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM trips WHERE deleted_at IS NULL AND user_id = :userId ORDER BY date_at DESC")
    fun observeTrips(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM fuel_entries WHERE deleted_at IS NULL AND user_id = :userId ORDER BY date_at DESC, odometer_km DESC")
    fun observeFuelEntries(userId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM maintenance_items WHERE deleted_at IS NULL AND user_id = :userId ORDER BY name ASC")
    fun observeMaintenanceItems(userId: String): Flow<List<MaintenanceItemEntity>>

    @Query("SELECT * FROM categories WHERE deleted_at IS NULL AND user_id = :userId ORDER BY label ASC")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM user_profiles WHERE user_id = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenses(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFuelEntry(entry: FuelEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFuelEntries(entries: List<FuelEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaintenanceItem(item: MaintenanceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("UPDATE expenses SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteExpense(id: String, now: Long, syncState: String)

    @Query("UPDATE products SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteProduct(id: String, now: Long, syncState: String)

    @Query("UPDATE vehicles SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteVehicle(id: String, now: Long, syncState: String)

    @Query("UPDATE trips SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteTrip(id: String, now: Long, syncState: String)

    @Query("UPDATE fuel_entries SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteFuelEntry(id: String, now: Long, syncState: String)

    @Query("UPDATE maintenance_items SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteMaintenanceItem(id: String, now: Long, syncState: String)

    @Query("UPDATE categories SET deleted_at = :now, updated_at = :now, sync_state = :syncState WHERE id = :id")
    suspend fun softDeleteCategory(id: String, now: Long, syncState: String)

    @Query("SELECT source_hash FROM expenses WHERE user_id = :userId AND source_hash != ''")
    suspend fun expenseSourceHashes(userId: String): List<String>

    @Query("DELETE FROM expenses WHERE user_id = :userId")
    suspend fun hardDeleteExpensesForUser(userId: String)

    @Query("DELETE FROM products WHERE user_id = :userId")
    suspend fun hardDeleteProductsForUser(userId: String)

    @Query("DELETE FROM vehicles WHERE user_id = :userId")
    suspend fun hardDeleteVehiclesForUser(userId: String)

    @Query("DELETE FROM trips WHERE user_id = :userId")
    suspend fun hardDeleteTripsForUser(userId: String)

    @Query("DELETE FROM fuel_entries WHERE user_id = :userId")
    suspend fun hardDeleteFuelEntriesForUser(userId: String)

    @Query("DELETE FROM maintenance_items WHERE user_id = :userId")
    suspend fun hardDeleteMaintenanceItemsForUser(userId: String)

    @Query("DELETE FROM categories WHERE user_id = :userId")
    suspend fun hardDeleteCategoriesForUser(userId: String)

    @Query("DELETE FROM user_profiles WHERE user_id = :userId")
    suspend fun hardDeleteProfileForUser(userId: String)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun expenseById(id: String): ExpenseEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun productById(id: String): ProductEntity?

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun vehicleById(id: String): VehicleEntity?

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    suspend fun fuelEntryById(id: String): FuelEntryEntity?
}
