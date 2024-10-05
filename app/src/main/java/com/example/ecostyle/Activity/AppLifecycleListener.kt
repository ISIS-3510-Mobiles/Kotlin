package com.example.ecostyle.Activity

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppLifecycleListener : Application.ActivityLifecycleCallbacks {

    private var activityCount = 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        activityCount++
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCount--

        if (activityCount == 0) {

            val app = activity.application as Notification
            app.checkCartAndSendNotification()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
