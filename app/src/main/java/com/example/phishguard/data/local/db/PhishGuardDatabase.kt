package com.example.phishguard.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.phishguard.data.local.dao.ThreatDao
import com.example.phishguard.data.local.entity.ThreatEntity

@Database(
    entities = [ThreatEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhishGuardDatabase : RoomDatabase() {
    abstract fun threatDao(): ThreatDao
}