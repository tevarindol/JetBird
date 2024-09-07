package dev.bg.jetbird

import android.app.Application
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import dev.bg.jetbird.data.JetBirdConstants
import dev.bg.jetbird.util.ktx.getExceptionPendingIntent
import dev.bg.jetbird.util.ktx.sendNotification
import timber.log.Timber

@HiltAndroidApp
class JetBird: Application() {

    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Thread.setDefaultUncaughtExceptionHandler { t, exception ->
            sendNotification(
                channel = JetBirdConstants.EXCEPTION_NOTIFICATION_CHANNEL,
                title = getString(R.string.jetbird_exception),
                description = getString(R.string.jetbird_exception_description),
                pendingIntent = getExceptionPendingIntent(exception)
            )
            defaultExceptionHandler?.uncaughtException(t, exception)
        }
    }

}
