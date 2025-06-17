package com.example.projektinzyneiria   // zostaw swój pakiet

import MainApp
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.example.projektinzyneiria.ui.theme.AppTheme
import com.example.projektinzyneiria.worker.LimitCheckWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0) Tworzymy kanał dla naszych powiadomień
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "limit_exceeded"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // jeśli kanał nie istnieje, tworzymy nowy
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(
                    channelId,
                    "Limit exceeded",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Powiadomienia o przekroczeniu limitu czasu"
                }
                nm.createNotificationChannel(ch)
            }
        }

        // 1) Uruchamiamy sprawdzanie limitów
        LimitCheckWorker.enqueueFirst(this)

        // 2. UI
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainApp()
            }
        }
    }
}

