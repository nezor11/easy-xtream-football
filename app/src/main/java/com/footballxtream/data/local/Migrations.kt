package com.footballxtream.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit Room migrations so updating the app keeps the user's saved profiles and favorites
 * instead of wiping the database (the old behaviour was [fallbackToDestructiveMigration]).
 *
 * The CREATE TABLE statements are copied verbatim from the exported schema under `app/schemas`,
 * so the resulting tables match exactly what Room expects after the migration runs.
 */

/**
 * v1 → v2: the `profiles` table was introduced.
 *
 * v1 shipped in two interim shapes: one with no `profiles` table, and a later one with a
 * Xtream-only `profiles` table (no `type`/`m3uUrl` columns). Both predate any release, so rather
 * than reconcile the partial column set we drop any leftover table and recreate it with the final
 * v2 schema. Only throwaway dev data is lost; real installs never had v1 profiles.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `profiles`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `profiles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`serverUrl` TEXT NOT NULL, " +
                "`username` TEXT NOT NULL, " +
                "`password` TEXT NOT NULL, " +
                "`m3uUrl` TEXT NOT NULL)",
        )
    }
}

/**
 * v2 → v3: favorites moved from individual channels (`favorite_channels`) to brand folders
 * (`favorite_folders`). The keys are incompatible (raw stream identity vs. normalized brand name),
 * so old favorites are dropped rather than mis-mapped; the `profiles` table is untouched and its
 * data is preserved.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `favorite_channels`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_folders` " +
                "(`name` TEXT NOT NULL, PRIMARY KEY(`name`))",
        )
    }
}

/** All migrations, in order. Add new ones here as the schema version grows. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
