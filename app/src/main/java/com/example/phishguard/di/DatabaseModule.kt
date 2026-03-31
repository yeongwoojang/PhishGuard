package com.example.phishguard.di

import android.content.Context
import androidx.room.Room
import com.example.phishguard.data.local.dao.ThreatDao
import com.example.phishguard.data.local.db.PhishGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhishGuardDatabase {
        return Room.databaseBuilder(
            context,
            PhishGuardDatabase::class.java,
            "phishguard.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideThreatDao(database: PhishGuardDatabase): ThreatDao {
        return database.threatDao()
    }
}