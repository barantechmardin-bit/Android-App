package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.ErpRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.viewmodel.ErpViewModel
import com.example.ui.viewmodel.ErpViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ErpViewModel
    private val barcodeBuffer = StringBuilder()

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null) {
            val char = event.unicodeChar.toChar()
            if (char.isLetterOrDigit()) {
                barcodeBuffer.append(char)
            } else if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (barcodeBuffer.isNotEmpty()) {
                    val scannedCode = barcodeBuffer.toString().trim()
                    viewModel.updateBarcodeSearchQuery(scannedCode)
                    viewModel.triggerBarcodeScanNotification(scannedCode)
                    barcodeBuffer.setLength(0) // clear buffer
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialise local Room database
        val database = AppDatabase.getDatabase(this)

        // 2. Wrap database in repository with simulated Firestore sync logs
        val repository = ErpRepository(database.erpDao())

        // 3. Obtain ERP ViewModel via Factory Provider
        viewModel = ViewModelProvider(
            this,
            ErpViewModelFactory(repository)
        )[ErpViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val autoNotification by viewModel.autoMessageNotification.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isTablet = maxWidth >= 600.dp

                        if (isTablet) {
                            // LARGE SCREEN (Masaüstü Sürüm): Top Titlebar + Left menu, right workspace
                            Column(modifier = Modifier.fillMaxSize()) {
                                DesktopSimulatedTitleBar(viewModel)
                                
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    LeftSidebarMenu(
                                        currentTab = currentTab,
                                        onTabSelected = { viewModel.selectTab(it) },
                                        isDarkMode = isDarkMode,
                                        onThemeToggle = { viewModel.toggleTheme() },
                                        viewModel = viewModel
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        ActiveScreenContent(tabName = currentTab, viewModel = viewModel)
                                    }
                                }
                            }
                        } else {
                            // MOBILE SCREEN: Top Header, Bottom Navigation Workspace
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    MobileTopBar(
                                        currentTabName = currentTab,
                                        isDarkMode = isDarkMode,
                                        onThemeToggle = { viewModel.toggleTheme() }
                                    )
                                },
                                bottomBar = {
                                    MobileBottomNavigationBar(
                                        currentTab = currentTab,
                                        onTabSelected = { viewModel.selectTab(it) }
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    ActiveScreenContent(tabName = currentTab, viewModel = viewModel)
                                }
                            }
                        }

                        // WhatsApp / Telegram message notification banner overlay
                        AnimatedVisibility(
                            visible = autoNotification != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            autoNotification?.let { msg ->
                                NotificationBanner(
                                    messageText = msg,
                                    onCloseClicked = { viewModel.clearNotification() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopSimulatedTitleBar(viewModel: ErpViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // macOS Style Traffic Lights (Window controls)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button (Red)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFC5753))
                    .clickable { 
                        viewModel.triggerBarcodeScanNotification("🔴 Kapatma Algılandı: Masaüstü programından çıkmak için Alt+F4 kullanın veya sistem tepsisinden kapatın.")
                    }
            )
            // Minimize Button (Yellow)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDBC40))
                    .clickable { 
                        viewModel.triggerBarcodeScanNotification("🟡 Simge Durumu: Program görev çubuğuna küçültüldü ve dükkan barkod dinleyicici aktif kalmaya devam ediyor.")
                    }
            )
            // Maximize Button (Green)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF33C748))
                    .clickable { 
                        viewModel.triggerBarcodeScanNotification("🟢 Pencere Yönetimi: Uygulama pencere genişliği usta monitörüne göre optimize edildi!")
                    }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "DESKTOP MODE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // App Title
        Text(
            text = "BaranTech Bilişim Masaüstü ERP Portal — Windows & macOS Pro v1.2.0 (Çevrimdışı/Bulut Senkronize)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Windows Style Window Buttons (Right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Text(
                    text = "Lokal Port Bağlantısı: Aktif (Com3)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        viewModel.triggerBarcodeScanNotification("🟡 Simge Durumu: Pencere simge durumuna küçültüldü.")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Minimize",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = { 
                        viewModel.triggerBarcodeScanNotification("🟢 Pencere Yönetimi: Tam Ekran / Optimize Boyut arasında geçiş yapıldı!")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CropSquare,
                        contentDescription = "Maximize",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = { 
                        viewModel.triggerBarcodeScanNotification("🔴 Çıkış: Masaüstü yerel ERP program kapatma işlemi onay bekliyor.")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// Sidebar Navigation Left for Desktop / Tablet view classes
@Composable
fun LeftSidebarMenu(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: ErpViewModel
) {
    val logs by viewModel.syncLogs.collectAsState()

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(0.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Enterprise ERP Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AppSettingsAlt,
                        contentDescription = "Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "TEKNIK SERVIS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "Cari & Finans ERP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu Items List
            val menuItems = listOf(
                NavigationMenuItem(id = "DASHBOARD", label = "Dashboard Panel", icon = Icons.Filled.Dashboard, testTag = "nav_dashboard"),
                NavigationMenuItem(id = "CARI", label = "Cari Listesi", icon = Icons.Filled.People, testTag = "nav_cari"),
                NavigationMenuItem(id = "JOB_BOARD", label = "Onarım Board", icon = Icons.Filled.Hardware, testTag = "nav_jobs"),
                NavigationMenuItem(id = "OPERATIONS", label = "İşlem Kataloğu", icon = Icons.Filled.ListAlt, testTag = "nav_operations"),
                NavigationMenuItem(id = "STOCK", label = "Stok & Tedarikçi", icon = Icons.Filled.Warehouse, testTag = "nav_stok"),
                NavigationMenuItem(id = "LEDGER", label = "Kasa & Banka", icon = Icons.Filled.AccountBalance, testTag = "nav_ledger")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                menuItems.forEach { item ->
                    val isActive = currentTab == item.id
                    val itemBg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    val itemTextColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBg)
                            .clickable { onTabSelected(item.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag(item.testTag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = itemTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = itemTextColor
                        )
                    }
                }
            }
        }

        // Sidebar Footer options & Theme transition togglers
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Heartbeat live sensor card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Column {
                        Text("Cloud Firestore", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Real-Time Listeners Connected", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }

            // Theme switch action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onThemeToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("toggle_dark_mode"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = "Theme",
                        tint = if (isDarkMode) PrimaryTeal else AccentOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isDarkMode) "Koyu Tema Aktif" else "Açık Tema Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) PrimaryTeal else AccentOrange)
                )
            }
        }
    }
}

// Mobile top scaffold header
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTopBar(currentTabName: String, isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val cleanName = when (currentTabName) {
        "DASHBOARD" -> "Dashboard ERP"
        "CARI" -> "Cari Kartlar"
        "JOB_BOARD" -> "İş Takip Panosu"
        "OPERATIONS" -> "Servis Kataloğu"
        "STOCK" -> "Stok / Tedarikçi"
        else -> "Finans & Gelir-Gider"
    }

    TopAppBar(
        title = {
            Text(
                text = cleanName, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            IconButton(
                onClick = onThemeToggle,
                modifier = Modifier.testTag("mobile_toggle_dark_mode")
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = "Theme Toggle",
                    tint = if (isDarkMode) PrimaryTeal else AccentOrange
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

// Mobile bottom navigation bar layout
@Composable
fun MobileBottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val items = listOf(
        NavigationMenuItem(id = "DASHBOARD", label = "Ana Ekran", icon = Icons.Filled.Dashboard, testTag = "mob_dashboard"),
        NavigationMenuItem(id = "CARI", label = "Cariler", icon = Icons.Filled.People, testTag = "mob_cari"),
        NavigationMenuItem(id = "JOB_BOARD", label = "Onarımlar", icon = Icons.Filled.Hardware, testTag = "mob_jobs"),
        NavigationMenuItem(id = "STOCK", label = "Stok", icon = Icons.Filled.Warehouse, testTag = "mob_stok"),
        NavigationMenuItem(id = "LEDGER", label = "Kasa/Banka", icon = Icons.Filled.AccountBalance, testTag = "mob_ledger")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding() // Notch & Navigation Bar Safe Area rules!
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.id
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.id) },
                icon = { Icon(item.icon, contentDescription = item.label, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray) },
                label = { Text(item.label, fontSize = 9.sp) },
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

// Render active screen based on selected nav tab
@Composable
fun ActiveScreenContent(tabName: String, viewModel: ErpViewModel) {
    when (tabName) {
        "DASHBOARD" -> DashboardScreen(viewModel = viewModel)
        "CARI" -> CariScreen(viewModel = viewModel)
        "JOB_BOARD" -> ServisScreen(viewModel = viewModel)
        "OPERATIONS" -> IslemScreen(viewModel = viewModel)
        "STOCK" -> StokScreen(viewModel = viewModel)
        "LEDGER" -> FinansScreen(viewModel = viewModel)
        else -> DashboardScreen(viewModel = viewModel)
    }
}

// Customized success alerts SMS simulator banner
@Composable
fun NotificationBanner(messageText: String, onCloseClicked: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_whatsapp_sms"),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "SmsNotify",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = messageText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            IconButton(onClick = onCloseClicked) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

// Pure data model mapping sidebar items
data class NavigationMenuItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
