package com.thomrnowtea.livetracks.audio

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Owns temporary system audio focus and restores the user's notification policy after playback. */
class PerformanceModeController(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private var previousFilter: Int? = null
    private var previousPolicy: NotificationManager.Policy? = null

    val hasNotificationPolicyAccess: Boolean
        get() = notificationManager.isNotificationPolicyAccessGranted

    fun openNotificationPolicyAccessSettings() {
        appContext.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Returns false only when exclusive notification suppression was requested but not granted. */
    fun activate(suppressNotifications: Boolean): Boolean {
        if (!suppressNotifications) return true
        if (!hasNotificationPolicyAccess) return false
        if (previousFilter == null) {
            val activated = runCatching {
                previousFilter = notificationManager.currentInterruptionFilter
                previousPolicy = notificationManager.notificationPolicy
                val silentPolicy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                )
                notificationManager.notificationPolicy = silentPolicy
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }.isSuccess
            if (!activated) {
                previousFilter = null
                previousPolicy = null
                return false
            }
        }
        return true
    }

    fun deactivate() {
        val filter = previousFilter
        if (filter != null && hasNotificationPolicyAccess) {
            runCatching {
                previousPolicy?.let { notificationManager.notificationPolicy = it }
                notificationManager.setInterruptionFilter(filter)
            }
        }
        previousFilter = null
        previousPolicy = null
    }
}
