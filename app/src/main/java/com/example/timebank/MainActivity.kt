package com.example.timebank

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var add1MinButton: Button
    private lateinit var add5MinButton: Button
    private lateinit var add10MinButton: Button
    private lateinit var resetButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var timerRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerText = findViewById(R.id.timer_text)
        startButton = findViewById(R.id.start_button)
        add1MinButton = findViewById(R.id.add_1_min_button)
        add5MinButton = findViewById(R.id.add_5_min_button)
        add10MinButton = findViewById(R.id.add_10_min_button)
        resetButton = findViewById(R.id.reset_button)

        startButton.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        add1MinButton.setOnClickListener {
            addTime(60 * 1000)
        }

        add5MinButton.setOnClickListener {
            addTime(5 * 60 * 1000)
        }

        add10MinButton.setOnClickListener {
            addTime(10 * 60 * 1000)
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        updateTimerText()
    }

    private fun addTime(milliseconds: Long) {
        timeLeftInMillis += milliseconds
        updateTimerText()
        if (timerRunning) {
            countDownTimer?.cancel()
            startTimer()
        }
    }

    private fun startTimer() {
        if (timeLeftInMillis > 0) {
            countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftInMillis = millisUntilFinished
                    updateTimerText()
                }

                override fun onFinish() {
                    timerRunning = false
                    startButton.text = "Start"
                    timeLeftInMillis = 0
                    updateTimerText()
                }
            }.start()

            timerRunning = true
            startButton.text = "Pause"
        }
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        startButton.text = "Start"
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = 0
        updateTimerText()
        timerRunning = false
        startButton.text = "Start"
    }

    private fun updateTimerText() {
        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis))
        val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        timerText.text = timeFormatted
    }
}
