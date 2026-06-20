package de.h3nri5h.spendfox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.SpendFoxSnapshot
import de.h3nri5h.spendfox.data.SpendFoxStore
import de.h3nri5h.spendfox.data.Vehicle
import de.h3nri5h.spendfox.domain.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpendFoxViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SpendFoxStore(application.applicationContext)
    private val _state = MutableStateFlow(SpendFoxUiState())
    val state: StateFlow<SpendFoxUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.seedIfEmpty()
                store.snapshot()
            }.also { snapshot ->
                _state.value = SpendFoxUiState(snapshot)
            }
        }
    }

    fun addExpense(amount: String, merchant: String, category: ExpenseCategory, note: String): Boolean {
        val cents = Money.centsFrom(amount) ?: return false
        val trimmedMerchant = merchant.trim().ifBlank { "Ausgabe" }
        viewModelScope.launch(Dispatchers.IO) {
            store.insertExpense(
                Expense(
                    amountCents = cents,
                    merchant = trimmedMerchant,
                    category = category,
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    note = note.trim()
                )
            )
            refresh()
        }
        return true
    }

    fun addProduct(name: String, manufacturer: String, price: String, category: ProductCategory): Boolean {
        val cents = Money.centsFrom(price) ?: return false
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            store.insertProduct(
                Product(
                    name = trimmedName,
                    manufacturer = manufacturer.trim(),
                    purchasePriceCents = cents,
                    purchasedAtEpochMillis = System.currentTimeMillis(),
                    category = category
                )
            )
            refresh()
        }
        return true
    }

    fun addVehicle(name: String, manufacturer: String, model: String, plate: String, fuelType: FuelType): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            store.insertVehicle(
                Vehicle(
                    displayName = trimmedName,
                    manufacturer = manufacturer.trim(),
                    modelName = model.trim(),
                    licensePlate = plate.trim(),
                    fuelType = fuelType
                )
            )
            refresh()
        }
        return true
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.deleteExpense(id)
            refresh()
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.deleteProduct(id)
            refresh()
        }
    }

    fun deleteVehicle(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            store.deleteVehicle(id)
            refresh()
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            store.clearAll()
            refresh()
        }
    }

    private suspend fun refresh() {
        val snapshot = store.snapshot()
        _state.value = SpendFoxUiState(snapshot)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                @Suppress("UNCHECKED_CAST")
                return SpendFoxViewModel(application) as T
            }
        }
    }
}

data class SpendFoxUiState(
    val expenses: List<Expense> = emptyList(),
    val products: List<Product> = emptyList(),
    val vehicles: List<Vehicle> = emptyList()
) {
    constructor(snapshot: SpendFoxSnapshot) : this(snapshot.expenses, snapshot.products, snapshot.vehicles)

    val monthlyExpenseTotalCents: Long = expenses.sumOf { it.amountCents }
    val productValueCents: Long = products.sumOf { it.purchasePriceCents }
}
