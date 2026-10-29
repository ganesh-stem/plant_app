package com.verdant.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    fun observeById(id: Long): Flow<ScanEntity?>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getById(id: Long): ScanEntity?

    @Query("SELECT image_path FROM scans")
    suspend fun allImagePaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScanEntity): Long

    @Query("UPDATE scans SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}
