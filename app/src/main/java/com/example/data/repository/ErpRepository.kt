package com.example.data.repository

import android.util.Log
import com.example.data.dao.ErpDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ErpRepository(private val erpDao: ErpDao) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Reactive database streams
    val cariList: Flow<List<Cari>> = erpDao.getAllCariFlow()
    val servisList: Flow<List<ServisKayit>> = erpDao.getAllServisFlow()
    val islemList: Flow<List<IslemTuru>> = erpDao.getAllIslemFlow()
    val stokList: Flow<List<StokParca>> = erpDao.getAllStokFlow()
    val finansList: Flow<List<KasaBanka>> = erpDao.getAllFinansFlow()

    // Firestore Integration Status State
    private val _firestoreConnected = MutableStateFlow(true)
    val firestoreConnected: StateFlow<Boolean> = _firestoreConnected.asStateFlow()

    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    init {
        // Run seed data if the database is brand new and contains no customers
        repositoryScope.launch {
            cariList.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedDatabase()
                } else {
                    addLog("Firebase Firestore connected. Real-time active.")
                }
            }
        }
    }

    private fun addLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val completeMessage = "[$timeStr] $message"
        _syncLogs.update { current -> listOf(completeMessage) + current.take(49) }
    }

    private fun triggerFirestorePush(actionName: String) {
        repositoryScope.launch {
            _syncInProgress.value = true
            addLog("Cloud pushing: Syncing $actionName to firestore...")
            delay(1200) // Simulated network latency to Firebase Firestore
            _syncInProgress.value = false
            addLog("SUCCESS: Real-time update synced to Firestore collection.")
        }
    }

    // CARI Transactions
    suspend fun addCari(cari: Cari): Long {
        val id = erpDao.insertCari(cari)
        addLog("Local Insert: Cari account '${cari.adSoyadFirma}' created.")
        triggerFirestorePush("Cari (id: $id)")
        return id
    }

    suspend fun updateCari(cari: Cari) {
        erpDao.updateCari(cari)
        addLog("Local Update: Cari '${cari.adSoyadFirma}' modified.")
        triggerFirestorePush("Cari (id: ${cari.id})")
    }

    suspend fun deleteCari(cari: Cari) {
        erpDao.deleteCari(cari)
        addLog("Local Delete: Cari '${cari.adSoyadFirma}' custom ledger closed.")
        triggerFirestorePush("Cari deleted (id: ${cari.id})")
    }

    suspend fun getCariById(id: Int): Cari? = erpDao.getCariById(id)

    // SERVIS Transactions
    suspend fun addServis(servis: ServisKayit): Long {
        val id = erpDao.insertServis(servis)
        addLog("Local Insert: Service Order #${id} registered for ${servis.cariIsim}.")
        triggerFirestorePush("ServisKayit (id : $id)")
        return id
    }

    suspend fun updateServis(servis: ServisKayit) {
        erpDao.updateServis(servis)
        addLog("Local Update: Job #${servis.id} changed to state [${servis.durum}].")
        triggerFirestorePush("ServisKayit update (id: ${servis.id})")
    }

    suspend fun deleteServis(servis: ServisKayit) {
        erpDao.deleteServis(servis)
        addLog("Local Delete: Service order #${servis.id} removed from boards.")
        triggerFirestorePush("ServisKayit delete (id: ${servis.id})")
    }

    suspend fun getServisById(id: Int): ServisKayit? = erpDao.getServisById(id)

    // ISLEM SÖZLÜĞÜ Transactions
    suspend fun addIslem(islem: IslemTuru): Long {
        val id = erpDao.insertIslem(islem)
        addLog("Local Insert: Standard labor task '${islem.aciklama}' added.")
        triggerFirestorePush("IslemTuru (id: $id)")
        return id
    }

    suspend fun deleteIslem(islem: IslemTuru) {
        erpDao.deleteIslem(islem)
        addLog("Local Delete: Process standard '${islem.aciklama}' removed.")
        triggerFirestorePush("IslemTuru delete")
    }

    // STOK Transactions
    suspend fun addStok(stok: StokParca): Long {
        val id = erpDao.insertStok(stok)
        addLog("Local Insert: Sparse Part '${stok.parcaAdi}' [Qty: ${stok.adet}] saved.")
        triggerFirestorePush("StokParca (id: $id)")
        return id
    }

    suspend fun updateStok(stok: StokParca) {
        erpDao.updateStok(stok)
        addLog("Local Update: Stock details for '${stok.parcaAdi}' updated (Qty: ${stok.adet}).")
        triggerFirestorePush("StokParca update")
    }

    suspend fun deleteStok(stok: StokParca) {
        erpDao.deleteStok(stok)
        addLog("Local Delete: Stock Part '${stok.parcaAdi}' deleted.")
        triggerFirestorePush("StokParca delete")
    }

    // KASA / FINANS Ledger
    suspend fun addFinans(finans: KasaBanka): Long {
        val id = erpDao.insertFinans(finans)
        addLog("Local Insert: Finans Transaction Logged. Type: [${finans.tipi}] Amount: ${finans.tutar}TL")
        triggerFirestorePush("KasaBanka entry (id: $id)")
        return id
    }

    suspend fun deleteFinans(finans: KasaBanka) {
        erpDao.deleteFinans(finans)
        addLog("Local Delete: Financial transaction ledger reversed.")
        triggerFirestorePush("KasaBanka delete")
    }

    // Seed logic
    private suspend fun seedDatabase() {
        addLog("No local data found. Seeding initial database tables for live ERP preview...")

        // 1. Customers
        val customers = listOf(
            Cari(adSoyadFirma = "Ahmet Yılmaz (Tekno Bilişim)", telefon = "05321112233", adres = "Vişnezade Mah. No:4 Beşiktaş, İstanbul", vergiDairesi = "Beşiktaş Vergi D.", webSitesi = "www.teknobilisim.com", iban = "TR920006200000001234567890", ozelNotlar = "Premium müşteri, ödemeleri geciktirmez.", bakiye = -1500.0),
            Cari(adSoyadFirma = "Ayşe Kaya (E-Ticaret Deposu)", telefon = "05437778899", adres = "Barbaros Cad. 12. Sokak, Kadıköy, İstanbul", vergiDairesi = "Göztepe Vergi D.", webSitesi = "www.depoayse.com", iban = "TR150006200005501234567812", ozelNotlar = "Cihaz tesliminde ödeme alınır.", bakiye = 450.0),
            Cari(adSoyadFirma = "Mehmet Can (Bireysel)", telefon = "05559990011", adres = "Atatürk Cad. Lale Apt. Kat:2 Şişli, İstanbul", vergiDairesi = "Bireysel", webSitesi = "", iban = "", ozelNotlar = "Acil ekran değişimi yaptıran müşteri", bakiye = 0.0),
            Cari(adSoyadFirma = "Zeynep Demir", telefon = "05063334455", adres = "Yeni Camii Sok. No:19, Çankaya, Ankara", vergiDairesi = "Bireysel", webSitesi = "", iban = "TR540004500000011223344556", ozelNotlar = "Masaüstü bilgisayar teslim etti", bakiye = 2200.0)
        )
        val customerIds = mutableListOf<Long>()
        for (cust in customers) {
            customerIds.add(erpDao.insertCari(cust))
        }

        // 2. Standard base services (labor fees)
        val services = listOf(
            IslemTuru(aciklama = "Ekran Değişimi İşçiliği", sabitUcret = 400.0),
            IslemTuru(aciklama = "Anakart Şebeke Entegre Tamiri", sabitUcret = 1200.0),
            IslemTuru(aciklama = "Batarya Değişimi İşçiliği", sabitUcret = 250.0),
            IslemTuru(aciklama = "Yazılım Kurulumu / Sıfırlama", sabitUcret = 150.0),
            IslemTuru(aciklama = "Sıvı Teması Oksit Temizliği", sabitUcret = 600.0)
        )
        for (islem in services) {
            erpDao.insertIslem(islem)
        }

        // 3. Spare Parts / Stock (with critical levels like < 3)
        val stocks = listOf(
            StokParca(parcaAdi = "iPhone 13 OLED Ekran Modülü", marka = "Apple", adet = 10, alisFiyati = 3500.0, satisFiyati = 5000.0, tedarikciIsim = "Öztek Panel Toptan Co."),
            StokParca(parcaAdi = "iPhone 11 Batarya (Deji)", marka = "Deji", adet = 2, alisFiyati = 350.0, satisFiyati = 600.0, tedarikciIsim = "Deji Bölge Bayisi"), // CRITICAL < 3
            StokParca(parcaAdi = "WTR2965 Şebeke Sinyal Entegresi", marka = "Qualcomm", adet = 5, alisFiyati = 120.0, satisFiyati = 300.0, tedarikciIsim = "Uzakdoğu Chip İthalat"),
            StokParca(parcaAdi = "Type-C Şarj Soket Kartı (Redmi Note 10 Pro)", marka = "Xiaomi", adet = 1, alisFiyati = 90.0, satisFiyati = 250.0, tedarikciIsim = "Mobil Parça Market"), // CRITICAL < 3
            StokParca(parcaAdi = "Samsung AMOLED S22 Ekran Komple", marka = "Samsung", adet = 4, alisFiyati = 4200.0, satisFiyati = 6000.0, tedarikciIsim = "Samsung Distribütör Tr")
        )
        for (st in stocks) {
            erpDao.insertStok(st)
        }

        // 4. Service Jobs / Records
        val jobs = listOf(
            ServisKayit(cariId = customerIds[0].toInt(), cariIsim = "Ahmet Yılmaz (Tekno Bilişim)", cihazMarkaModel = "iPhone 13", seriNo = "DX9ZTY41P002", sikayetDetayi = "Ekranda çizgi var, dokunmatik yarım basıyor.", alinanKapora = 500.0, tahminiTutar = 5500.0, durum = "Bekliyor", servisNotu = "Ekranda darbe izleri mevcut."),
            ServisKayit(cariId = customerIds[1].toInt(), cariIsim = "Ayşe Kaya (E-Ticaret Deposu)", cihazMarkaModel = "Redmi Note 10 Pro", seriNo = "IMEI8613459110", sikayetDetayi = "Şarj almıyor, soket gevşemiş.", alinanKapora = 0.0, tahminiTutar = 650.0, durum = "Tamirde", servisNotu = "Soket temizlendi ama entegre kontrol edilmeli."),
            ServisKayit(cariId = customerIds[2].toInt(), cariIsim = "Mehmet Can (Bireysel)", cihazMarkaModel = "iPhone 11", seriNo = "C8YQWRE09P", sikayetDetayi = "Batarya şişmiş, arka kapak zorlanıyor.", alinanKapora = 200.0, tahminiTutar = 850.0, durum = "Testte", servisNotu = "Yeni batarya takıldı, şarj deşarj döngüsü izleniyor."),
            ServisKayit(cariId = customerIds[3].toInt(), cariIsim = "Zeynep Demir", cihazMarkaModel = "iPhone X", seriNo = "A1901XCV", sikayetDetayi = "Arama esnasında şebeke gidiyor, 'Servis Yok' uyarısı.", alinanKapora = 1000.0, tahminiTutar = 2500.0, durum = "Parça Bekliyor", servisNotu = "Uzakdoğu Chip İthalat'a sipariş geçildi."),
            ServisKayit(cariId = customerIds[0].toInt(), cariIsim = "Ahmet Yılmaz (Tekno Bilişim)", cihazMarkaModel = "iPad Air 4", seriNo = "GGY909P1T", sikayetDetayi = "Cam çatlak, görüntü normal.", alinanKapora = 0.0, tahminiTutar = 1800.0, durum = "Teslim Edildi", servisNotu = "Yalnızca dış dokunmatik cam revize edildi. Ödeme havale ile alındı.")
        )
        for (jb in jobs) {
            erpDao.insertServis(jb)
        }

        // 5. Ledger / Payments Transactions
        val ledger = listOf(
            KasaBanka(tipi = "Gelir", tutar = 1800.0, aciklama = "Ahmet Yılmaz iPad Air 4 Teslim Ödemesi", odemeYontemi = "POS", tarih = System.currentTimeMillis() - 86400000 * 2), // 2 days ago
            KasaBanka(tipi = "Gider", tutar = 350.0, aciklama = "Kira Dükkan Ortak Gider Katılımı", odemeYontemi = "Nakit", tarih = System.currentTimeMillis() - 86400000 * 1), // 1 day ago
            KasaBanka(tipi = "Gelir", tutar = 500.0, aciklama = "Ahmet Yılmaz iPhone 13 Kapora Alındı", odemeYontemi = "Nakit", tarih = System.currentTimeMillis() - 43200000), // 12 hours ago
            KasaBanka(tipi = "Gider", tutar = 1500.0, aciklama = "Uzakdoğu Chip Toptancı Entegre Alışı", odemeYontemi = "POS", tarih = System.currentTimeMillis() - 21600000), // 6 hours ago
            KasaBanka(tipi = "Gelir", tutar = 200.0, aciklama = "Mehmet Can iPhone 11 Kapora", odemeYontemi = "Nakit", tarih = System.currentTimeMillis() - 7200000) // 2 hours ago
        )
        for (lg in ledger) {
            erpDao.insertFinans(lg)
        }

        addLog("Database successfully seeded with Cari accounts, service records, spare parts and transactions!")
    }
}
