package com.example.timebank

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var timerRunning: Boolean = false
    private var ringtone: Ringtone? = null
    private var isAlarmPlaying: Boolean = false

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_ADD_TIME = "ACTION_ADD_TIME"
        const val ACTION_REQUEST_INFO = "ACTION_REQUEST_INFO"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        const val EXTRA_TIME_TO_ADD = "EXTRA_TIME_TO_ADD"
        const val BROADCAST_TIMER_UPDATE = "BROADCAST_TIMER_UPDATE"
        const val EXTRA_TIME_LEFT = "EXTRA_TIME_LEFT"
        const val EXTRA_TIMER_FINISHED = "EXTRA_TIMER_FINISHED"
        const val EXTRA_TIMER_RUNNING = "EXTRA_TIMER_RUNNING"
        const val EXTRA_ALARM_PLAYING = "EXTRA_ALARM_PLAYING"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val time = intent.getLongExtra(EXTRA_TIME_LEFT, 0L)
                if (time > 0) {
                    timeLeftInMillis = time
                    startTimer()
                }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
            ACTION_ADD_TIME -> {
                val timeToAdd = intent.getLongExtra(EXTRA_TIME_TO_ADD, 0L)
                addTime(timeToAdd)
            }
            ACTION_REQUEST_INFO -> {
                sendUpdate(false)
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        stopAlarm()
        if (timerRunning) return

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                sendUpdate(false)
                updateNotification()
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = 0
                playAlarm()
                // Stop foreground after starting alarm, or keep it?
                // Often better to keep foreground service while alarm is ringing if we want to ensure it plays
                // But for now, let's keep the flow as is, maybe just update notification.
                // The original code stopped foreground here. Let's keep it running for the alarm?
                // Actually, if we stop foreground, the service might be killed.
                // Let's keep foreground service alive while alarm is playing?
                // For now, adhering to user flow:
                
                sendUpdate(true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                showFinishedNotification()
            }
        }.start()

        timerRunning = true
        startForeground(1, buildNotification())
        sendUpdate(false)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        sendUpdate(false)
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = 0
        timerRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        sendUpdate(false)
        stopAlarm()
    }

    private fun addTime(millis: Long) {
        timeLeftInMillis += millis
        stopAlarm()
        if (timerRunning) {
            countDownTimer?.cancel()
            timerRunning = false 
            startTimer() // Restart with new time
        } else {
             sendUpdate(false)
        }
    }

    private fun sendUpdate(finished: Boolean) {
        val intent = Intent(BROADCAST_TIMER_UPDATE)
        intent.putExtra(EXTRA_TIME_LEFT, timeLeftInMillis)
        intent.putExtra(EXTRA_TIMER_FINISHED, finished)
        intent.putExtra(EXTRA_TIMER_RUNNING, timerRunning)
        intent.putExtra(EXTRA_ALARM_PLAYING, isAlarmPlaying)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_HIGH // Changed to HIGH for heads-up notification potential
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeBank Timer")
            .setContentText("Timer Running") // You can format time here
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification() {
         val notification = buildNotification() // Ideally update content text with time
         val manager = getSystemService(NotificationManager::class.java)
         manager.notify(1, notification)
    }

    private fun showFinishedNotification() {
         val notificationIntent = Intent(this, MainActivity::class.java)
         val pendingIntent = android.app.PendingIntent.getActivity(
             this, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE
         )

         val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeBank Timer")
            .setContentText("Time is up!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // For lock screen visibility
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
         val manager = getSystemService(NotificationManager::class.java)
         manager.notify(2, notification)
    }

    private fun playAlarm() {
        try {
            var notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
            isAlarmPlaying = true
            sendUpdate(true) // Notify UI that alarm is playing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            if (ringtone != null && ringtone!!.isPlaying) {
                ringtone?.stop()
            }
            isAlarmPlaying = false
            sendUpdate(false) // Notify UI that alarm stopped
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        countDownTimer?.cancel()
        stopAlarm()
        super.onDestroy()
    }
}
