package com.ayush.tranxporter.driver.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class DriverHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DriverHomeUiState())
    val uiState: StateFlow<DriverHomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Simulate API call
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Fetch data from repository
                val earnings = fetchEarnings()
                val stats = fetchStats()
                val orders = fetchOrders()

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        earnings = earnings,
                        stats = stats,
                        orders = orders
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                ) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // Refresh data from repository
                val earnings = fetchEarnings()
                val stats = fetchStats()
                val orders = fetchOrders()

                _uiState.update { currentState ->
                    currentState.copy(
                        isRefreshing = false,
                        earnings = earnings,
                        stats = stats,
                        orders = orders
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    error = e.message ?: "Unknown error occurred"
                ) }
            }
        }
    }

    fun toggleOnlineStatus() {
        _uiState.update { currentState ->
            currentState.copy(isOnline = !currentState.isOnline)
        }
    }

    fun toggleOrderExpansion(orderId: String) {
        _uiState.update { currentState ->
            val expandedOrderIds = currentState.expandedOrderIds.toMutableSet()
            if (expandedOrderIds.contains(orderId)) {
                expandedOrderIds.remove(orderId)
            } else {
                expandedOrderIds.add(orderId)
            }
            currentState.copy(expandedOrderIds = expandedOrderIds)
        }
    }

    fun selectOrder(order: Order) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            try {
                // Call repository to accept order
                _uiState.update { currentState ->
                    val updatedOrders = currentState.orders.map { order ->
                        if (order.id == orderId) {
                            order.copy(status = OrderStatus.ACCEPTED)
                        } else {
                            order
                        }
                    }
                    currentState.copy(orders = updatedOrders)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to accept order") }
            }
        }
    }

    // Simulated repository calls
    private suspend fun fetchEarnings(): Earnings {
        delay(1000)
        return Earnings(
            todayEarnings = 1250.0,
            lastWeekEarnings = 8450.0,
            percentageChange = 15.0,
            trips = 5,
            hours = 6,
            distance = 120.0
        )
    }

    private suspend fun fetchStats(): Stats {
        delay(2000)
        return Stats(
            rating = 4.8,
            acceptanceRate = 85,
            completionRate = 98
        )
    }

    private suspend fun fetchOrders(): List<Order> {
        val sampleOrders =
            (1..5).map { index ->
                Order(
                    id = "order_$index",
                    pickup = "Pickup Location $index",
                    dropoff = "Dropoff Location $index",
                    distance = 5.0,
                    price = 150,
                    status = OrderStatus.PENDING
                )
            }
        return sampleOrders
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DriverHomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val earnings: Earnings? = null,
    val stats: Stats? = null,
    val orders: List<Order> = emptyList(),
    val expandedOrderIds: Set<String> = emptySet(),
    val selectedOrder: Order? = null,
    val error: String? = null
)

data class Earnings(
    val todayEarnings: Double,
    val lastWeekEarnings: Double,
    val percentageChange: Double,
    val trips: Int,
    val hours: Int,
    val distance: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class Stats(
    val rating: Double,
    val acceptanceRate: Int,
    val completionRate: Int
)

data class Order(
    val id: String,
    val pickup: String,
    val dropoff: String,
    val distance: Double,
    val price: Int,
    val vehicleType: String = "Mini Truck",
    val weight: String = "250 kg",
    val timeEstimate: String = "45 mins",
    val paymentMode: String = "Online",
    val additionalNotes: String = "Fragile items, handle with care",
    val status: OrderStatus = OrderStatus.PENDING,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class OrderStatus {
    PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}