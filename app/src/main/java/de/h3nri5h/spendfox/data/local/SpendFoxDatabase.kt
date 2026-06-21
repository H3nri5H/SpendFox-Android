package de.h3nri5h.spendfox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        ProductEntity::class,
        VehicleEntity::class,
        TripEntity::class,
        FuelEntryEntity::class,
        MaintenanceItemEntity::class,
        CategoryEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SpendFoxDatabase : RoomDatabase() {
    abstract fun dao(): SpendFoxDao

    companion object {
        const val DATABASE_NAME = "nutzblick_cache.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                addColumn(db, "expenses", "custom_category", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "payment_account", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "booking_text", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "purpose", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "tags", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "source_type", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "source_hash", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "expenses", "imported_at", "INTEGER")
                addColumn(db, "expenses", "user_id", "TEXT NOT NULL DEFAULT 'local'")
                addColumn(db, "expenses", "created_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "expenses", "updated_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "expenses", "deleted_at", "INTEGER")
                addColumn(db, "expenses", "sync_state", "TEXT NOT NULL DEFAULT 'Synced'")

                addColumn(db, "products", "custom_category", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "products", "model_name", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "products", "serial_reference", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "products", "usage_duration_months", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "products", "warranty_until", "INTEGER")
                addColumn(db, "products", "user_id", "TEXT NOT NULL DEFAULT 'local'")
                addColumn(db, "products", "created_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "products", "updated_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "products", "deleted_at", "INTEGER")
                addColumn(db, "products", "sync_state", "TEXT NOT NULL DEFAULT 'Synced'")

                addColumn(db, "vehicles", "user_id", "TEXT NOT NULL DEFAULT 'local'")
                addColumn(db, "vehicles", "created_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "vehicles", "updated_at", "INTEGER NOT NULL DEFAULT $now")
                addColumn(db, "vehicles", "deleted_at", "INTEGER")
                addColumn(db, "vehicles", "sync_state", "TEXT NOT NULL DEFAULT 'Synced'")

                createNewTables(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createFuelEntriesTable(db)
            }
        }

        fun create(context: Context): SpendFoxDatabase {
            return Room.databaseBuilder(context, SpendFoxDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        private fun addColumn(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
            val cursor = db.query("PRAGMA table_info($table)")
            cursor.use {
                while (it.moveToNext()) {
                    if (it.getString(it.getColumnIndexOrThrow("name")) == column) {
                        return
                    }
                }
            }
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }

        private fun createNewTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trips (
                    id TEXT PRIMARY KEY NOT NULL,
                    vehicle_id TEXT NOT NULL,
                    date_at INTEGER NOT NULL,
                    start_odometer_km INTEGER NOT NULL,
                    end_odometer_km INTEGER NOT NULL,
                    purpose TEXT NOT NULL,
                    start_location TEXT NOT NULL DEFAULT '',
                    end_location TEXT NOT NULL DEFAULT '',
                    note TEXT NOT NULL DEFAULT '',
                    user_id TEXT NOT NULL DEFAULT 'local',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    sync_state TEXT NOT NULL DEFAULT 'PendingUpsert'
                )
                """.trimIndent()
            )
            createFuelEntriesTable(db)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS maintenance_items (
                    id TEXT PRIMARY KEY NOT NULL,
                    vehicle_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    interval_km INTEGER NOT NULL DEFAULT 0,
                    interval_months INTEGER NOT NULL DEFAULT 0,
                    last_service_date INTEGER,
                    last_service_odometer_km INTEGER,
                    next_due_date INTEGER,
                    next_due_odometer_km INTEGER,
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    note TEXT NOT NULL DEFAULT '',
                    user_id TEXT NOT NULL DEFAULT 'local',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    sync_state TEXT NOT NULL DEFAULT 'PendingUpsert'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS categories (
                    id TEXT PRIMARY KEY NOT NULL,
                    scope TEXT NOT NULL,
                    label TEXT NOT NULL,
                    user_id TEXT NOT NULL DEFAULT 'local',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    sync_state TEXT NOT NULL DEFAULT 'PendingUpsert'
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_profiles (
                    id TEXT PRIMARY KEY NOT NULL,
                    email TEXT NOT NULL DEFAULT '',
                    display_name TEXT NOT NULL DEFAULT '',
                    notifications_enabled INTEGER NOT NULL DEFAULT 0,
                    user_id TEXT NOT NULL DEFAULT 'local',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    sync_state TEXT NOT NULL DEFAULT 'PendingUpsert'
                )
                """.trimIndent()
            )
        }

        private fun createFuelEntriesTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS fuel_entries (
                    id TEXT PRIMARY KEY NOT NULL,
                    vehicle_id TEXT NOT NULL,
                    date_at INTEGER NOT NULL,
                    odometer_km INTEGER NOT NULL,
                    distance_km INTEGER NOT NULL DEFAULT 0,
                    liters REAL NOT NULL,
                    amount_cents INTEGER NOT NULL,
                    fuel_station TEXT NOT NULL DEFAULT '',
                    fuel_type_label TEXT NOT NULL DEFAULT '',
                    note TEXT NOT NULL DEFAULT '',
                    source_type TEXT NOT NULL DEFAULT '',
                    source_hash TEXT NOT NULL DEFAULT '',
                    user_id TEXT NOT NULL DEFAULT 'local',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    sync_state TEXT NOT NULL DEFAULT 'PendingUpsert'
                )
                """.trimIndent()
            )
        }
    }
}
