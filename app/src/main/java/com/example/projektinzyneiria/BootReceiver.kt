package com.example.projektinzyneiria

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.projektinzyneiria.worker.LimitCheckWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LimitCheckWorker.enqueueFirst(ctx)
        }
    }
}

