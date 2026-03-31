package com.example.phishguard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhishGuardApp: Application() {
    override fun onCreate() {
        super.onCreate()
        //_ 추후 초기화 로직 추가
    }
}