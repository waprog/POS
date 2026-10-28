package com.example.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CartItem
import com.example.model.Customer
import com.example.model.Product
import com.example.model.Transaction
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    products: List<Product>,
    customers: List<Customer>,
    onCheckout: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showCartDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val categories = listOf("Semua") + products.map { it.category }.distinct()

    val filteredProducts = products.filter { p ->
        val matchCategory = selectedCategory == "Semua" || p.category == selectedCategory
        val matchQuery = p.name.contains(searchQuery, ignoreCase = true) || p.barcode.contains(searchQuery, ignoreCase = true)
        matchCategory && matchQuery
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val cartTotal = cartItems.sumOf { it.subtotal }
    val cartCount = cartItems.sumOf { it.quantity }

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                showScanner = false
                val found = products.find { it.barcode == barcode }
                if (found != null) {
                    val existing = cartItems.find { it.productId == found.id }
                    if (existing != null) {
                        cartItems = cartItems.map {
                            if (it.productId == found.id) it.copy(quantity = it.quantity + 1) else it
                        }
                    } else {
                        cartItems = cartItems + CartItem(
                            productId = found.id,
                            productName = found.name,
                            price = found.sellPrice,
                            quantity = 1
                        )
                    }
                }
            },
            onDismiss = { showScanner = false }
        )
        return
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            cartItems = cartItems,
            subtotal = cartTotal,
            selectedCustomer = selectedCustomer,
            customers = customers,
            onSelectCustomer = { selectedCustomer = it },
            onConfirmCheckout = { paymentMethod, cust ->
                val tx = Transaction(
                    invoiceNo = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
                    items = cartItems,
                    totalAmount = cartTotal,
                    paymentMethod = paymentMethod,
                    customerId = cust?.id,
                    customerName = cust?.name ?: "Umum",
                    cashierName = "Kasir Utama"
                )
                onCheckout(tx)
                cartItems = emptyList()
                selectedCustomer = null
                showCheckoutDialog = false
            },
            onDismiss = { showCheckoutDialog = false }
        )
    }

    if (showCartDialog) {
        CartBottomSheet(
            cartItems = cartItems,
            onUpdateQuantity = { productId, qty ->
                if (qty <= 0) {
                    cartItems = cartItems.filter { it.productId != productId }
                } else {
                    cartItems = cartItems.map {
                        if (it.productId == productId) it.copy(quantity = qty) else it
                    }
                }
            },
            onClearCart = { cartItems = emptyList() },
            onProceedToCheckout = {
                showCartDialog = false
                showCheckoutDialog = true
            },
            onDismiss = { showCartDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir (POS)", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            )
        },
        bottomBar = {
            if (cartCount > 0) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.clickable { showCartDialog = true }) {
                            Text("$cartCount Item di Keranjang", style = MaterialTheme.typography.bodySmall)
                            Text(currencyFormat.format(cartTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showCheckoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bayar Sekarang")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama produk atau barcode...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada produk ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { product ->
                        ProductPosCard(
                            product = product,
                            onClick = {
                                if (product.stock > 0) {
                                    val existing = cartItems.find { it.productId == product.id }
                                    if (existing != null) {
                                        cartItems = cartItems.map {
                                            if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it
                                        }
                                    } else {
                                        cartItems = cartItems + CartItem(
                                            productId = product.id,
                                            productName = product.name,
                                            price = product.sellPrice,
                                            quantity = 1
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPosCard(product: Product, onClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val isOutStock = product.stock <= 0

    Card(
        onClick = onClick,
        enabled = !isOutStock,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currencyFormat.format(product.sellPrice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOutStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOutStock) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("Habis", color = MaterialTheme.colorScheme.onError, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val total = cartItems.sumOf { it.subtotal }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keranjang Belanja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClearCart) {
                    Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, fontWeight = FontWeight.SemiBold)
                            Text(currencyFormat.format(item.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateQuantity(item.productId, item.quantity - 1) }) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang")
                            }
                            Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            IconButton(onClick = { onUpdateQuantity(item.productId, item.quantity + 1) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium)
                Text(currencyFormat.format(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onProceedToCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Lanjut ke Pembayaran", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    cartItems: List<CartItem>,
    subtotal: Double,
    selectedCustomer: Customer?,
    customers: List<Customer>,
    onSelectCustomer: (Customer?) -> Unit,
    onConfirmCheckout: (String, Customer?) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var cashPaid by remember { mutableStateOf("") }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showReceiptPreview by remember { mutableStateOf<Transaction?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val discountPercent = if (selectedCustomer != null) {
        when {
            selectedCustomer.visitCount >= 10 -> 0.10
            selectedCustomer.visitCount >= 5 -> 0.05
            else -> 0.0
        }
    } else {
        0.0
    }

    val discountAmount = subtotal * discountPercent
    val finalTotal = subtotal - discountAmount
    val cashDouble = cashPaid.toDoubleOrNull() ?: 0.0
    val change = cashDouble - finalTotal

    if (showReceiptPreview != null) {
        ReceiptPreviewDialog(
            transaction = showReceiptPreview!!,
            customer = selectedCustomer,
            onDismiss = {
                showReceiptPreview = null
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkout Pembayaran") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = { showCustomerDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pelanggan (CRM)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = selectedCustomer?.name ?: "Pilih Pelanggan / Umum",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedCustomer != null) {
                                Text(
                                    text = "Poin: ${selectedCustomer.points} | Kunjungan: ${selectedCustomer.visitCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                if (showCustomerDropdown) {
                    Surface(
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                TextButton(onClick = {
                                    onSelectCustomer(null)
                                    showCustomerDropdown = false
                                }) {
                                    Text("Umum (Tanpa Pelanggan)")
                                }
                            }
                            items(customers) { cust ->
                                TextButton(onClick = {
                                    onSelectCustomer(cust)
                                    showCustomerDropdown = false
                                }) {
                                    Text("${cust.name} (${cust.phone})")
                                }
                            }
                        }
                    }
                }

                Divider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal")
                    Text(currencyFormat.format(subtotal))
                }
                if (discountPercent > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon Loyalty (${(discountPercent * 100).toInt()}%)", color = MaterialTheme.colorScheme.primary)
                        Text("- ${currencyFormat.format(discountAmount)}", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Akhir", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(currencyFormat.format(finalTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }

                Divider()

                Text("Metode Pembayaran", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tunai", "QRIS", "Transfer").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (paymentMethod == "Tunai") {
                    OutlinedTextField(
                        value = cashPaid,
                        onValueChange = { cashPaid = it },
                        label = { Text("Uang Tunai Diterima") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (cashDouble >= finalTotal) {
                        Text(
                            text = "Kembalian: ${currencyFormat.format(change)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tx = Transaction(
                        invoiceNo = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
                        items = cartItems,
                        totalAmount = finalTotal,
                        paymentMethod = paymentMethod,
                        customerId = selectedCustomer?.id,
                        customerName = selectedCustomer?.name ?: "Umum",
                        cashierName = "Kasir Utama"
                    )
                    onConfirmCheckout(paymentMethod, selectedCustomer)
                    showReceiptPreview = tx
                },
                enabled = paymentMethod != "Tunai" || cashDouble >= finalTotal
            ) {
                Text("Proses & Cetak Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ReceiptPreviewDialog(
    transaction: Transaction,
    customer: Customer?,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Struk Pembayaran (Thermal Bluetooth)") },
        text = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("=== TENANT POS ===", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Jl. Bisnis Raya No. 88, Jakarta", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No. Inv: ${transaction.invoiceNo}", style = MaterialTheme.typography.bodySmall)
                        Text("Kasir: ${transaction.cashierName}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Pelanggan: ${transaction.customerName}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    transaction.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} x${item.quantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(currencyFormat.format(item.subtotal), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(transaction.totalAmount), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Metode: ${transaction.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: LUNAS", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Terima Kasih atas Kunjungan Anda!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kirim ke Printer Thermal")
            }
        }
    )
}
