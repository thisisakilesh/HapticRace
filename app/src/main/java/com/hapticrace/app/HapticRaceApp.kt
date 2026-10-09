package com.hapticrace.app

import android.app.Application
import com.hapticrace.app.data.TimelineRepository
import com.hapticrace.app.haptic.HapticManager

/**
 * Application class initializing HapticManager and TimelineRepository singletons.
 */
class HapticRaceApp : Application() {

    lateinit var hapticManager: HapticManager
        private set

    lateinit var timelineRepository: TimelineRepository
        private set

    override fun onCreate() {
        super.onCreate()
        hapticManager = HapticManager(this)
        timelineRepository = TimelineRepository(this)
    }
}
