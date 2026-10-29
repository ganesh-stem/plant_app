package com.verdant.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ScanEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class VerdantDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        const val NAME = "verdant.db"
    }
}
