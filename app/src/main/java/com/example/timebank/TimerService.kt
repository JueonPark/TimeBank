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

    private val timers = mutableMapOf<Int, CountDownTimer>()
    private val timeRemainingMap = mutableMapOf<Int, Long>()
    private val isRunningMap = mutableMapOf<Int, Boolean>()
    private val isAlarmPlayingMap = mutableMapOf<Int, Boolean>()
    
    private var ringtone: Ringtone? = null

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
        const val EXTRA_SECTION_ID = "EXTRA_SECTION_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sectionId = intent?.getIntExtra(EXTRA_SECTION_ID, -1) ?: -1
        
        when (intent?.action) {
            ACTION_START -> {
                if (sectionId != -1) {
                    val time = intent.getLongExtra(EXTRA_TIME_LEFT, 0L)
                    if (time > 0) {
                        timeRemainingMap[sectionId] = time
                        startTimer(sectionId)
                    }
                }
            }
            ACTION_PAUSE -> if (sectionId != -1) pauseTimer(sectionId)
            ACTION_RESET -> if (sectionId != -1) resetTimer(sectionId)
            ACTION_ADD_TIME -> {
                if (sectionId != -1) {
                    val timeToAdd = intent.getLongExtra(EXTRA_TIME_TO_ADD, 0L)
                    addTime(sectionId, timeToAdd)
                }
            }
            ACTION_REQUEST_INFO -> {
                 if (sectionId != -1) {
                     sendUpdate(sectionId, false)
                 }
            }
            ACTION_STOP_ALARM -> {
                if (sectionId != -1) {
                    stopAlarm(sectionId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(sectionId: Int) {
        stopAlarm(sectionId)
        if (isRunningMap[sectionId] == true) return

        val timeLeft = timeRemainingMap[sectionId] ?: 0L
        if (timeLeft <= 0) return

        timers[sectionId]?.cancel()
        
        val timer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMap[sectionId] = millisUntilFinished
                sendUpdate(sectionId, false)
                updateNotification()
            }

            override fun onFinish() {
                isRunningMap[sectionId] = false
                timeRemainingMap[sectionId] = 0
                playAlarm(sectionId)
                
                sendUpdate(sectionId, true)
                updateServiceState()
                showFinishedNotification(sectionId)
            }
        }.start()

        timers[sectionId] = timer
        isRunningMap[sectionId] = true
        updateServiceState()
        sendUpdate(sectionId, false)
    }

    private fun pauseTimer(sectionId: Int) {
        timers[sectionId]?.cancel()
        isRunningMap[sectionId] = false
        updateServiceState()
        sendUpdate(sectionId, false)
    }

    private fun resetTimer(sectionId: Int) {
        timers[sectionId]?.cancel()
        timeRemainingMap[sectionId] = 0
        isRunningMap[sectionId] = false
        updateServiceState()
        sendUpdate(sectionId, false)
        stopAlarm(sectionId)
    }

    private fun addTime(sectionId: Int, millis: Long) {
        val current = timeRemainingMap[sectionId] ?: 0L
        timeRemainingMap[sectionId] = current + millis
        
        stopAlarm(sectionId)
        
        if (isRunningMap[sectionId] == true) {
            timers[sectionId]?.cancel()
            isRunningMap[sectionId] = false 
            startTimer(sectionId) // Restart with new time
        } else {
             sendUpdate(sectionId, false)
        }
    }

    private fun sendUpdate(sectionId: Int, finished: Boolean) {
        if (!timeRemainingMap.containsKey(sectionId)) return

        val intent = Intent(BROADCAST_TIMER_UPDATE)
        intent.putExtra(EXTRA_SECTION_ID, sectionId)
        intent.putExtra(EXTRA_TIME_LEFT, timeRemainingMap[sectionId] ?: 0L)
        intent.putExtra(EXTRA_TIMER_FINISHED, finished)
        intent.putExtra(EXTRA_TIMER_RUNNING, isRunningMap[sectionId] ?: false)
        intent.putExtra(EXTRA_ALARM_PLAYING, isAlarmPlayingMap[sectionId] ?: false)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_HIGH 
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val activeTimers = isRunningMap.filterValues { it }.keys
        val contentText = if (activeTimers.isNotEmpty()) {
             activeTimers.joinToString(" | ") { id -> 
                 "Sec $id: ${TimeUtil.formatTime(timeRemainingMap[id] ?: 0)}"
             }
        } else {
            "TimeBank Timer"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeBank Timer")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) 
            .build()
    }
    
    private fun updateNotification() {
         if (isRunningMap.values.any { it }) {
             val notification = buildNotification()
             val manager = getSystemService(NotificationManager::class.java)
             manager.notify(1, notification)
         }
    }
    
    private fun updateServiceState() {
        if (isRunningMap.values.any { it }) {
            startForeground(1, buildNotification())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun showFinishedNotification(sectionId: Int) {
         val notificationIntent = Intent(this, MainActivity::class.java)
         val pendingIntent = android.app.PendingIntent.getActivity(
             this, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE
         )

         val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeBank Timer")
            .setContentText("Section $sectionId: Time is up!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) 
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
         val manager = getSystemService(NotificationManager::class.java)
         manager.notify(2 + sectionId, notification)
    }

    private fun playAlarm(sectionId: Int) {
        try {
            isAlarmPlayingMap[sectionId] = true
            
            // Play ringtone if not already playing
            if (ringtone == null || !ringtone!!.isPlaying) {
                var notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (notification == null) {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                ringtone?.play()
            }
            
            sendUpdate(sectionId, true) 
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm(sectionId: Int) {
        try {
            isAlarmPlayingMap[sectionId] = false
            sendUpdate(sectionId, false)
            
            // Stop ringtone if no alarms are playing
            if (isAlarmPlayingMap.values.none { it }) {
                if (ringtone != null && ringtone!!.isPlaying) {
                    ringtone?.stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        timers.values.forEach { it.cancel() }
        if (ringtone != null && ringtone!!.isPlaying) {
            ringtone?.stop()
        }
        super.onDestroy()
    }
}
