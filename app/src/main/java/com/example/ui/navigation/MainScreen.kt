package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TenantRepository
import com.example.ui.crm.CrmScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.pos.PosScreen
import com.example.ui.report.ReportScreen
import com.example.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Pos : Screen("pos", "Kasir", Icons.Default.PointOfSale)
    object Inventory : Screen("inventory", "Stok", Icons.Default.Inventory)
    object Crm : Screen("crm", "Pelanggan", Icons.Default.People)
    object Report : Screen("report", "Laporan", Icons.Default.Assessment)
    object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSignOut: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TenantRepository(context) }
    val scope = rememberCoroutineScope()

    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Pos) }

    val products by repository.observeProducts().collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by repository.observeCustomers().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by repository.observeTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    val stockIns by repository.observeStockIns().collectAsStateWithLifecycle(initialValue = emptyList())

    val items = listOf(
        Screen.Pos,
        Screen.Inventory,
        Screen.Crm,
        Screen.Report,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedScreen) {
                Screen.Pos -> PosScreen(
                    products = products,
                    customers = customers,
                    onCheckout = { tx ->
                        scope.launch {
                            try {
                                repository.checkoutTransaction(tx)
                            } catch (e: Exception) {
                                // handle error
                            }
                        }
                    }
                )
                Screen.Inventory -> InventoryScreen(
                    products = products,
                    stockIns = stockIns,
                    onAddProduct = { prod ->
                        scope.launch { repository.addProduct(prod) }
                    },
                    onUpdateProduct = { prod ->
                        scope.launch { repository.updateProduct(prod) }
                    },
                    onDeleteProduct = { id ->
                        scope.launch { repository.deleteProduct(id) }
                    },
                    onAddStockIn = { stockIn ->
                        scope.launch { repository.addStockIn(stockIn) }
                    }
                )
                Screen.Crm -> CrmScreen(
                    customers = customers,
                    onAddCustomer = { cust ->
                        scope.launch { repository.addCustomer(cust) }
                    }
                )
                Screen.Report -> ReportScreen(
                    products = products,
                    customers = customers,
                    transactions = transactions
                )
                Screen.Settings -> SettingsScreen(
                    onSignOut = onSignOut
                )
            }
        }
    }
}
