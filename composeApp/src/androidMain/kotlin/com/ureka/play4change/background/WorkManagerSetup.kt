package com.ureka.play4change.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the [BackgroundFetchWorker] periodic work request with the constraints defined
 * in Phase 05 Task 5.3:
 * - Period: 24 hours with a 1-hour flex window (WorkManager may run up to 1 hour early).
 * - Network: [NetworkType.UNMETERED] (WiFi only).
 * - Battery: not critically low ([Constraints.requiresBatteryNotLow]).
 *
 * Uses [ExistingPeriodicWorkPolicy.KEEP] so a running or pending job is not replaced on each app start.
 */
object WorkManagerSetup {

    fun scheduleBackgroundFetch(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BackgroundFetchWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(BackgroundFetchWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BackgroundFetchWorker.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
