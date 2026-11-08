package com.oneui.stocks

import android.app.job.JobParameters
import android.app.job.JobService

class StockSyncJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        WidgetUpdateHelper.refreshFromYahooAsync(applicationContext) {
            if (params != null) {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
