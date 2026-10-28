package com.example.ui.inventory

import androidx.compose.foundation.layout.*
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
import com.example.model.Product
import com.example.model.StockIn
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    stockIns: List<StockIn>,
    onAddProduct: (Product) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAddStockIn: (StockIn) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    if (showAddProductDialog || editingProduct != null) {
        AddProductDialog(
            productToEdit = editingProduct,
            onSave = { prod ->
                if (editingProduct != null) {
                    onUpdateProduct(prod)
                } else {
                    onAddProduct(prod)
                }
                showAddProductDialog = false
                editingProduct = null
            },
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    if (showStockInDialog) {
        StockInDialog(
            products = products,
            onSave = { stockIn ->
                onAddStockIn(stockIn)
                showStockInDialog = false
            },
            onDismiss = { showStockInDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Stok & Produk", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddProductDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Produk")
                        }
                    } else {
                        IconButton(onClick = { showStockInDialog = true }) {
                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Tambah Stok Masuk")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddProductDialog = true else showStockInDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Daftar Produk") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat Stok Masuk") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada produk. Tambahkan produk baru.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Kategori: ${product.category} | Barcode: ${product.barcode.ifEmpty { '-' }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Beli: ${currencyFormat.format(product.buyPrice)} | Jual: ${currencyFormat.format(product.sellPrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Stok: ${product.stock}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            if (product.stock <= product.minStock) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                    Text("Stok Menipis", color = MaterialTheme.colorScheme.onError, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { editingProduct = product }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { onDeleteProduct(product.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (stockIns.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat stok masuk.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stockIns) { stockIn ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stockIn.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("+${stockIn.quantityAdded} Unit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Supplier: ${stockIn.supplier.ifEmpty { '-' }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    productToEdit: Product?,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "Umum") }
    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }
    var buyPrice by remember { mutableStateOf(productToEdit?.buyPrice?.toString() ?: "") }
    var sellPrice by remember { mutableStateOf(productToEdit?.sellPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(productToEdit?.minStock?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (productToEdit == null) "Tambah Produk Baru" else "Edit Produk") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Produk") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Harga Beli (Modal)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it }, label = { Text("Harga Jual") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok Awal") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Minimum Stok Peringatan") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = Product(
                        id = productToEdit?.id ?: "",
                        name = name,
                        category = category,
                        barcode = barcode,
                        buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                        sellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        minStock = minStock.toIntOrNull() ?: 5
                    )
                    onSave(p)
                },
                enabled = name.isNotBlank() && sellPrice.toDoubleOrNull() != null
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun StockInDialog(
    products: List<Product>,
    onSave: (StockIn) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var quantity by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Stok Masuk (Supplier)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = { showDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedProduct?.name ?: "Pilih Produk")
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                if (showDropdown) {
                    Surface(
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn {
                            items(products) { p ->
                                TextButton(onClick = {
                                    selectedProduct = p
                                    showDropdown = false
                                }) {
                                    Text("${p.name} (Stok: ${p.stock})")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah Masuk") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Nama Supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProduct != null && quantity.toIntOrNull() != null) {
                        val stockIn = StockIn(
                            productId = selectedProduct!!.id,
                            productName = selectedProduct!!.name,
                            quantityAdded = quantity.toIntOrNull() ?: 0,
                            supplier = supplier
                        )
                        onSave(stockIn)
                    }
                },
                enabled = selectedProduct != null && quantity.toIntOrNull() != null && (quantity.toIntOrNull() ?: 0) > 0
            ) {
                Text("Tambah Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
