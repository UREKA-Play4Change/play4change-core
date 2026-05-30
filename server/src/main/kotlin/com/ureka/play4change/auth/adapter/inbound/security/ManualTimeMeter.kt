package com.ureka.play4change.auth.adapter.inbound.security

import io.github.bucket4j.TimeMeter
import java.time.Duration

class ManualTimeMeter(private var nanos: Long = 0L) : TimeMeter {
    override fun currentTimeNanos(): Long = nanos
    override fun isWallClockBased(): Boolean = false
    fun addTime(duration: Duration) {
        nanos += duration.toNanos()
    }
}
