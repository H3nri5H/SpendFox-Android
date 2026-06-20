package de.h3nri5h.spendfox.data

import java.util.UUID

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

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amountCents: Long,
    val merchant: String,
    val category: ExpenseCategory,
    val occurredAtEpochMillis: Long,
    val note: String = ""
)

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val manufacturer: String,
    val purchasePriceCents: Long,
    val purchasedAtEpochMillis: Long,
    val category: ProductCategory,
    val note: String = ""
)

data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val manufacturer: String,
    val modelName: String,
    val licensePlate: String,
    val fuelType: FuelType
)

data class SpendFoxSnapshot(
    val expenses: List<Expense> = emptyList(),
    val products: List<Product> = emptyList(),
    val vehicles: List<Vehicle> = emptyList()
)
