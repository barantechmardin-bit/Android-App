package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cari_listesi")
data class Cari(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val adSoyadFirma: String,
    val telefon: String,
    val adres: String,
    val vergiDairesi: String,
    val webSitesi: String,
    val iban: String,
    val ozelNotlar: String,
    val bakiye: Double = 0.0
)

@Entity(tableName = "servis_kayitlari")
data class ServisKayit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cariId: Int,
    val cariIsim: String,
    val cihazMarkaModel: String,
    val seriNo: String,
    val sikayetDetayi: String,
    val alinanKapora: Double,
    val tahminiTutar: Double,
    val durum: String, // "Bekliyor", "Tamirde", "Parça Bekliyor", "Testte", "Teslim Edildi"
    val servisNotu: String = "",
    val tarih: Long = System.currentTimeMillis()
)

@Entity(tableName = "islem_turleri")
data class IslemTuru(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val aciklama: String,
    val sabitUcret: Double
)

@Entity(tableName = "stok_parcalari")
data class StokParca(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parcaAdi: String,
    val marka: String,
    val adet: Int,
    val alisFiyati: Double,
    val satisFiyati: Double,
    val tedarikciIsim: String
)

@Entity(tableName = "kasa_banka_hareketleri")
data class KasaBanka(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipi: String, // "Gelir" veya "Gider"
    val tutar: Double,
    val aciklama: String,
    val odemeYontemi: String, // "Nakit" veya "POS"
    val tarih: Long = System.currentTimeMillis()
)
