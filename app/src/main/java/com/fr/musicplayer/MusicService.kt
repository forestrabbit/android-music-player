package com.fr.musicplayer

import android.app.*
import android.content.Intent
import android.os.IBinder

class MusicService : Service() {

    override fun onCreate() {
        super.onCreate()
        val name = "music channel"
        val descriptionText = "play music"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("music player channel", name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = Notification.Builder(this, "music player channel")
            .setContentTitle(getText(R.string.app_name))
            .setContentText("music player")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("播放音乐")
            .build()
        startForeground(1, notification)
        MainActivity.audioManager.play()
        return START_STICKY
    }

    override fun onDestroy() {
        MainActivity.audioManager.stop()
        super.onDestroy()
    }
}