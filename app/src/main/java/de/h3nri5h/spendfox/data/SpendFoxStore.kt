package de.h3nri5h.spendfox.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SpendFoxStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE expenses (
                id TEXT PRIMARY KEY NOT NULL,
                amount_cents INTEGER NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                occurred_at INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE products (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                purchase_price_cents INTEGER NOT NULL,
                purchased_at INTEGER NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE vehicles (
                id TEXT PRIMARY KEY NOT NULL,
                display_name TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                model_name TEXT NOT NULL,
                license_plate TEXT NOT NULL,
                fuel_type TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun snapshot(): SpendFoxSnapshot {
        return SpendFoxSnapshot(
            expenses = readExpenses(),
            products = readProducts(),
            vehicles = readVehicles()
        )
    }

    @Synchronized
    fun seedIfEmpty() {
        val totalRecords = countRows("expenses") + countRows("products") + countRows("vehicles")
        if (totalRecords > 0) return

        val now = System.currentTimeMillis()
        insertExpense(
            Expense(
                amountCents = 6420,
                merchant = "Wocheneinkauf",
                category = ExpenseCategory.Groceries,
                occurredAtEpochMillis = now - ONE_DAY
            )
        )
        insertExpense(
            Expense(
                amountCents = 4900,
                merchant = "Monatsticket",
                category = ExpenseCategory.Mobility,
                occurredAtEpochMillis = now - 3 * ONE_DAY
            )
        )
        insertExpense(
            Expense(
                amountCents = 1299,
                merchant = "Streaming",
                category = ExpenseCategory.Leisure,
                occurredAtEpochMillis = now - 5 * ONE_DAY
            )
        )
        insertProduct(
            Product(
                name = "Notebook",
                manufacturer = "Beispiel",
                purchasePriceCents = 129900,
                purchasedAtEpochMillis = now - 180 * ONE_DAY,
                category = ProductCategory.Technology
            )
        )
        insertVehicle(
            Vehicle(
                displayName = "Alltagsauto",
                manufacturer = "Beispiel",
                modelName = "Kompakt",
                licensePlate = "XX SF 2026",
                fuelType = FuelType.Petrol
            )
        )
    }

    @Synchronized
    fun insertExpense(expense: Expense) {
        writableDatabase.insertWithOnConflict("expenses", null, ContentValues().apply {
            put("id", expense.id)
            put("amount_cents", expense.amountCents)
            put("merchant", expense.merchant)
            put("category", expense.category.name)
            put("occurred_at", expense.occurredAtEpochMillis)
            put("note", expense.note)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun insertProduct(product: Product) {
        writableDatabase.insertWithOnConflict("products", null, ContentValues().apply {
            put("id", product.id)
            put("name", product.name)
            put("manufacturer", product.manufacturer)
            put("purchase_price_cents", product.purchasePriceCents)
            put("purchased_at", product.purchasedAtEpochMillis)
            put("category", product.category.name)
            put("note", product.note)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun insertVehicle(vehicle: Vehicle) {
        writableDatabase.insertWithOnConflict("vehicles", null, ContentValues().apply {
            put("id", vehicle.id)
            put("display_name", vehicle.displayName)
            put("manufacturer", vehicle.manufacturer)
            put("model_name", vehicle.modelName)
            put("license_plate", vehicle.licensePlate)
            put("fuel_type", vehicle.fuelType.name)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun deleteExpense(id: String) {
        writableDatabase.delete("expenses", "id = ?", arrayOf(id))
    }

    @Synchronized
    fun deleteProduct(id: String) {
        writableDatabase.delete("products", "id = ?", arrayOf(id))
    }

    @Synchronized
    fun deleteVehicle(id: String) {
        writableDatabase.delete("vehicles", "id = ?", arrayOf(id))
    }

    @Synchronized
    fun clearAll() {
        writableDatabase.delete("expenses", null, null)
        writableDatabase.delete("products", null, null)
        writableDatabase.delete("vehicles", null, null)
    }

    private fun readExpenses(): List<Expense> {
        val cursor = readableDatabase.query(
            "expenses",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC"
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        Expense(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            amountCents = it.getLong(it.getColumnIndexOrThrow("amount_cents")),
                            merchant = it.getString(it.getColumnIndexOrThrow("merchant")),
                            category = enumValue(
                                it.getString(it.getColumnIndexOrThrow("category")),
                                ExpenseCategory.Other
                            ),
                            occurredAtEpochMillis = it.getLong(it.getColumnIndexOrThrow("occurred_at")),
                            note = it.getString(it.getColumnIndexOrThrow("note"))
                        )
                    )
                }
            }
        }
    }

    private fun readProducts(): List<Product> {
        val cursor = readableDatabase.query("products", null, null, null, null, null, "purchased_at DESC")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        Product(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            name = it.getString(it.getColumnIndexOrThrow("name")),
                            manufacturer = it.getString(it.getColumnIndexOrThrow("manufacturer")),
                            purchasePriceCents = it.getLong(it.getColumnIndexOrThrow("purchase_price_cents")),
                            purchasedAtEpochMillis = it.getLong(it.getColumnIndexOrThrow("purchased_at")),
                            category = enumValue(
                                it.getString(it.getColumnIndexOrThrow("category")),
                                ProductCategory.Other
                            ),
                            note = it.getString(it.getColumnIndexOrThrow("note"))
                        )
                    )
                }
            }
        }
    }

    private fun readVehicles(): List<Vehicle> {
        val cursor = readableDatabase.query("vehicles", null, null, null, null, null, "display_name ASC")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        Vehicle(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            displayName = it.getString(it.getColumnIndexOrThrow("display_name")),
                            manufacturer = it.getString(it.getColumnIndexOrThrow("manufacturer")),
                            modelName = it.getString(it.getColumnIndexOrThrow("model_name")),
                            licensePlate = it.getString(it.getColumnIndexOrThrow("license_plate")),
                            fuelType = enumValue(
                                it.getString(it.getColumnIndexOrThrow("fuel_type")),
                                FuelType.Other
                            )
                        )
                    )
                }
            }
        }
    }

    private fun countRows(table: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: fallback
    }

    companion object {
        private const val DATABASE_NAME = "spendfox.db"
        private const val DATABASE_VERSION = 1
        private const val ONE_DAY = 24L * 60L * 60L * 1000L
    }
}
