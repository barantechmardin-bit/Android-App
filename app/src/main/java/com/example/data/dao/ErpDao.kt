package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ErpDao {

    // CARI Queries
    @Query("SELECT * FROM cari_listesi ORDER BY adSoyadFirma ASC")
    fun getAllCariFlow(): Flow<List<Cari>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCari(cari: Cari): Long

    @Update
    suspend fun updateCari(cari: Cari)

    @Delete
    suspend fun deleteCari(cari: Cari)

    @Query("SELECT * FROM cari_listesi WHERE id = :id")
    suspend fun getCariById(id: Int): Cari?


    // SERVIS Queries
    @Query("SELECT * FROM servis_kayitlari ORDER BY tarih DESC")
    fun getAllServisFlow(): Flow<List<ServisKayit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServis(servis: ServisKayit): Long

    @Update
    suspend fun updateServis(servis: ServisKayit)

    @Delete
    suspend fun deleteServis(servis: ServisKayit)

    @Query("SELECT * FROM servis_kayitlari WHERE id = :id")
    suspend fun getServisById(id: Int): ServisKayit?


    // ISLEM Queries
    @Query("SELECT * FROM islem_turleri ORDER BY aciklama ASC")
    fun getAllIslemFlow(): Flow<List<IslemTuru>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIslem(islem: IslemTuru): Long

    @Delete
    suspend fun deleteIslem(islem: IslemTuru)


    // STOK Queries
    @Query("SELECT * FROM stok_parcalari ORDER BY parcaAdi ASC")
    fun getAllStokFlow(): Flow<List<StokParca>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStok(stok: StokParca): Long

    @Update
    suspend fun updateStok(stok: StokParca)

    @Delete
    suspend fun deleteStok(stok: StokParca)


    // KASA/BANKA Queries
    @Query("SELECT * FROM kasa_banka_hareketleri ORDER BY tarih DESC")
    fun getAllFinansFlow(): Flow<List<KasaBanka>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinans(finans: KasaBanka): Long

    @Delete
    suspend fun deleteFinans(finans: KasaBanka)
}
