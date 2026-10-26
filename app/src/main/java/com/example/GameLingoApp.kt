package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryRepository
import com.example.data.local.SettingsRepository
import com.example.engine.ocr.OcrEngine
import com.example.engine.translate.TranslationEngine

class GameLingoApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val ocrEngine: OcrEngine by lazy { OcrEngine() }
    val translationEngine: TranslationEngine by lazy { TranslationEngine() }

    companion object {
        const val CHANNEL_ID = "gamelingo_foreground_channel"
        lateinit var instance: GameLingoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
