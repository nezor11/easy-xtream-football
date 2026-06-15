package com.footballxtream.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A favorited channel family, keyed by its folder name (e.g. "Canal Deporte"). */
@Entity(tableName = "favorite_folders")
data class FavoriteFolderEntity(
    @PrimaryKey val name: String,
)

@Dao
interface FavoriteFolderDao {

    @Query("SELECT * FROM favorite_folders ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FavoriteFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(folder: FavoriteFolderEntity)

    @Query("DELETE FROM favorite_folders WHERE name = :name")
    suspend fun remove(name: String)
}
