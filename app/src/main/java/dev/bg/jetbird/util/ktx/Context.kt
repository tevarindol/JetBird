package dev.bg.jetbird.util.ktx

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.bg.jetbird.ExceptionActivity
import dev.bg.jetbird.MainActivity
import dev.bg.jetbird.R

fun Context.getConfigPath(): String = "${filesDir.path}/netbird.cfg"

fun Context.hasOtherAlwaysOnVpn(): Boolean {
    val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
    val network = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        network.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
    } else {
        network.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }
}

fun Context.getPendingIntent(): PendingIntent = PendingIntent.getActivity(
    this,
    0,
    Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

fun Context.getExceptionPendingIntent(
    exception: Throwable
): PendingIntent = PendingIntent.getActivity(
    this,
    1,
    Intent(this, ExceptionActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("stacktrace", exception.stackTraceToString())
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

fun Context.sendNotification(
    channel: Pair<String, String>,
    title: String,
    description: String,
    pendingIntent: PendingIntent = getPendingIntent()
) {
    val (channelId, channelTitle) = channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                channelId,
                channelTitle,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
    val notification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_stat)
        .setContentTitle(title)
        .setContentText(description)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    with(NotificationManagerCompat.from(this)) {
        if (ActivityCompat.checkSelfPermission(
                this@sendNotification,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notify(1, notification)
    }
}
