package com.footballxtream.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteFolderEntity::class, ProfileEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteFolderDao(): FavoriteFolderDao
    abstract fun profileDao(): ProfileDao
}
