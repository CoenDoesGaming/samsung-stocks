package com.oneui.stocks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StockAutoSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        WidgetUpdateHelper.refreshFromYahooAsync(context, pendingResult)
    }
}
