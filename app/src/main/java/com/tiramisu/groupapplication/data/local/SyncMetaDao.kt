package com.tiramisu.groupapplication.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "sync_meta")
data class SyncMeta(
    @PrimaryKey val key: String,
    val lastEpoch: Long
)

@Dao
interface SyncMetaDao {
    @Query("SELECT lastEpoch FROM sync_meta WHERE `key` = :key LIMIT 1")
    suspend fun getLast(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMeta)

    @Query("DELETE FROM sync_meta WHERE `key` LIKE :prefix || '%'")
    suspend fun clearPrefix(prefix: String)
}