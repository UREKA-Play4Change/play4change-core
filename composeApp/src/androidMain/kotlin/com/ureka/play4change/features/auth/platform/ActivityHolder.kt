package com.ureka.play4change.features.auth.platform

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the current foreground [ComponentActivity].
 * Bound in [com.ureka.play4change.MainActivity.onCreate] and released in
 * [com.ureka.play4change.MainActivity.onDestroy].
 *
 * Using a WeakReference ensures the Activity is not leaked if the holder
 * outlives the Activity (e.g. process-level singleton).
 */
internal object ActivityHolder {

    @Volatile private var ref: WeakReference<ComponentActivity>? = null

    fun bind(activity: ComponentActivity) {
        ref = WeakReference(activity)
    }

    fun release() {
        ref = null
    }

    fun require(): ComponentActivity =
        ref?.get() ?: error("No Activity bound — did MainActivity.onCreate() run?")
}
