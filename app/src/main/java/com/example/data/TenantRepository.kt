package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Customer
import com.example.model.Product
import com.example.model.StockIn
import com.example.model.Transaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TenantRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in.")
    }

    // --- PRODUCTS ---
    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Product::class.java) }
        )
    }

    suspend fun addProduct(product: Product) {
        val uid = requireUserId()
        val data = mapOf(
            "name" to product.name,
            "category" to product.category,
            "barcode" to product.barcode,
            "buyPrice" to product.buyPrice,
            "sellPrice" to product.sellPrice,
            "stock" to product.stock,
            "minStock" to product.minStock,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("products").add(data).await()
    }

    suspend fun updateProduct(product: Product) {
        val uid = requireUserId()
        require(product.id.isNotBlank())
        val ref = db.collection("users").document(uid).collection("products").document(product.id)
        val data = mapOf(
            "name" to product.name,
            "category" to product.category,
            "barcode" to product.barcode,
            "buyPrice" to product.buyPrice,
            "sellPrice" to product.sellPrice,
            "stock" to product.stock,
            "minStock" to product.minStock,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
    }

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("products").document(productId).delete().await()
    }

    // --- STOCK IN ---
    fun observeStockIns(): Flow<List<StockIn>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("stockIns")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StockIn::class.java) }
        )
    }

    suspend fun addStockIn(stockIn: StockIn) {
        val uid = requireUserId()
        val batch = db.batch()
        val stockInRef = db.collection("users").document(uid).collection("stockIns").document()
        val stockInData = mapOf(
            "productId" to stockIn.productId,
            "productName" to stockIn.productName,
            "quantityAdded" to stockIn.quantityAdded,
            "supplier" to stockIn.supplier,
            "userId" to uid,
            "date" to FieldValue.serverTimestamp()
        )
        batch.set(stockInRef, stockInData)

        val productRef = db.collection("users").document(uid).collection("products").document(stockIn.productId)
        batch.update(productRef, "stock", FieldValue.increment(stockIn.quantityAdded.toLong()))

        batch.commit().await()
    }

    // --- CUSTOMERS (CRM) ---
    fun observeCustomers(): Flow<List<Customer>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Customer::class.java) }
        )
    }

    suspend fun addCustomer(customer: Customer) {
        val uid = requireUserId()
        val data = mapOf(
            "name" to customer.name,
            "phone" to customer.phone,
            "email" to customer.email,
            "points" to customer.points,
            "totalSpent" to customer.totalSpent,
            "visitCount" to customer.visitCount,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("customers").add(data).await()
    }

    suspend fun updateCustomer(customer: Customer) {
        val uid = requireUserId()
        require(customer.id.isNotBlank())
        val ref = db.collection("users").document(uid).collection("customers").document(customer.id)
        val data = mapOf(
            "name" to customer.name,
            "phone" to customer.phone,
            "email" to customer.email,
            "points" to customer.points,
            "totalSpent" to customer.totalSpent,
            "visitCount" to customer.visitCount,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
    }

    // --- TRANSACTIONS ---
    fun observeTransactions(): Flow<List<Transaction>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
        )
    }

    suspend fun checkoutTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val batch = db.batch()

        val txRef = db.collection("users").document(uid).collection("transactions").document()
        val txData = mapOf(
            "invoiceNo" to transaction.invoiceNo,
            "items" to transaction.items,
            "totalAmount" to transaction.totalAmount,
            "paymentMethod" to transaction.paymentMethod,
            "customerId" to transaction.customerId,
            "customerName" to transaction.customerName,
            "cashierName" to transaction.cashierName,
            "userId" to uid,
            "timestamp" to FieldValue.serverTimestamp()
        )
        batch.set(txRef, txData)

        for (item in transaction.items) {
            val prodRef = db.collection("users").document(uid).collection("products").document(item.productId)
            batch.update(prodRef, "stock", FieldValue.increment(-item.quantity.toLong()))
        }

        if (!transaction.customerId.isNullOrBlank()) {
            val custRef = db.collection("users").document(uid).collection("customers").document(transaction.customerId)
            val earnedPoints = (transaction.totalAmount / 10000).toInt()
            batch.update(custRef,
                "totalSpent", FieldValue.increment(transaction.totalAmount),
                "visitCount", FieldValue.increment(1),
                "points", FieldValue.increment(earnedPoints.toLong())
            )
        }

        batch.commit().await()
    }
}
