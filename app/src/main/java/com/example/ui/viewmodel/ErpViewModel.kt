package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ErpRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

class ErpViewModel(
    private val repository: ErpRepository,
    private val sharedPrefs: android.content.SharedPreferences
) : ViewModel() {

    // Active Navigation Tab preserved via SharedPreferences (analogous to localStorage)
    private val _currentTab = MutableStateFlow(sharedPrefs.getString("current_tab", "DASHBOARD") ?: "DASHBOARD")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
        sharedPrefs.edit().putString("current_tab", tab).apply()
    }

    // Barcode Search state mapping for USB scanner and direct search
    private val _barcodeSearchQuery = MutableStateFlow("")
    val barcodeSearchQuery: StateFlow<String> = _barcodeSearchQuery.asStateFlow()

    fun updateBarcodeSearchQuery(query: String) {
        _barcodeSearchQuery.value = query
        if (query.isNotEmpty()) {
            _currentTab.value = "DASHBOARD"
            sharedPrefs.edit().putString("current_tab", "DASHBOARD").apply()
        }
    }

    // Modern Dark Mode toggler state preserved via SharedPreferences
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("is_dark_mode", true)) // Dark Mode by default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        val nextVal = !_isDarkMode.value
        _isDarkMode.value = nextVal
        sharedPrefs.edit().putBoolean("is_dark_mode", nextVal).apply()
    }

    // Read reactive streams from Repository
    val cariList: StateFlow<List<Cari>> = repository.cariList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servisList: StateFlow<List<ServisKayit>> = repository.servisList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val islemList: StateFlow<List<IslemTuru>> = repository.islemList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stokList: StateFlow<List<StokParca>> = repository.stokList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finansList: StateFlow<List<KasaBanka>> = repository.finansList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<String>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncInProgress: StateFlow<Boolean> = repository.syncInProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val firestoreConnected: StateFlow<Boolean> = repository.firestoreConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Automated Notification State (for Simulated WhatsApp / Telegram SMS notifications)
    private val _autoMessageNotification = MutableStateFlow<String?>(null)
    val autoMessageNotification: StateFlow<String?> = _autoMessageNotification.asStateFlow()

    fun clearNotification() {
        _autoMessageNotification.value = null
    }

    fun triggerBarcodeScanNotification(barcode: String) {
        _autoMessageNotification.value = "🔌 Donanım Girişi: USB/Kablosuz Barkod Okuyucu cihaz algıladı!\n\n'$barcode' numaralı cihaz veritabanında başarıyla sorgulandı."
    }

    // Dynamic AI Stock Warnings State: automatic derivation of stock parts under 3 items
    val criticalStockAlerts: StateFlow<List<String>> = stokList
        .map { list ->
            list.filter { it.adet < 3 }.map { 
                "Kritik Stok! '${it.parcaAdi}' adedi ${it.adet} adete düştü. En uygun tedarikçi [${it.tedarikciIsim}] üzerinden sipariş verilmesi öneriliyor!"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic AI Repair Delay/Due Date Warnings State: alert when active repair duration exceeds 12 hours
    val upcomingRepairAlerts: StateFlow<List<String>> = servisList
        .map { list ->
            list.filter { it.durum != "Teslim Edildi" }.mapNotNull { 
                val elapsedMs = System.currentTimeMillis() - it.tarih
                val elapsedHours = elapsedMs / (1000 * 60 * 60)
                if (elapsedHours >= 12) {
                    val formattedTime = if (elapsedHours >= 24) {
                        "${elapsedHours / 24} gün ${elapsedHours % 24} saat"
                    } else {
                        "$elapsedHours saat"
                    }
                    "Süre Uyarısı! ${it.cariIsim} - '${it.cihazMarkaModel}' tamir aşaması '${it.durum}' durumunda $formattedTime süredir bekliyor."
                } else null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI actions calling Repository (background flow)
    fun addCari(adSoyad: String, tel: String, adres: String, vd: String, web: String, iban: String, ozelNot: String) {
        viewModelScope.launch {
            val cari = Cari(
                adSoyadFirma = adSoyad,
                telefon = tel,
                adres = adres,
                vergiDairesi = vd,
                webSitesi = web,
                iban = iban,
                ozelNotlar = ozelNot
            )
            repository.addCari(cari)
        }
    }

    fun updateCari(cari: Cari) {
        viewModelScope.launch {
            repository.updateCari(cari)
        }
    }

    fun deleteCari(cari: Cari) {
        viewModelScope.launch {
            repository.deleteCari(cari)
        }
    }

    fun addServisKaydi(cariId: Int, cariIsim: String, markaModel: String, seriNo: String, sikayet: String, kapora: Double, tahminiTutar: Double) {
        viewModelScope.launch {
            val servis = ServisKayit(
                cariId = cariId,
                cariIsim = cariIsim,
                cihazMarkaModel = markaModel,
                seriNo = seriNo,
                sikayetDetayi = sikayet,
                alinanKapora = kapora,
                tahminiTutar = tahminiTutar,
                durum = "Bekliyor"
            )
            repository.addServis(servis)
        }
    }

    fun updateServisDurum(servis: ServisKayit, yeniDurum: String) {
        viewModelScope.launch {
            val updated = servis.copy(durum = yeniDurum)
            repository.updateServis(updated)

            // Automate Message Simulation Trigger
            if (yeniDurum == "Testte" || yeniDurum == "Hazır" || yeniDurum == "Teslim Edildi") {
                val msg = "Otomatik Mesaj Bilgilendirmesi:\n\nMüşteriye (${servis.cariIsim}) WhatsApp/Telegram " +
                        "üzerinden \"Cihazınızın (${servis.cihazMarkaModel}) tamiri tamamlandı, güncel durum: $yeniDurum\" mesajı başarıyla gönderildi!"
                _autoMessageNotification.value = msg

                // Autodismiss notification after 4 seconds
                delay(4000)
                if (_autoMessageNotification.value == msg) {
                    _autoMessageNotification.value = null
                }
            }
        }
    }

    fun updateServisNotu(servis: ServisKayit, yeniNot: String) {
        viewModelScope.launch {
            val updated = servis.copy(servisNotu = yeniNot)
            repository.updateServis(updated)
        }
    }

    fun updateServisRecord(servis: ServisKayit) {
        viewModelScope.launch {
            repository.updateServis(servis)
        }
    }

    fun deleteServisKaydi(servis: ServisKayit) {
        viewModelScope.launch {
            repository.deleteServis(servis)
        }
    }

    fun addIslemTuru(aciklama: String, ucret: Double) {
        viewModelScope.launch {
            val islem = IslemTuru(aciklama = aciklama, sabitUcret = ucret)
            repository.addIslem(islem)
        }
    }

    fun deleteIslemTuru(islem: IslemTuru) {
        viewModelScope.launch {
            repository.deleteIslem(islem)
        }
    }

    fun addStokParca(ad: String, marka: String, adet: Int, alis: Double, satis: Double, tedarikci: String) {
        viewModelScope.launch {
            val stok = StokParca(
                parcaAdi = ad,
                marka = marka,
                adet = adet,
                alisFiyati = alis,
                satisFiyati = satis,
                tedarikciIsim = tedarikci
            )
            repository.addStok(stok)
        }
    }

    fun updateStokAdet(stok: StokParca, yeniAdet: Int) {
        viewModelScope.launch {
            val updated = stok.copy(adet = yeniAdet.coerceAtLeast(0))
            repository.updateStok(updated)
        }
    }

    fun updateStokRecord(stok: StokParca) {
        viewModelScope.launch {
            repository.updateStok(stok)
        }
    }

    fun deleteStokParca(stok: StokParca) {
        viewModelScope.launch {
            repository.deleteStok(stok)
        }
    }

    fun addKasaBanka(tipi: String, tutar: Double, aciklama: String, odeme: String) {
        viewModelScope.launch {
            val kb = KasaBanka(
                tipi = tipi,
                tutar = tutar,
                aciklama = aciklama,
                odemeYontemi = odeme
            )
            repository.addFinans(kb)
        }
    }

    fun deleteKasaBanka(kb: KasaBanka) {
        viewModelScope.launch {
            repository.deleteFinans(kb)
        }
    }

    // Dynamic AI Intelligent Diagnostics Logic based on typed text
    fun getAiArizaTahmini(sikayet: String): String? {
        if (sikayet.isBlank()) return null
        val lower = sikayet.lowercase(Locale.getDefault())

        val stocks = stokList.value

        return when {
            lower.contains("şebeke") || lower.contains("servis yok") || lower.contains("sinyal") || lower.contains("hat yok") -> {
                val stockItem = stocks.firstOrNull { it.parcaAdi.lowercase().contains("şebeke") || it.parcaAdi.lowercase().contains("entegre") }
                val stockText = if (stockItem != null) "${stockItem.adet} adet yedek parça var" else "stokta yedek parça yok, tedarikçiden istenecek"
                "💡 AI Tahmini: Arıza %80 ihtimalle şebeke entegresinden (Baseband RF) kaynaklanıyor. $stockText."
            }
            lower.contains("ekran") || lower.contains("dokunmatik") || lower.contains("kırık") || lower.contains("görüntü") || lower.contains("çizgi") -> {
                val stockItem = stocks.firstOrNull { it.parcaAdi.lowercase().contains("ekran") || it.parcaAdi.lowercase().contains("oled") }
                val stockText = if (stockItem != null) "stokta ${stockItem.adet} adet OLED ekran mevcut" else "stokta kalmamış"
                "💡 AI Tahmini: Arıza %95 ihtimalle ekran paneli hasarı. Öneri: Ekran modülü komple değişimi ($stockText)."
            }
            lower.contains("şarj") || lower.contains("soket") || lower.contains("temas") || lower.contains("kablo") || lower.contains("almıyor") -> {
                val stockItem = stocks.firstOrNull { it.parcaAdi.lowercase().contains("soket") || it.parcaAdi.lowercase().contains("redmi") }
                val stockText = if (stockItem != null) "stokta ${stockItem.adet} adet şarj soket kartı var" else "stokta yedek parça bulunmuyor"
                "💡 AI Tahmini: Arıza %85 ihtimalle şarj soket kartından veya sub-flex bağlantısından kaynaklanıyor. ($stockText)."
            }
            lower.contains("batarya") || lower.contains("pil") || lower.contains("şişme") || lower.contains("çabuk bitiyor") || lower.contains("kapanıyor") -> {
                val stockItem = stocks.firstOrNull { it.parcaAdi.lowercase().contains("batarya") || it.parcaAdi.lowercase().contains("pil") }
                val stockText = if (stockItem != null) "${stockItem.adet} adet yedek batarya var" else "Yedek batarya temin edilmeli"
                "💡 AI Tahmini: Batarya ömrü tükenmiş (%90 ihtimalle hücre aşınması). Öneri: Deji premium batarya değişimi ($stockText)."
            }
            lower.contains("sıvı") || lower.contains("su") || lower.contains("ıslandı") || lower.contains("oksit") -> {
                "💡 AI Tahmini: Sıvı teması oksitlenme riski! İvedi olarak anakartın alkol banyosunda yıkanması ve oksit temizliği yapılması gerekir. Şarj uygulanmamalıdır!"
            }
            else -> {
                "💡 AI Gözlemi: Kelimeler analiz ediliyor... İlk aşamada dükkan içi 'Sıfırlama / Yazılım Kontrolü' yapılması ve donanımsal akım testiyle teyit edilmesi önerilir."
            }
        }
    }
}

// ViewModel factory to inject Repository and SharedPreferences dependencies
class ErpViewModelFactory(
    private val repository: ErpRepository,
    private val sharedPrefs: android.content.SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ErpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ErpViewModel(repository, sharedPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
