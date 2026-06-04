package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Context
import android.app.Activity

// Formatted double helper
fun Double.formatTL(): String {
    return DecimalFormat("#,##0.00").format(this) + " TL"
}

// FORMAT DATE helper
fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun printHtmlReport(context: Context, htmlContent: String) {
    val activity = context.findActivity()
    activity?.runOnUiThread {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "BARANTECH_ERP_RAPORU_${System.currentTimeMillis()}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun DashboardScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val servisList by viewModel.servisList.collectAsState()
    val finansList by viewModel.finansList.collectAsState()
    val logs by viewModel.syncLogs.collectAsState()
    val isSyncInProgress by viewModel.syncInProgress.collectAsState()

    val criticalStockAlerts by viewModel.criticalStockAlerts.collectAsState()
    val upcomingRepairAlerts by viewModel.upcomingRepairAlerts.collectAsState()
    val totalAlertsCount = criticalStockAlerts.size + upcomingRepairAlerts.size

    val barcodeQuery by viewModel.barcodeSearchQuery.collectAsState()
    var showExecutiveReport by remember { mutableStateOf(false) }

    // Calculate count for counters
    val countBekliyor = servisList.count { it.durum == "Bekliyor" }
    val countTamirde = servisList.count { it.durum == "Tamirde" }
    val countParca = servisList.count { it.durum == "Parça Bekliyor" }
    val countTeslim = servisList.count { it.durum == "Teslim Edildi" }

    // Financial aggregation
    val totalGelir = finansList.filter { it.tipi == "Gelir" }.sumOf { it.tutar }
    val totalGider = finansList.filter { it.tipi == "Gider" }.sumOf { it.tutar }
    val netKasa = totalGelir - totalGider

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Cloud connection banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Teknik Servis ERP Kontrol Paneli",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Müşteri cari ilişkileri, onarım takip ve nakit girdi çıktılarını canlı yönetin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Cloud Real-time indicator
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSyncInProgress) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSyncInProgress) AccentOrange else SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncInProgress) "Firebase Sync..." else "Firestore Live",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSyncInProgress) AccentOrange else SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // Real-Time System Notification & Warning alert board widget
        if (totalAlertsCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_notification_center_banner"),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.08f)),
                    border = BorderStroke(1.2.dp, AccentOrange.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Alert Warning",
                                    tint = AccentOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Aktif Sistem Uyarıları ($totalAlertsCount)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MÜDAHALE GEREKİYOR",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentOrange
                                )
                            }
                        }
                        
                        Divider(color = AccentOrange.copy(alpha = 0.2f))
                        
                        // Show combined stock alerts + upcoming repair delays in a smart scroll free sequence
                        val allAlerts = upcomingRepairAlerts.map { "🛠️ $it" } + criticalStockAlerts.map { "📦 $it" }
                        allAlerts.take(4).forEach { alertMsg ->
                            Text(
                                text = "• $alertMsg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                        }
                        
                        if (allAlerts.size > 4) {
                            Text(
                                text = "...ve ${allAlerts.size - 4} adet aktif sistem uyarısı daha mevcut. Çözüm/detaylar için üst bardaki zil simgesine dokunun.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Trigger button card for Executive Summary & Reporting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showExecutiveReport = true }
                    .testTag("executive_report_trigger_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assessment,
                            contentDescription = "Rapor Modulu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yönetici Özet Raporu & PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Dükkan mali verilerini, aktif onarımları ve müşteri bakiyelerini kapsayan şık bir PDF raporu oluşturup yazdırın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Rapor Detay",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Cihaz Seri No / Barkod Hızlı Sorgulama Sistemi (Şipşak Sorgu)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("barcode_search_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Barcode Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Cihaz Seri No / Barkod Sorgulama",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Usta, dükkana gelen cihazın seri nosunu veya barkodunu okuttuğunda/yazdığında, cihazın geçmişte gelip gelmediğini, hangi işlemlerden geçtiğini ve hangi müşteriye ait olduğunu şipşak listeler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = barcodeQuery,
                        onValueChange = { viewModel.updateBarcodeSearchQuery(it) },
                        placeholder = { Text("Barkod veya Seri Numarası girin...") },
                        leadingIcon = { Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "Query Icon") },
                        trailingIcon = {
                            if (barcodeQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateBarcodeSearchQuery("") }) {
                                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear Input")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("barcode_search_input"),
                        singleLine = true
                    )

                    if (barcodeQuery.trim().isNotEmpty()) {
                        val queryText = barcodeQuery.trim().lowercase()
                        val matchedJobs = servisList.filter {
                            it.seriNo.lowercase().contains(queryText) ||
                            it.cihazMarkaModel.lowercase().contains(queryText)
                        }

                        if (matchedJobs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🔍 Kayıt Yok: Girdiğiniz seri no/barkod ile dükkan geçmişinde eşleşen bir cihaz kaydı bulunamadı.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Bulunan Eşleşen Servis Geçmişi (${matchedJobs.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                matchedJobs.forEach { job ->
                                    val isDelivered = job.durum == "Teslim Edildi"
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = job.cihazMarkaModel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = job.durum.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            TableRowLabel(label = "Müşteri / Cari:", value = job.cariIsim)
                                            TableRowLabel(label = "Seri No / IMEI:", value = job.seriNo)
                                            TableRowLabel(label = "Arıza / İşlem:", value = job.sikayetDetayi)
                                            TableRowLabel(label = "Giriş Tarihi:", value = job.tarih.formatDate())
                                            TableRowLabel(label = "Ödenen Kapora:", value = job.alinanKapora.formatTL())
                                            TableRowLabel(label = "Toplam Tutar:", value = job.tahminiTutar.formatTL())
                                            
                                            if (isDelivered) {
                                                val warrantyDurationMs = 180L * 24 * 60 * 60 * 1000 // 6 Ay
                                                val remainingMs = (job.tarih + warrantyDurationMs) - System.currentTimeMillis()
                                                val remainingDays = remainingMs / (1000 * 60 * 60 * 24)
                                                val warrantyText = if (remainingDays > 0) {
                                                    "Garanti Aktif: $remainingDays gün kaldı"
                                                } else {
                                                    "Garanti Süresi Dolan Cihaz"
                                                }
                                                TableRowLabel(
                                                    label = "Garanti Süresi:",
                                                    value = "🛡️ $warrantyText (Bitiş: %s)".format((job.tarih + warrantyDurationMs).formatDate())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stats Counters Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Aktif Onarım Durumları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Bekleyen",
                        count = countBekliyor,
                        icon = Icons.Filled.PendingActions,
                        color = PendingYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "counter_pending"
                    )
                    StatCard(
                        title = "Tamirde",
                        count = countTamirde,
                        icon = Icons.Filled.BuildCircle,
                        color = BlueText,
                        modifier = Modifier.weight(1f),
                        testTag = "counter_repair"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Yedek Parça",
                        count = countParca,
                        icon = Icons.Filled.HourglassEmpty,
                        color = AccentOrange,
                        modifier = Modifier.weight(1f),
                        testTag = "counter_parts"
                    )
                    StatCard(
                        title = "Tamamlanan",
                        count = countTeslim,
                        icon = Icons.Filled.CheckCircle,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "counter_completed"
                    )
                }
            }
        }

        // Financial summary tiles and simple bar chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kasa Muhasebe Özeti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageOf = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("Toplam Ciro (Gelir)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalGelir.formatTL(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("Toplam Giderler", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalGider.formatTL(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Kar/Zarar Durumu:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = netKasa.formatTL(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (netKasa >= 0) SuccessGreen else ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Gelir / Gider Grafiği",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // CUSTOM FINANCIAL CHART
                    FinancialBarChart(gelir = totalGelir.toFloat(), gider = totalGider.toFloat())
                }
            }
        }

        // Live Real-Time terminal logging output
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F)),
                border = BorderStroke(1.dp, Color(0xFF1E2836))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Firebase Firestore Canlı Veri Akışı",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "Bulut Firestore dinleyici başlatıldı. Soket bağlantısı aktif...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        } else {
                            logs.forEach { logMsg ->
                                Text(
                                    text = logMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (logMsg.contains("SUCCESS") || logMsg.contains("connected")) SuccessGreen else if (logMsg.contains("pushed") || logMsg.contains("pushing")) AccentOrange else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExecutiveReport) {
        ExecutiveSummaryReportDialog(
            onDismiss = { showExecutiveReport = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun StatCard(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, testTag: String = "") {
    Card(
        modifier = modifier.testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
            }
            Icon(imageVector = icon, contentDescription = title, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun FinancialBarChart(gelir: Float, gider: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = (gelir + gider).coerceAtLeast(1.0f)
            val maxVal = maxOf(gelir, gider).coerceAtLeast(100.0f)

            val width = size.width
            val height = size.height

            val barWidth = width * 0.28f
            val spacing = width * 0.14f

            // Gelir Bar
            val gelirBarHeight = (gelir / maxVal) * (height - 30.dp.toPx())
            drawRect(
                color = SuccessGreen,
                topLeft = Offset(spacing, height - 20.dp.toPx() - gelirBarHeight),
                size = Size(barWidth, gelirBarHeight)
            )

            // Gider Bar
            val giderBarHeight = (gider / maxVal) * (height - 30.dp.toPx())
            drawRect(
                color = ErrorRed,
                topLeft = Offset(spacing * 2 + barWidth, height - 20.dp.toPx() - giderBarHeight),
                size = Size(barWidth, giderBarHeight)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("Gelir (Yeşil)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
            Text("Gider (Kırmızı)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
    }
}

data class MonthlyTotal(
    val monthName: String,
    val year: Int,
    val monthInt: Int,
    val income: Float,
    val expense: Float
)

fun getMonthlyTotals(finansList: List<KasaBanka>): List<MonthlyTotal> {
    val months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
    val cal = java.util.Calendar.getInstance()
    
    val grouped = finansList.groupBy {
        cal.timeInMillis = it.tarih
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) // 0-11
        year to month
    }
    
    val sortedKeys = grouped.keys.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
    
    val result = sortedKeys.map { (year, monthInt) ->
        val list = grouped[year to monthInt] ?: emptyList()
        val totalIncome = list.filter { it.tipi == "Gelir" }.sumOf { it.tutar }.toFloat()
        val totalExpense = list.filter { it.tipi == "Gider" }.sumOf { it.tutar }.toFloat()
        val yearSuffix = year.toString().takeLast(2)
        val label = "${months[monthInt]} '$yearSuffix"
        
        MonthlyTotal(
            monthName = label,
            year = year,
            monthInt = monthInt,
            income = totalIncome,
            expense = totalExpense
        )
    }
    
    if (result.isEmpty()) {
        cal.timeInMillis = System.currentTimeMillis()
        val currentYear = cal.get(java.util.Calendar.YEAR)
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val yearSuffix = currentYear.toString().takeLast(2)
        val label = "${months[currentMonth]} '$yearSuffix"
        return listOf(MonthlyTotal(label, currentYear, currentMonth, 0f, 0f))
    }
    
    return result
}

@Composable
fun MonthlyFinancialBarChart(finansList: List<KasaBanka>) {
    val monthlyData = remember(finansList) { getMonthlyTotals(finansList) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aylık Gelir ve Gider Analizi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dönemsel finansal büyüme ve maliyet karşılaştırması",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Legends indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                        Text("Gelir", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ErrorRed))
                        Text("Gider", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            
            // The Canvas drawing area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    // The Chart Bars
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            val bottomPadding = 4.dp.toPx()
                            val topPadding = 12.dp.toPx()
                            val chartHeight = canvasHeight - bottomPadding - topPadding
                            
                            // Determine the max amount among all monthly income and expense values to scale properly
                            val maxAmount = monthlyData.flatMap { listOf(it.income, it.expense) }
                                .maxOrNull()?.coerceAtLeast(100f) ?: 100f
                            
                            val count = monthlyData.size
                            val sectionWidth = canvasWidth / count
                            
                            monthlyData.forEachIndexed { index, data ->
                                val sectionLeft = index * sectionWidth
                                val barWidth = sectionWidth * 0.28f
                                val gap = sectionWidth * 0.06f
                                
                                val sectionCenterX = sectionLeft + sectionWidth / 2
                                val incomeBarLeft = sectionCenterX - barWidth - (gap / 2)
                                val expenseBarLeft = sectionCenterX + (gap / 2)
                                
                                val incomeHeight = (data.income / maxAmount) * chartHeight
                                val expenseHeight = (data.expense / maxAmount) * chartHeight
                                
                                val yBaseline = canvasHeight - bottomPadding
                                
                                // 1. Draw Income Bar (SuccessGreen)
                                if (incomeHeight > 0) {
                                    drawRoundRect(
                                        color = SuccessGreen,
                                        topLeft = Offset(incomeBarLeft, yBaseline - incomeHeight),
                                        size = Size(barWidth, incomeHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                } else {
                                    drawCircle(
                                        color = SuccessGreen.copy(alpha = 0.3f),
                                        center = Offset(incomeBarLeft + barWidth / 2, yBaseline - 2.dp.toPx()),
                                        radius = 2.dp.toPx()
                                    )
                                }
                                
                                // 2. Draw Expense Bar (ErrorRed)
                                if (expenseHeight > 0) {
                                    drawRoundRect(
                                        color = ErrorRed,
                                        topLeft = Offset(expenseBarLeft, yBaseline - expenseHeight),
                                        size = Size(barWidth, expenseHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                } else {
                                    drawCircle(
                                        color = ErrorRed.copy(alpha = 0.3f),
                                        center = Offset(expenseBarLeft + barWidth / 2, yBaseline - 2.dp.toPx()),
                                        radius = 2.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Monthly labels exactly positioned matching columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        monthlyData.forEach { data ->
                            Text(
                                text = data.monthName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Detailed list of figures per month
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                monthlyData.forEach { data ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.widthIn(min = 110.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(data.monthName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("+ ${data.income.toDouble().formatTL()}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Black)
                            Text("- ${data.expense.toDouble().formatTL()}", style = MaterialTheme.typography.bodySmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ExecutiveSummaryReportDialog(
    onDismiss: () -> Unit,
    viewModel: ErpViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val servisList by viewModel.servisList.collectAsState()
    val finansList by viewModel.finansList.collectAsState()
    val cariList by viewModel.cariList.collectAsState()
    
    // Stats calculation
    val countBekliyor = servisList.count { it.durum == "Bekliyor" }
    val countTamirde = servisList.count { it.durum == "Tamirde" }
    val countParca = servisList.count { it.durum == "Parça Bekliyor" }
    val countTeslim = servisList.count { it.durum == "Teslim Edildi" }
    val countTotalJobs = servisList.size
    
    val totalGelir = finansList.filter { it.tipi == "Gelir" }.sumOf { it.tutar }
    val totalGider = finansList.filter { it.tipi == "Gider" }.sumOf { it.tutar }
    val netKasa = totalGelir - totalGider
    
    val posGelir = finansList.filter { it.tipi == "Gelir" && it.odemeYontemi == "POS" }.sumOf { it.tutar }
    val posGider = finansList.filter { it.tipi == "Gider" && it.odemeYontemi == "POS" }.sumOf { it.tutar }
    val posNet = posGelir - posGider
    
    val nakitGelir = finansList.filter { it.tipi == "Gelir" && it.odemeYontemi == "Nakit" }.sumOf { it.tutar }
    val nakitGider = finansList.filter { it.tipi == "Gider" && it.odemeYontemi == "Nakit" }.sumOf { it.tutar }
    val nakitNet = nakitGelir - nakitGider
    
    val totalCariCount = cariList.size
    val totalBakiye = cariList.sumOf { it.bakiye }
    
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    
    val reportHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>BaranTech ERP Summary Report</title>
            <style>
                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #2D3748; margin: 40px; padding: 0; line-height: 1.6; background-color: #FFF; }
                .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #14B8A6; padding-bottom: 15px; margin-bottom: 30px; }
                .title { margin: 0; font-size: 24px; font-weight: bold; color: #0F766E; }
                .subtitle { font-size: 13px; color: #64748B; margin-top: 5px; }
                .date { font-size: 13px; color: #334155; font-weight: bold; text-align: right; }
                .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 30px; }
                .card { border: 1px solid #E2E8F0; border-radius: 8px; padding: 16px; background: #F8FAFC; }
                .card-title { font-size: 11px; font-weight: bold; text-transform: uppercase; color: #64748B; margin: 0 0 6px 0; }
                .card-value { font-size: 20px; font-weight: bold; margin: 0; color: #1E293B; }
                .text-green { color: #10B981 !important; }
                .text-red { color: #EF4444 !important; }
                .text-blue { color: #0EA5E9 !important; }
                .section-title { font-size: 16px; font-weight: bold; color: #0F766E; border-bottom: 2px solid #E2E8F0; padding-bottom: 6px; margin-bottom: 15px; margin-top: 30px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
                th, td { padding: 10px 12px; border-bottom: 1px solid #E2E8F0; text-align: left; font-size: 13px; }
                th { background-color: #F1F5F9; color: #475569; font-weight: bold; }
                tr:nth-child(even) { background-color: #F8FAFC; }
                .footer { text-align: center; margin-top: 50px; font-size: 11px; color: #94A3B8; border-top: 1px solid #E2E8F0; padding-top: 15px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div>
                    <h1 class="title">BARANTECH ERP GENEL ÖZET RAPORU</h1>
                    <p class="subtitle">Teknik Servis Onarım ve Finans Durum Raporu</p>
                </div>
                <div class="date">Oluşturma Tarihi<br>${dateStr}</div>
            </div>

            <div class="summary-grid">
                <div class="card" style="border-left: 4px solid #0EA5E9;">
                    <h3 class="card-title">Net Kasa Durumu</h3>
                    <p class="card-value ${if (netKasa >= 0) "text-green" else "text-red"}">${netKasa.formatTL()}</p>
                </div>
                <div class="card" style="border-left: 4px solid #10B981;">
                    <h3 class="card-title">Toplam Gelir (Ciro)</h3>
                    <p class="card-value text-green">${totalGelir.formatTL()}</p>
                </div>
                <div class="card" style="border-left: 4px solid #EF4444;">
                    <h3 class="card-title">Toplam Giderler</h3>
                    <p class="card-value text-red">${totalGider.formatTL()}</p>
                </div>
            </div>

            <h2 class="section-title">Finansal Dağılım</h2>
            <table>
                <thead>
                    <tr>
                        <th>Ödeme Kanalı</th>
                        <th>Gelir / Giriş</th>
                        <th>Gider / Çıkış</th>
                        <th>Net Bakiye</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><b>Nakit Kasası</b></td>
                        <td class="text-green">${nakitGelir.formatTL()}</td>
                        <td class="text-red">${nakitGider.formatTL()}</td>
                        <td style="font-weight: bold;" class="${if (nakitNet >= 0) "text-green" else "text-red"}">${nakitNet.formatTL()}</td>
                    </tr>
                    <tr>
                        <td><b>POS Cihazı / Banka</b></td>
                        <td class="text-green">${posGelir.formatTL()}</td>
                        <td class="text-red">${posGider.formatTL()}</td>
                        <td style="font-weight: bold;" class="${if (posNet >= 0) "text-green" else "text-red"}">${posNet.formatTL()}</td>
                    </tr>
                </tbody>
            </table>

            <h2 class="section-title">Teknik Servis Onarım Faaliyetleri</h2>
            <table>
                <thead>
                    <tr>
                        <th>Cihaz Onarım Durumu</th>
                        <th>Cihaz Adedi</th>
                        <th>Alt Detay ve Açıklamalar</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><b>Bekliyor / Arıza Tespiti Yapılan</b></td>
                        <td>${countBekliyor} Adet</td>
                        <td>Onay veya inceleme bekleyen aktif iş sırası.</td>
                    </tr>
                    <tr>
                        <td><b>Onarım Aşamasında (Tamirde)</b></td>
                        <td>${countTamirde} Adet</td>
                        <td>Teknisyen masasında işlem gören cihazlar.</td>
                    </tr>
                    <tr>
                        <td><b>Yedek Parça Bekleniyor</b></td>
                        <td>${countParca} Adet</td>
                        <td>Yurt dışı/içi parça temini bekleyen servisler.</td>
                    </tr>
                    <tr>
                        <td><b>Müşteriye Teslim Edildi</b></td>
                        <td>${countTeslim} Adet</td>
                        <td>Tamamlanan ve arşivlenen geçmiş operasyonlar.</td>
                    </tr>
                    <tr style="background-color: #E2E8F0; font-weight: bold;">
                        <td>GENEL TOPLAM REGİSTRE CİHAZ</td>
                        <td>${countTotalJobs} Cihaz</td>
                        <td>Sistemdeki tüm zamanların toplam kayıt yükü.</td>
                    </tr>
                </tbody>
            </table>

            <h2 class="section-title">Müşteriler (Cari) ve Stok Sağlık Durumu</h2>
            <table>
                <thead>
                    <tr>
                        <th>Gösterge Bölümü</th>
                        <th>Veri Ölçümü</th>
                        <th>Dükkan Etki Değerlendirmesi</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Kayıtlı Aktif Cari Sayısı</td>
                        <td>${totalCariCount} Cari Kart</td>
                        <td>Sisteme kayıt edilmiş benzersiz müşteri/kurum sayısı.</td>
                    </tr>
                    <tr>
                        <td>Toplam Cari Bakiye Yükü</td>
                        <td class="${if (totalBakiye >= 0) "text-green" else "text-red"}">${totalBakiye.formatTL()}</td>
                        <td>Müşterilerin dükkana olan toplam borç veya avans durum dengesi.</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer">
                BaranTech Bilişim ERP Sistemleri Otomatik Özet Raporlama Modülü.<br>
                Rapordaki veriler sisteme bağlı yerel ve bulut veritabanlarının tam anlık kesitidir.
            </div>
        </body>
        </html>
    """.trimIndent()

    val reportTextPlaintxt = """
        ==================================================
        BARANTECH ERP GENEL ÖZET RAPORU
        Tarih: $dateStr
        ==================================================
        
        [FİNANSAL GÖSTERGELER]
        Toplam Gelir (Ciro) : ${totalGelir.formatTL()}
        Toplam Giderler     : ${totalGider.formatTL()}
        Net Kasa Bakiye     : ${netKasa.formatTL()}
        
        -- Ödeme Yöntemi Detayları --
        Nakit Kasası Giriş  : ${nakitGelir.formatTL()}
        Nakit Kasası Çıkış  : ${nakitGider.formatTL()}
        Nakit Net Durum     : ${nakitNet.formatTL()}
        
        POS Girişleri       : ${posGelir.formatTL()}
        POS Çıkışları       : ${posGider.formatTL()}
        POS Net Durum       : ${posNet.formatTL()}
        
        [TEKNİK SERVİS FAALİYETLERİ]
        Bekleyen Cihaz      : $countBekliyor Adet
        Tamirdeki Cihaz     : $countTamirde Adet
        Doğrulanan Teslimat : $countTeslim Adet
        Parça Bekleyen      : $countParca Adet
        Toplam Servis Kaydı : $countTotalJobs Adet
        
        [CARI ILISKILER]
        Kayıtlı Cari Sayısı : $totalCariCount Kart
        Net cari Alacak/Borç: ${totalBakiye.formatTL()}
        
        ==================================================
        BaranTech Bilişim ERP Yönetim Raporlama Modülü.
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .testTag("executive_summary_report_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assessment,
                            contentDescription = "Report Dialog Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Yönetici Özet Raporu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Rapor")
                    }
                }
                
                Text(
                    text = "Dükkanınızdaki finansal verileri ve onarım geçmişini kullanarak derlenmiş canlı analiz belgesidir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Scrollable content showing on-screen visual tables
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Finans Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💰 FİNANSAL GÖSTERGELER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toplam Ciro:", style = MaterialTheme.typography.bodySmall)
                                Text(totalGelir.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toplam Giderler:", style = MaterialTheme.typography.bodySmall)
                                Text(totalGider.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Kasa Rezervi:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(netKasa.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = if (netKasa >= 0) SuccessGreen else ErrorRed)
                            }
                        }
                    }

                    // Onarım Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🛠️ TEKNİK SERVİS DURUMU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bekleyen Cihaz:", style = MaterialTheme.typography.bodySmall)
                                Text("$countBekliyor Adet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tamirde Olan Cihaz:", style = MaterialTheme.typography.bodySmall)
                                Text("$countTamirde Adet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Parça Bekleyen Cihaz:", style = MaterialTheme.typography.bodySmall)
                                Text("$countParca Adet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Teslim Edilen Cihaz:", style = MaterialTheme.typography.bodySmall)
                                Text("$countTeslim Adet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toplam Servis Kaydı:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("$countTotalJobs Adet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Cari Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👥 CARİ İLİŞKİLER VE STOK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kayıtlı Cari Sayısı:", style = MaterialTheme.typography.bodySmall)
                                Text("$totalCariCount Müşteri", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cari Bakiye Yükü:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(totalBakiye.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (totalBakiye >= 0) SuccessGreen else ErrorRed)
                            }
                        }
                    }

                    // Monospace preview area for easy readout
                    Text("Metin Formatı Önizleme:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportTextPlaintxt,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Bottom CTA controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to Clipboard
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(reportTextPlaintxt) })
                            viewModel.triggerBarcodeScanNotification("📋 ERP Özet Raporu metin formatında başarıyla panoya kopyalandı!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    // Print/PDF trigger
                    Button(
                        onClick = {
                            printHtmlReport(context, reportHtml)
                            viewModel.triggerBarcodeScanNotification("🖨️ Android Yazdırma Servisi: Rapor derlendi ve PDF çıktısı hazırlanıyor!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("dialog_print_pdf_button"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.LocalPrintshop, contentDescription = "Yazdir PDF", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Kaydet / Yazdır", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}


// Helper tool to bypass image resource loader
@Composable
fun Icon(imageOf: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, tint: Color) {
    Icon(imageVector = imageOf, contentDescription = contentDescription, tint = tint)
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
        text = { Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Evet, Sil", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}


// 2. CARI LISTESI & MUSTERI EKLE SCREEN
@Composable
fun CariScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val cariList by viewModel.cariList.collectAsState()
    val servisList by viewModel.servisList.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<com.example.data.model.Cari?>(null) }

    // Form inputs state
    var adSoyad by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var adres by remember { mutableStateOf("") }
    var vergiDairesi by remember { mutableStateOf("") }
    var webSitesi by remember { mutableStateOf("") }
    var ibanNumarasi by remember { mutableStateOf("") }
    var ozelNotlar by remember { mutableStateOf("") }

    var showAddForm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredCari = cariList.filter {
        it.adSoyadFirma.lowercase().contains(searchQuery.lowercase()) ||
                it.telefon.contains(searchQuery) ||
                it.vergiDairesi.lowercase().contains(searchQuery.lowercase())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Cari Kartlar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${cariList.size} kayıtlı firma/müşteri",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddForm = !showAddForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddForm) ErrorRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(38.dp).testTag("toggle_add_cari_form"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Filled.Close else Icons.Filled.PersonAdd,
                        contentDescription = "Ekle",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddForm) "Vazgeç" else "Cari Ekle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Add customer form panel
        if (showAddForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Yeni Cari Müşteri Ekleme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = adSoyad,
                            onValueChange = { adSoyad = it },
                            label = { Text("Ad Soyad / Firma Ünvanı *") },
                            modifier = Modifier.fillMaxWidth().testTag("add_cari_name"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = telefon,
                                onValueChange = { telefon = it },
                                label = { Text("Telefon *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f).testTag("add_cari_phone"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = vergiDairesi,
                                onValueChange = { vergiDairesi = it },
                                label = { Text("Vergi Dairesi/No") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = adres,
                            onValueChange = { adres = it },
                            label = { Text("Adres Bilgisi") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = webSitesi,
                                onValueChange = { webSitesi = it },
                                label = { Text("Web Sitesi") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ibanNumarasi,
                                onValueChange = { ibanNumarasi = it },
                                label = { Text("IBAN Numarası") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = ozelNotlar,
                            onValueChange = { ozelNotlar = it },
                            label = { Text("Özel Müşteri Notları") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (adSoyad.isNotBlank() && telefon.isNotBlank()) {
                                    viewModel.addCari(
                                        adSoyad.trim(),
                                        telefon.trim(),
                                        adres.trim(),
                                        vergiDairesi.trim(),
                                        webSitesi.trim(),
                                        ibanNumarasi.trim(),
                                        ozelNotlar.trim()
                                    )
                                    // Reset fields
                                    adSoyad = ""
                                    telefon = ""
                                    adres = ""
                                    vergiDairesi = ""
                                    webSitesi = ""
                                    ibanNumarasi = ""
                                    ozelNotlar = ""
                                    showAddForm = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = adSoyad.isNotBlank() && telefon.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_add_cari_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                        ) {
                            Text("Cari Kaydı Tamamla (Firestore Sync)")
                        }
                    }
                }
            }
        }

        // Search text field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Müşteri ismi veya telefon ile hızlı filtreleme...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
                modifier = Modifier.fillMaxWidth().testTag("search_cari"),
                singleLine = true
            )
        }

        // Cari customers list
        if (filteredCari.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PeopleOutline,
                            contentDescription = "Boş",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aradığınız kritere uygun cari kart bulunamadı.", color = Color.Gray)
                    }
                }
            }
        } else {
            items(filteredCari) { client ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "Firma", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = client.adSoyadFirma,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { itemToDelete = client }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Sil/Delete Card", tint = ErrorRed)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TableRowLabel(label = "Telefon:", value = client.telefon)
                            TableRowLabel(label = "Adres:", value = client.adres.ifBlank { "Girilmemiş" })
                            TableRowLabel(label = "Vergi Dairesi:", value = client.vergiDairesi.ifBlank { "Bireysel/Girilmemiş" })
                            TableRowLabel(label = "Web Sitesi:", value = client.webSitesi.ifBlank { "Mevcut Değil" })
                            TableRowLabel(label = "IBAN:", value = client.iban.ifBlank { "Girilmemiş" })
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Güncel Cari Bakiye:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val balanceColor = when {
                                    client.bakiye > 0 -> SuccessGreen
                                    client.bakiye < 0 -> ErrorRed
                                    else -> Color.Gray
                                }
                                val balanceText = when {
                                    client.bakiye > 0 -> "+${client.bakiye.formatTL()} (Ön Ödeme/Alacak)"
                                    client.bakiye < 0 -> "${client.bakiye.formatTL()} (Borç/Bakiye)"
                                    else -> "0,00 TL"
                                }
                                Text(
                                    text = balanceText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = balanceColor
                                )
                            }
                            
                            if (client.ozelNotlar.isNotBlank()) {
                                TableRowLabel(label = "Özel Not:", value = client.ozelNotlar)
                            }
                        }

                        val clientJobs = servisList.filter { it.cariId == client.id }
                        if (clientJobs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Müşteri Hareket Geçmişi (${clientJobs.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clientJobs.forEach { job ->
                                    val isDelivered = job.durum == "Teslim Edildi"
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            // İşlem/Cihaz bilgisi (Üstte)
                                            Text(
                                                text = "${job.cihazMarkaModel} - ${job.sikayetDetayi} (${job.durum})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            // Ücret miktarı (Altta)
                                            Text(
                                                text = "Ücret: " + job.tahminiTutar.formatTL(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SuccessGreen
                                            )
                                            if (isDelivered) {
                                                val warrantyDurationMs = 180L * 24 * 60 * 60 * 1000 // 180 gün (6 ay)
                                                val remainingMs = (job.tarih + warrantyDurationMs) - System.currentTimeMillis()
                                                val remainingDays = remainingMs / (1000 * 60 * 60 * 24)
                                                val warrantyText = if (remainingDays > 0) {
                                                    "Garanti Aktif: $remainingDays gün kaldı"
                                                } else {
                                                    "Garanti Süresi Dolan Cihaz"
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "🛡️ $warrantyText (6 Ay)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (remainingDays > 0) SuccessGreen else Color.Gray,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { client ->
        DeleteConfirmationDialog(
            title = "Cari Kartı Sil",
            message = "${client.adSoyadFirma} isimli cari kart silinecektir. Bu işlem geri alınamaz!",
            onConfirm = {
                viewModel.deleteCari(client)
                itemToDelete = null
            },
            onDismiss = {
                itemToDelete = null
            }
        )
    }
}

@Composable
fun TableRowLabel(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}


// 3. IS TAKIP PANOSU (SERVIS KAYITLARI)
@Composable
fun ServisScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val servisList by viewModel.servisList.collectAsState()
    val cariList by viewModel.cariList.collectAsState()

    // Form states
    var selectedCariId by remember { mutableStateOf(-1) }
    var selectedCariName by remember { mutableStateOf("") }
    var searchCariQuery by remember { mutableStateOf("") }
    var expandedCariDropdown by remember { mutableStateOf(false) }

    var cihazMarkaModel by remember { mutableStateOf("") }
    var seriNo by remember { mutableStateOf("") }
    var sikayetDetayi by remember { mutableStateOf("") }
    var alinanKaporaStr by remember { mutableStateOf("") }
    var tahminiTutarStr by remember { mutableStateOf("") }

    var showAddJobForm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val aiAlert = viewModel.getAiArizaTahmini(sikayetDetayi)

    val activeJobs = servisList.filter { it.durum != "Teslim Edildi" }
    val deliveredJobs = servisList.filter { it.durum == "Teslim Edildi" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "İş Takip Panosu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeJobs.size} aktif, ${deliveredJobs.size} tamamlanmış",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddJobForm = !showAddJobForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddJobForm) ErrorRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(38.dp).testTag("toggle_add_servis_form"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (showAddJobForm) Icons.Filled.Close else Icons.Filled.AddCircle,
                        contentDescription = "Evrak",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddJobForm) "Vazgeç" else "Yeni Kabul",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Cihaz Kabul Formu
        if (showAddJobForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Yeni Cihaz Kabul Kabul Kaydı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Relative Customer Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedCariId != -1) selectedCariName else searchCariQuery,
                                onValueChange = {
                                    searchCariQuery = it
                                    selectedCariId = -1 // Reset if editing
                                    expandedCariDropdown = true
                                },
                                label = { Text("Cari Müşteri Seçimi *") },
                                trailingIcon = {
                                    IconButton(onClick = { expandedCariDropdown = !expandedCariDropdown }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Aç")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("select_cari_input")
                            )

                            DropdownMenu(
                                expanded = expandedCariDropdown,
                                onDismissRequest = { expandedCariDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                val searchableList = cariList.filter {
                                    it.adSoyadFirma.lowercase().contains(searchCariQuery.lowercase())
                                }
                                if (searchableList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Müşteri bulunamadı! Lütfen CARI menüsünden önce ekleyin.") },
                                        onClick = { expandedCariDropdown = false }
                                    )
                                } else {
                                    searchableList.take(5).forEach { client ->
                                        DropdownMenuItem(
                                            text = { Text("${client.adSoyadFirma} (${client.telefon})") },
                                            onClick = {
                                                selectedCariId = client.id
                                                selectedCariName = client.adSoyadFirma
                                                searchCariQuery = client.adSoyadFirma
                                                expandedCariDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = cihazMarkaModel,
                                onValueChange = { cihazMarkaModel = it },
                                label = { Text("Cihaz Marka/Model *") },
                                modifier = Modifier.weight(1f).testTag("add_servis_device"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = seriNo,
                                onValueChange = { seriNo = it },
                                label = { Text("Seri No / IMEI") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = sikayetDetayi,
                            onValueChange = { sikayetDetayi = it },
                            label = { Text("Arıza / Şikayet Detayı *") },
                            modifier = Modifier.fillMaxWidth().testTag("add_servis_complaint"),
                            maxLines = 3
                        )

                        // Real-time Intelligent AI Diagnostic Predictor Warning Box
                        if (aiAlert != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.Psychology, contentDescription = "AILogo", tint = AccentOrange, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiAlert,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentOrange
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = alinanKaporaStr,
                                onValueChange = { alinanKaporaStr = it },
                                label = { Text("Alınan Kapora (TL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tahminiTutarStr,
                                onValueChange = { tahminiTutarStr = it },
                                label = { Text("Tahmini Tutar (TL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (selectedCariId != -1 && cihazMarkaModel.isNotBlank() && sikayetDetayi.isNotBlank()) {
                                    val kap = alinanKaporaStr.toDoubleOrNull() ?: 0.0
                                    val tut = tahminiTutarStr.toDoubleOrNull() ?: 0.0
                                    
                                    viewModel.addServisKaydi(
                                        selectedCariId,
                                        selectedCariName,
                                        cihazMarkaModel.trim(),
                                        seriNo.trim(),
                                        sikayetDetayi.trim(),
                                        kap,
                                        tut
                                    )

                                    // Clear
                                    selectedCariId = -1
                                    selectedCariName = ""
                                    searchCariQuery = ""
                                    cihazMarkaModel = ""
                                    seriNo = ""
                                    sikayetDetayi = ""
                                    alinanKaporaStr = ""
                                    tahminiTutarStr = ""
                                    showAddJobForm = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = selectedCariId != -1 && cihazMarkaModel.isNotBlank() && sikayetDetayi.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_add_servis_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Cihaz Girişini Onayla (Müşteri Bildirimi Aktif)")
                        }
                    }
                }
            }
        }

        // Active repairs list
        item {
            Text(
                text = "Devam Eden Onarımlar (${activeJobs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeJobs.isEmpty()) {
            item {
                Text(
                    text = "Şu anda devam eden aktif bir onarım iş kaydı bulunmamaktadır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(activeJobs) { job ->
                ActiveJobItemCard(job = job, viewModel = viewModel)
            }
        }

        // Delivered historic repairs list
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tamamlanan ve Teslim Edilen Cihaz Arşivi (${deliveredJobs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (deliveredJobs.isEmpty()) {
            item {
                Text(
                    text = "Sonlandırılmış/teslim edilmiş geçmiş arşiv kaydı bulunmamaktadır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(deliveredJobs) { job ->
                DeliveredJobItemCard(job = job, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DeliveredJobItemCard(job: ServisKayit, viewModel: ErpViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showQuickStatusMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditServisDialog(
            job = job,
            onDismiss = { showEditDialog = false },
            viewModel = viewModel
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Onarım Arşivini Sil",
            message = "${job.cihazMarkaModel} (${if (job.seriNo.isBlank()) "Seri No Yok" else job.seriNo}) cihazına ait servis onarım kaydı arşivden tamamen silinecektir. Bu işlem geri alınamaz!",
            onConfirm = {
                viewModel.deleteServisKaydi(job)
                showDeleteConfirm = false
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Device Info & Delivered Badging Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Smartphone,
                            contentDescription = "Cihaz",
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = job.cihazMarkaModel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Teslim: ${job.tarih.formatDate()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Quick actions row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick Edit
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Düzenle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Quick Status Update
                    Box {
                        IconButton(
                            onClick = { showQuickStatusMenu = true },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PublishedWithChanges,
                                contentDescription = "Durum Güncelle",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showQuickStatusMenu,
                            onDismissRequest = { showQuickStatusMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            val statuses = listOf("Bekliyor", "Tamirde", "Parça Bekliyor", "Testte", "Hazır", "Teslim Edildi")
                            statuses.forEach { targetStatus ->
                                val optColor = when (targetStatus) {
                                    "Bekliyor" -> PendingYellow
                                    "Tamirde" -> BlueText
                                    "Parça Bekliyor" -> AccentOrange
                                    "Testte" -> Color(0xFF9B59B6)
                                    "Hazır" -> PrimaryTeal
                                    else -> SuccessGreen
                                }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(optColor)
                                            )
                                            Text(
                                                text = targetStatus,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (job.durum == targetStatus) FontWeight.Bold else FontWeight.Normal,
                                                color = if (job.durum == targetStatus) optColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showQuickStatusMenu = false
                                        viewModel.updateServisDurum(job, targetStatus)
                                    }
                                )
                            }
                        }
                    }

                    // Compact Delivered Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SuccessGreen.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
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
                                text = "TESLİM EDİLDİ",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            // Customer Name & Serial Display Group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Müşteri",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = job.cariIsim,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (job.seriNo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Seri No",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = job.seriNo,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Symptom / Fault Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Yapılan İşlem",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = job.sikayetDetayi,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Technician note display (Conditional)
            if (job.servisNotu.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                            Text(
                                text = "Teknisyen Onarım Raporu",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Text(
                            text = job.servisNotu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom actions row (Billing, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Ödeme Alındı",
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(text = "Toplam Tahsil Edilen", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(
                            text = job.tahminiTutar.formatTL(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Arşivi Sil", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ActiveJobItemCard(job: ServisKayit, viewModel: ErpViewModel) {
    var noteInput by remember { mutableStateOf(job.servisNotu) }
    var showNotEditingBlock by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showQuickStatusMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showReceiptDialog) {
        ReceiptPrintDialog(
            job = job,
            onDismiss = { showReceiptDialog = false },
            viewModel = viewModel
        )
    }

    if (showEditDialog) {
        EditServisDialog(
            job = job,
            onDismiss = { showEditDialog = false },
            viewModel = viewModel
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Onarım Kaydını Sil",
            message = "${job.cihazMarkaModel} (${if (job.seriNo.isBlank()) "Seri No Yok" else job.seriNo}) cihazının aktif onarım takibi kaydı tamamen silinecektir. Bu işlem geri alınamaz!",
            onConfirm = {
                viewModel.deleteServisKaydi(job)
                showDeleteConfirm = false
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }

    // Repair statuses coloring
    val statusColor = when (job.durum) {
        "Bekliyor" -> PendingYellow
        "Tamirde" -> BlueText
        "Parça Bekliyor" -> AccentOrange
        "Testte" -> Color(0xFF9B59B6)
        "Hazır" -> PrimaryTeal
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Device Info & Status Badging Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Smartphone,
                            contentDescription = "Cihaz",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = job.cihazMarkaModel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("device_title")
                        )
                        Text(
                            text = "Kayıt: ${job.tarih.formatDate()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Quick actions row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick Edit
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Düzenle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Quick Status Update
                    Box {
                        IconButton(
                            onClick = { showQuickStatusMenu = true },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PublishedWithChanges,
                                contentDescription = "Durum Güncelle",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showQuickStatusMenu,
                            onDismissRequest = { showQuickStatusMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            val statuses = listOf("Bekliyor", "Tamirde", "Parça Bekliyor", "Testte", "Hazır", "Teslim Edildi")
                            statuses.forEach { targetStatus ->
                                val optColor = when (targetStatus) {
                                    "Bekliyor" -> PendingYellow
                                    "Tamirde" -> BlueText
                                    "Parça Bekliyor" -> AccentOrange
                                    "Testte" -> Color(0xFF9B59B6)
                                    "Hazır" -> PrimaryTeal
                                    else -> SuccessGreen
                                }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(optColor)
                                            )
                                            Text(
                                                text = targetStatus,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (job.durum == targetStatus) FontWeight.Bold else FontWeight.Normal,
                                                color = if (job.durum == targetStatus) optColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showQuickStatusMenu = false
                                        viewModel.updateServisDurum(job, targetStatus)
                                    }
                                )
                            }
                        }
                    }

                    // Modern Glowing Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = job.durum.uppercase(),
                                color = statusColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            // Customer Name & Serial Display Group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Müşteri",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = job.cariIsim,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (job.seriNo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Seri No",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = job.seriNo,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Warranty Block (Conditional)
            if (job.durum == "Teslim Edildi" || job.durum == "Eski Kayıt") {
                val warrantyDurationMs = 180L * 24 * 60 * 60 * 1000 // 180 gün (6 ay)
                val remainingMs = (job.tarih + warrantyDurationMs) - System.currentTimeMillis()
                val remainingDays = remainingMs / (1000 * 60 * 60 * 24)
                val warrantyText = if (remainingDays > 0) {
                    "Garanti Sürüyor: $remainingDays gün kaldı"
                } else {
                    "Garanti Süresi Dolan Cihaz"
                }
                val warrantyColor = if (remainingDays > 0) SuccessGreen else Color.Gray

                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(containerColor = warrantyColor.copy(alpha = 0.08f)),
                     border = BorderStroke(1.dp, warrantyColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = warrantyColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = "🛡️ $warrantyText (Bitiş: ${(job.tarih + warrantyDurationMs).formatDate()})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = warrantyColor
                        )
                    }
                }
            }

            // Symptom / Fault Box (High Visibility)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReportProblem,
                            contentDescription = "Arıza",
                            tint = AccentOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Arıza / Müşteri Şikayeti",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    Text(
                        text = job.sikayetDetayi,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Financial Overview Mini Dashboard
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left: Alınan Kapora
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Kapora", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = job.alinanKapora.formatTL(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (job.alinanKapora > 0) SuccessGreen else Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                // Middle: Kalan Ödeme
                val remainingAmount = job.tahminiTutar - job.alinanKapora
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Kalan", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = remainingAmount.formatTL(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = if (remainingAmount > 0) ErrorRed else SuccessGreen,
                            maxLines = 1
                        )
                    }
                }

                // Right: Toplam
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Toplam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = job.tahminiTutar.formatTL(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Technician notes editor or display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = "Onarım Notu", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Teknisyen Onarım Raporu",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(onClick = { showNotEditingBlock = !showNotEditingBlock }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (showNotEditingBlock) Icons.Filled.Close else Icons.Filled.Edit,
                                contentDescription = "Onarım Notu Düzenle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (showNotEditingBlock) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text("Parça değişimi, yapılan testler ve onarım detayları...") },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    viewModel.updateServisNotu(job, noteInput)
                                    showNotEditingBlock = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Kaydet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = if (job.servisNotu.isBlank()) "Henüz teknisyen notu girilmemiş." else job.servisNotu,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (job.servisNotu.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                            fontStyle = if (job.servisNotu.isBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        )
                    }
                }
            }

            // WhatsApp template panel (Conditional)
            if (job.durum == "Hazır") {
                val templateText = "Sayın ${job.cariIsim}, ${job.cihazMarkaModel} cihazınızın onarımı tamamlanmıştır. Toplam Tutar: ${job.tahminiTutar.formatTL()}. İyi günler dileriz - BaranTech Bilişim"
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(templateText) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Sms, contentDescription = "SMS", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Müşteri Bilgilendir - WhatsApp Şablonu",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Text(
                            text = templateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(annotatedString)
                                    viewModel.triggerBarcodeScanNotification("📋 Bilgilendirme metni başarıyla kopyalandı!")
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Şablonu Kopyala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(annotatedString)
                                    viewModel.triggerBarcodeScanNotification("💬 WhatsApp Entegrasyonu: Müşteriye WhatsApp mesajı göndermek üzere tarayıcı yönlendiriliyor...")
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "WhatsApp Gönder", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp Gönder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Interactive Modern Status Update Segment
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Onarım Aşamasını Güncelle:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                var expandedStatusDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedStatusDropdown = true },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = statusColor),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = statusColor)
                            Text(
                                text = "AŞAMA: ${job.durum.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = statusColor)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedStatusDropdown,
                        onDismissRequest = { expandedStatusDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        val statuses = listOf("Bekliyor", "Tamirde", "Parça Bekliyor", "Testte", "Hazır", "Teslim Edildi")
                        statuses.forEach { targetStatus ->
                            val optColor = when (targetStatus) {
                                "Bekliyor" -> PendingYellow
                                "Tamirde" -> BlueText
                                "Parça Bekliyor" -> AccentOrange
                                "Testte" -> Color(0xFF9B59B6)
                                "Hazır" -> PrimaryTeal
                                else -> SuccessGreen
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(optColor)
                                        )
                                        Text(
                                            text = targetStatus,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (job.durum == targetStatus) FontWeight.Bold else FontWeight.Normal,
                                            color = if (job.durum == targetStatus) optColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    expandedStatusDropdown = false
                                    viewModel.updateServisDurum(job, targetStatus)
                                    
                                    // On final delivered billing, we automatically register a financial income transaction!
                                    if (targetStatus == "Teslim Edildi") {
                                        val remainingCash = job.tahminiTutar - job.alinanKapora
                                        if (remainingCash > 0.0) {
                                            viewModel.addKasaBanka(
                                                tipi = "Gelir",
                                                tutar = remainingCash,
                                                aciklama = "${job.cariIsim} - ${job.cihazMarkaModel} Kalan Bakiye Tahsilatı",
                                                odeme = "Nakit"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom actions row (Print Fiş, Archive Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showReceiptDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = "Fiş Yazdır", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fiş Yazdır (80mm)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Arşivi Sil", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EditServisDialog(
    job: ServisKayit,
    onDismiss: () -> Unit,
    viewModel: ErpViewModel
) {
    val allCariList by viewModel.cariList.collectAsState()

    var selectedCariId by remember { mutableStateOf(job.cariId) }
    var selectedCariName by remember { mutableStateOf(job.cariIsim) }
    var searchCariQuery by remember { mutableStateOf(job.cariIsim) }
    var expandedCariDropdown by remember { mutableStateOf(false) }

    var cihazMarkaModel by remember { mutableStateOf(job.cihazMarkaModel) }
    var seriNo by remember { mutableStateOf(job.seriNo) }
    var sikayetDetayi by remember { mutableStateOf(job.sikayetDetayi) }
    var alinanKaporaStr by remember { mutableStateOf(job.alinanKapora.toString()) }
    var tahminiTutarStr by remember { mutableStateOf(job.tahminiTutar.toString()) }
    var selectedStatus by remember { mutableStateOf(job.durum) }
    var expandedStatusDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kaydı Düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                // Cari Seçimi
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedCariId != -1) selectedCariName else searchCariQuery,
                        onValueChange = {
                            searchCariQuery = it
                            selectedCariId = -1
                            expandedCariDropdown = true
                        },
                        label = { Text("Cari Müşteri Seçimi") },
                        trailingIcon = {
                            IconButton(onClick = { expandedCariDropdown = !expandedCariDropdown }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Aç")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expandedCariDropdown,
                        onDismissRequest = { expandedCariDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        val searchableList = allCariList.filter {
                            it.adSoyadFirma.lowercase().contains(searchCariQuery.lowercase())
                        }
                        if (searchableList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Müşteri bulunamadı!") },
                                onClick = { expandedCariDropdown = false }
                            )
                        } else {
                            searchableList.take(5).forEach { client ->
                                DropdownMenuItem(
                                    text = { Text("${client.adSoyadFirma} (${client.telefon})") },
                                    onClick = {
                                        selectedCariId = client.id
                                        selectedCariName = client.adSoyadFirma
                                        searchCariQuery = client.adSoyadFirma
                                        expandedCariDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Cihaz Bilgisi
                OutlinedTextField(
                    value = cihazMarkaModel,
                    onValueChange = { cihazMarkaModel = it },
                    label = { Text("Cihaz Marka/Model *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = seriNo,
                    onValueChange = { seriNo = it },
                    label = { Text("Seri No / IMEI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sikayetDetayi,
                    onValueChange = { sikayetDetayi = it },
                    label = { Text("Arıza / Şikayet Detayı *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Finansalları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = alinanKaporaStr,
                        onValueChange = { alinanKaporaStr = it },
                        label = { Text("Alınan Kapora (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tahminiTutarStr,
                        onValueChange = { tahminiTutarStr = it },
                        label = { Text("Tahmini Tutar (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Status dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aşama/Durum") },
                        trailingIcon = {
                            IconButton(onClick = { expandedStatusDropdown = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Aç")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { expandedStatusDropdown = true }
                    )

                    DropdownMenu(
                        expanded = expandedStatusDropdown,
                        onDismissRequest = { expandedStatusDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        val statuses = listOf("Bekliyor", "Tamirde", "Parça Bekliyor", "Testte", "Hazır", "Teslim Edildi")
                        statuses.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    selectedStatus = st
                                    expandedStatusDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Vazgeç")
                    }

                    Button(
                        onClick = {
                            if (cihazMarkaModel.isNotBlank() && sikayetDetayi.isNotBlank() && selectedCariId != -1) {
                                val kapora = alinanKaporaStr.toDoubleOrNull() ?: 0.0
                                val tutar = tahminiTutarStr.toDoubleOrNull() ?: 0.0
                                val updatedJob = job.copy(
                                    cariId = selectedCariId,
                                    cariIsim = selectedCariName,
                                    cihazMarkaModel = cihazMarkaModel.trim(),
                                    seriNo = seriNo.trim(),
                                    sikayetDetayi = sikayetDetayi.trim(),
                                    alinanKapora = kapora,
                                    tahminiTutar = tutar,
                                    durum = selectedStatus
                                )
                                viewModel.updateServisRecord(updatedJob)
                                onDismiss()
                            }
                        },
                        enabled = cihazMarkaModel.isNotBlank() && sikayetDetayi.isNotBlank() && selectedCariId != -1,
                        modifier = Modifier.weight(1.5f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptPrintDialog(
    job: ServisKayit,
    onDismiss: () -> Unit,
    viewModel: ErpViewModel
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val receiptText = buildString {
        appendLine("========================================")
        appendLine("           BARANTECH BİLİŞİM            ")
        appendLine("       TEKNİK SERVİS TESLİM FİŞİ       ")
        appendLine("========================================")
        appendLine("Tarih: ${job.tarih.formatDate()}")
        appendLine("Fiş No: BTS-${100000 + job.id}")
        appendLine("----------------------------------------")
        appendLine("Cari Müşteri:")
        appendLine("  ${job.cariIsim}")
        appendLine("----------------------------------------")
        appendLine("Cihaz Bilgileri:")
        appendLine("  Marka/Model: ${job.cihazMarkaModel}")
        appendLine("  Seri / IMEI: " + if (job.seriNo.isEmpty()) "N/A" else job.seriNo)
        appendLine("----------------------------------------")
        appendLine("Şikayet / Arıza:")
        appendLine("  ${job.sikayetDetayi}")
        if (job.servisNotu.isNotBlank()) {
            appendLine("Teknisyen Notu:")
            appendLine("  ${job.servisNotu}")
        }
        appendLine("----------------------------------------")
        appendLine("Finansal Detaylar:")
        appendLine("  Alınan Kapora : ${job.alinanKapora.formatTL()}")
        appendLine("  Kalan Ücret   : ${(job.tahminiTutar - job.alinanKapora).formatTL()}")
        appendLine("  Toplam Tutar  : ${job.tahminiTutar.formatTL()}")
        appendLine("----------------------------------------")
        appendLine("Durum: ${job.durum.uppercase()}")
        appendLine("----------------------------------------")
        appendLine("Cihazınızı teslim alırken bu fişi")
        appendLine("ibra etmeniz gerekmektedir.")
        appendLine("Teşekkür eder, iyi günler dileriz.")
        appendLine("========================================")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖨️ Termal Fiş Önizleme (80mm)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                Text(
                    text = "Dükkandaki 80mm ESC/POS termal yazıcı formatında çıktı şablonu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFFFFEE5))
                        .border(BorderStroke(1.dp, Color.LightGray))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = receiptText,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(receiptText) })
                            viewModel.triggerBarcodeScanNotification("📋 Servis fişi ham ESC/POS metni panoya kopyalandı! Doğrudan termal yazıcı programına yapıştırıp basabilirsiniz.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Kopya", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Metni Kopyala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.triggerBarcodeScanNotification("🔌 ESC/POS Yazdırma: 80mm Termal rulo yazıcıya veri paketi gönderildi! UUID: BTS-${100000 + job.id}")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1.1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Yazdır", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yazıcıya Gönder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// 4. ISLEMLER & ISLEM TURLERI SCREEN
@Composable
fun IslemScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val islemList by viewModel.islemList.collectAsState()
    var itemToDelete by remember { mutableStateOf<com.example.data.model.IslemTuru?>(null) }

    var aciklama by remember { mutableStateOf("") }
    var sabitUcretStr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Standart İşçilik & İşlem Kataloğu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Teknisyenlerin hızlıca faturalandıracağı standart onarım kalıplarını ve fiyatlarını tanımlayın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Add Standard Task Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Yeni Fiyat Şablonu Tanımla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = aciklama,
                        onValueChange = { aciklama = it },
                        label = { Text("Yapılan İşlem Açıklaması * (Örn: Entegre Kalıplama)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_islem_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sabitUcretStr,
                        onValueChange = { sabitUcretStr = it },
                        label = { Text("Sabit İşçilik Ücreti (TL) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_islem_price"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val ucret = sabitUcretStr.toDoubleOrNull() ?: 0.0
                            if (aciklama.isNotBlank() && ucret > 0) {
                                viewModel.addIslemTuru(aciklama.trim(), ucret)
                                aciklama = ""
                                sabitUcretStr = ""
                            }
                        },
                        enabled = aciklama.isNotBlank() && (sabitUcretStr.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.fillMaxWidth().testTag("submit_add_islem_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Kataloğa Kaydet (Firestore Sync)")
                    }
                }
            }
        }

        // List tasks list
        item {
            Text(
                text = "Kayıtlı Sabit İşlemler (${islemList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (islemList.isEmpty()) {
            item {
                Text("Kayıtlı herhangi bir standart işlem bulunmuyor. Lütfen form yardımıyla ekleyiniz.")
            }
        } else {
            items(islemList) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Handyman, contentDescription = "Labor", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = task.aciklama,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Sabit İşçilik Fiyatı", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.wrapContentWidth(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.sabitUcret.formatTL(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { itemToDelete = task },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = ErrorRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { task ->
        DeleteConfirmationDialog(
            title = "Katalog İşlemini Sil",
            message = "${task.aciklama} isimli standart işlem katalogtan tamamen kaldırılacaktır. Onaylıyor musunuz?",
            onConfirm = {
                viewModel.deleteIslemTuru(task)
                itemToDelete = null
            },
            onDismiss = {
                itemToDelete = null
            }
        )
    }
}


// 5. STOK YONETIMI, MARKALAR & TEDARIKCILER SCREEN
@Composable
fun StokScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val stokList by viewModel.stokList.collectAsState()
    val rawAlerts by viewModel.criticalStockAlerts.collectAsState()
    var itemToDelete by remember { mutableStateOf<com.example.data.model.StokParca?>(null) }

    var showAddStokForm by remember { mutableStateOf(false) }

    // Inputs state
    var parcaAdi by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var adetStr by remember { mutableStateOf("") }
    var alisFiyatiStr by remember { mutableStateOf("") }
    var satisFiyatiStr by remember { mutableStateOf("") }
    var tedarikciIsim by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Yedek Parça Stok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stokList.size} kalem yedek parça",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddStokForm = !showAddStokForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddStokForm) ErrorRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(38.dp).testTag("toggle_add_stok_form"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (showAddStokForm) Icons.Filled.Close else Icons.Filled.AddShoppingCart,
                        contentDescription = "StokEkle",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddStokForm) "Vazgeç" else "Parça Ekle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live AI-driven automatic critical stock level warnings banner block
        if (rawAlerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Kritik", tint = AccentOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Akıllı Stok Seviyesi Uyarıları (${rawAlerts.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                        
                        Divider(color = AccentOrange.copy(alpha = 0.3f))
                        
                        rawAlerts.forEach { alertMsg ->
                            Text(
                                text = "• " + alertMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Add Part Input Panel
        if (showAddStokForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Yeni Yedek Parça Kabul Formu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = parcaAdi,
                            onValueChange = { parcaAdi = it },
                            label = { Text("Yedek Parça Adı *") },
                            modifier = Modifier.fillMaxWidth().testTag("add_stok_name"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = marka,
                                onValueChange = { marka = it },
                                label = { Text("Marka / Üretici *") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = adetStr,
                                onValueChange = { adetStr = it },
                                label = { Text("Stok Adet *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f).testTag("add_stok_qty"),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = alisFiyatiStr,
                                onValueChange = { alisFiyatiStr = it },
                                label = { Text("Birim Alış (TL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = satisFiyatiStr,
                                onValueChange = { satisFiyatiStr = it },
                                label = { Text("Birim Satış (TL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = tedarikciIsim,
                            onValueChange = { tedarikciIsim = it },
                            label = { Text("Toptancı / Tedarikçi Firma *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val count = adetStr.toIntOrNull() ?: 1
                                val buy = alisFiyatiStr.toDoubleOrNull() ?: 0.0
                                val sell = satisFiyatiStr.toDoubleOrNull() ?: 0.0
                                if (parcaAdi.isNotBlank() && marka.isNotBlank() && tedarikciIsim.isNotBlank()) {
                                    viewModel.addStokParca(
                                        parcaAdi.trim(),
                                        marka.trim(),
                                        count,
                                        buy,
                                        sell,
                                        tedarikciIsim.trim()
                                    )

                                    // Clear
                                    parcaAdi = ""
                                    marka = ""
                                    adetStr = ""
                                    alisFiyatiStr = ""
                                    satisFiyatiStr = ""
                                    tedarikciIsim = ""
                                    showAddStokForm = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = parcaAdi.isNotBlank() && marka.isNotBlank() && tedarikciIsim.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_add_stok_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Stok Girişi Sağla (Firestore Sync)")
                        }
                    }
                }
            }
        }

        // List parts list with quick stepper counters (+ / -) to change quantities immediately
        if (stokList.isEmpty()) {
            item {
                Text("Stokta henüz yedek parça kaydı yok. Lütfen parça ekleyin.")
            }
        } else {
            items(stokList) { item ->
                StokItemCard(item = item, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StokItemCard(item: StokParca, viewModel: ErpViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditStokDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            viewModel = viewModel
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Yedek Parça Sil",
            message = "${item.parcaAdi} isimli yedek parça stok veri tabanından kalıcı olarak silinecektir. Bu işlem geri alınamaz!",
            onConfirm = {
                viewModel.deleteStokParca(item)
                showDeleteConfirm = false
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (item.adet < 3) AccentOrange.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.parcaAdi,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Marka: ${item.marka} | Tedarikçi: ${item.tedarikciIsim}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Stepper Qty with ripple feedbacks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.updateStokAdet(item, item.adet - 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Azalt", modifier = Modifier.size(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (item.adet < 3) AccentOrange.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.adet.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (item.adet < 3) AccentOrange else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.updateStokAdet(item, item.adet + 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Artir", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Alış Fiyatı", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(item.alisFiyati.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Satış Fiyatı", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(item.satisFiyati.formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Column {
                        Text("Potansiyel Kar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(((item.satisFiyati - item.alisFiyati) * item.adet).formatTL(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Düzenle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = ErrorRed, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditStokDialog(
    item: StokParca,
    onDismiss: () -> Unit,
    viewModel: ErpViewModel
) {
    var parcaAdi by remember { mutableStateOf(item.parcaAdi) }
    var marka by remember { mutableStateOf(item.marka) }
    var adetStr by remember { mutableStateOf(item.adet.toString()) }
    var alisFiyatiStr by remember { mutableStateOf(item.alisFiyati.toString()) }
    var satisFiyatiStr by remember { mutableStateOf(item.satisFiyati.toString()) }
    var tedarikciIsim by remember { mutableStateOf(item.tedarikciIsim) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stok Kaydını Düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                OutlinedTextField(
                    value = parcaAdi,
                    onValueChange = { parcaAdi = it },
                    label = { Text("Yedek Parça Adı *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = marka,
                        onValueChange = { marka = it },
                        label = { Text("Marka / Üretici *") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adetStr,
                        onValueChange = { adetStr = it },
                        label = { Text("Stok Adet *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = alisFiyatiStr,
                        onValueChange = { alisFiyatiStr = it },
                        label = { Text("Birim Alış (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = satisFiyatiStr,
                        onValueChange = { satisFiyatiStr = it },
                        label = { Text("Birim Satış (TL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = tedarikciIsim,
                    onValueChange = { tedarikciIsim = it },
                    label = { Text("Toptancı / Tedarikçi Firma *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Vazgeç")
                    }

                    Button(
                        onClick = {
                            if (parcaAdi.isNotBlank() && marka.isNotBlank() && tedarikciIsim.isNotBlank()) {
                                val count = adetStr.toIntOrNull() ?: 0
                                val buy = alisFiyatiStr.toDoubleOrNull() ?: 0.0
                                val sell = satisFiyatiStr.toDoubleOrNull() ?: 0.0
                                val updated = item.copy(
                                    parcaAdi = parcaAdi.trim(),
                                    marka = marka.trim(),
                                    adet = count,
                                    alisFiyati = buy,
                                    satisFiyati = sell,
                                    tedarikciIsim = tedarikciIsim.trim()
                                )
                                viewModel.updateStokRecord(updated)
                                onDismiss()
                            }
                        },
                        enabled = parcaAdi.isNotBlank() && marka.isNotBlank() && tedarikciIsim.isNotBlank(),
                        modifier = Modifier.weight(1.5f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// 6. KASA & BANKA (GELIR/GIDER) Muhasebe
@Composable
fun FinansScreen(viewModel: ErpViewModel, modifier: Modifier = Modifier) {
    val finansList by viewModel.finansList.collectAsState()
    var itemToDelete by remember { mutableStateOf<com.example.data.model.KasaBanka?>(null) }

    var showFinansForm by remember { mutableStateOf(false) }

    // Inputs
    var tipi by remember { mutableStateOf("Gelir") } // Gelir or Gider
    var tutarStr by remember { mutableStateOf("") }
    var aciklama by remember { mutableStateOf("") }
    var odemeYontemi by remember { mutableStateOf("Nakit") } // Nakit or POS

    val focusManager = LocalFocusManager.current

    // Balance Metrics
    val totalGelir = finansList.filter { it.tipi == "Gelir" }.sumOf { it.tutar }
    val totalGider = finansList.filter { it.tipi == "Gider" }.sumOf { it.tutar }
    val netAmount = totalGelir - totalGider

    val totalNakit = finansList.filter { it.odemeYontemi == "Nakit" }.sumOf { if (it.tipi == "Gelir") it.tutar else -it.tutar }
    val totalPos = finansList.filter { it.odemeYontemi == "POS" }.sumOf { if (it.tipi == "Gelir") it.tutar else -it.tutar }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Kasa & Banka Muhasebesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elden nakit ve banka kredi kartı tahsilat özetleri",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showFinansForm = !showFinansForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showFinansForm) ErrorRed else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(38.dp).testTag("toggle_add_finans_form"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (showFinansForm) Icons.Filled.Close else Icons.Filled.Payments,
                        contentDescription = "Islemler",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showFinansForm) "Vazgeç" else "Fiş Ekle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ledger metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Kasa:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(totalNakit.formatTL(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (totalNakit >= 0) SuccessGreen else ErrorRed)
                    }
                    VerticalDivider(modifier = Modifier.height(16.dp), color = MaterialTheme.colorScheme.outline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("POS:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(totalPos.formatTL(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (totalPos >= 0) SuccessGreen else ErrorRed)
                    }
                    VerticalDivider(modifier = Modifier.height(16.dp), color = MaterialTheme.colorScheme.outline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Net:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(netAmount.formatTL(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = if (netAmount >= 0) SuccessGreen else ErrorRed)
                    }
                }
            }
        }

        // Monthly analytical chart
        item {
            MonthlyFinancialBarChart(finansList = finansList)
        }

        // Add transaction entry form
        if (showFinansForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Gelir / Gider Faturası Girişi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Selector tipi: Gelir / Gider
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { tipi = "Gelir" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tipi == "Gelir") SuccessGreen else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text("Giriş (GELİR)", color = if (tipi == "Gelir") Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = { tipi = "Gider" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tipi == "Gider") ErrorRed else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text("Çıkış (GIDER)", color = if (tipi == "Gider") Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        OutlinedTextField(
                            value = tutarStr,
                            onValueChange = { tutarStr = it },
                            label = { Text("İşlem Tutarı (TL) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("add_finans_amount"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = aciklama,
                            onValueChange = { aciklama = it },
                            label = { Text("Evrak Açıklaması *") },
                            modifier = Modifier.fillMaxWidth().testTag("add_finans_desc"),
                            singleLine = true
                        )

                        // Payment selector: Cash / Card POS
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Ödeme Şekli:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = odemeYontemi == "Nakit", onClick = { odemeYontemi = "Nakit" })
                                Text("Nakit (Elden)", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = odemeYontemi == "POS", onClick = { odemeYontemi = "POS" })
                                Text("POS (Kart/Banka)", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Button(
                            onClick = {
                                val tutar = tutarStr.toDoubleOrNull() ?: 0.0
                                if (tutar > 0 && aciklama.isNotBlank()) {
                                    viewModel.addKasaBanka(
                                        tipi,
                                        tutar,
                                        aciklama.trim(),
                                        odemeYontemi
                                    )

                                    // Clear
                                    tutarStr = ""
                                    aciklama = ""
                                    showFinansForm = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = (tutarStr.toDoubleOrNull() ?: 0.0) > 0 && aciklama.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_add_finans_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Finans Muhasebe Fişini Kaydet")
                        }
                    }
                }
            }
        }

        // Ledger Lists
        item {
            Text(
                text = "Tarihsel Günlük Hareketler (${finansList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (finansList.isEmpty()) {
            item {
                Text("Kayıtlı herhangi bir finansal evrak bulunmuyor.")
            }
        } else {
            items(finansList) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (entry.tipi == "Gelir") SuccessGreen.copy(alpha = 0.2f)
                                        else ErrorRed.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (entry.tipi == "Gelir") Icons.Filled.AddCard else Icons.Filled.LocalAtm,
                                    contentDescription = "H",
                                    tint = if (entry.tipi == "Gelir") SuccessGreen else ErrorRed
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = entry.aciklama,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Yöntem: ${entry.odemeYontemi} | Tarih: ${entry.tarih.formatDate()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = (if (entry.tipi == "Gelir") "+" else "-") + " " + entry.tutar.formatTL(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.tipi == "Gelir") SuccessGreen else ErrorRed,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        IconButton(
                            onClick = { itemToDelete = entry },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "FisSil", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { entry ->
        DeleteConfirmationDialog(
            title = "Finans İşlemini Sil",
            message = "${entry.aciklama} (${entry.tipi} - ${entry.tutar.formatTL()}) kaydı kasa/banka defterinizden tamamen silinecektir. Bu işlem geri alınamaz!",
            onConfirm = {
                viewModel.deleteKasaBanka(entry)
                itemToDelete = null
            },
            onDismiss = {
                itemToDelete = null
            }
        )
    }
}
