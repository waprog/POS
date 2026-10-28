package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Umum",
    val barcode: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 5,
    val userId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val points: Int = 0,
    val totalSpent: Double = 0.0,
    val visitCount: Int = 0,
    val userId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
) {
    val subtotal: Double get() = price * quantity
}

data class Transaction(
    @DocumentId val id: String = "",
    val invoiceNo: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "Tunai",
    val customerId: String? = null,
    val customerName: String = "Umum",
    val cashierName: String = "Kasir",
    val userId: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

data class StockIn(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantityAdded: Int = 0,
    val supplier: String = "",
    val userId: String = "",
    @ServerTimestamp val date: Timestamp? = null
)
