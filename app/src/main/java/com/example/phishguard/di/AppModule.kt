package com.example.phishguard.di

import android.content.Context
import com.example.phishguard.domain.repository.ThreatRepository
import com.example.phishguard.domain.repository.ThreatRepositoryImpl
import com.example.phishguard.ml.PhishingDetector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindThreatRepository(
        impl: ThreatRepositoryImpl
    ): ThreatRepository

    @Module
    @InstallIn(SingletonComponent::class)
    object DetectorModule {

        @Provides
        @Singleton
        fun providePhishingDetector(
            @ApplicationContext context: Context
        ): PhishingDetector {
            return PhishingDetector(context)
        }
    }
}