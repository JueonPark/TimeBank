package com.example.timebank

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.concurrent.TimeUnit

class TimerSectionFragment : Fragment() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var add1MinButton: Button
    private lateinit var add5MinButton: Button
    private lateinit var add10MinButton: Button
    private lateinit var resetButton: Button
    private lateinit var stopAlarmButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var timerRunning: Boolean = false
    private var ringtone: Ringtone? = null

    private val PREFS_NAME = "TimeBankPrefs"
    private var prefPrefixKey = ""
    private var sectionNumber = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timer_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sectionNumber = arguments?.getInt("section_number") ?: 1
        prefPrefixKey = "section_${sectionNumber}_preset_time_"

        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timePrefKey = "section_${sectionNumber}_time_left"
        timeLeftInMillis = sharedPreferences.getLong(timePrefKey, 0L)

        timerText = view.findViewById(R.id.timer_text)
        startButton = view.findViewById(R.id.start_button)
        add1MinButton = view.findViewById(R.id.add_1_min_button)
        add5MinButton = view.findViewById(R.id.add_5_min_button)
        add10MinButton = view.findViewById(R.id.add_10_min_button)
        resetButton = view.findViewById(R.id.reset_button)
        stopAlarmButton = view.findViewById(R.id.stop_alarm_button)

        setupPresetButtons()

        startButton.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        resetButton.setOnClickListener {
            resetTimer()
        }
        
        stopAlarmButton.setOnClickListener {
            stopAlarm()
        }

        updateTimerText()
    }

    override fun onPause() {
        super.onPause()
        if (timerRunning) {
            pauseTimer()
        }
        stopAlarm() // Stop alarm if leaving fragment
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timePrefKey = "section_${sectionNumber}_time_left"
        sharedPreferences.edit().putLong(timePrefKey, timeLeftInMillis).apply()
    }

    private fun setupPresetButtons() {
        setupPresetButton(add1MinButton, 1, R.id.add_1_min_button)
        setupPresetButton(add5MinButton, 5, R.id.add_5_min_button)
        setupPresetButton(add10MinButton, 10, R.id.add_10_min_button)
    }

    private fun setupPresetButton(button: Button, defaultMinutes: Int, buttonId: Int) {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetKey = prefPrefixKey + buttonId
        var minutes = sharedPreferences.getInt(presetKey, defaultMinutes)
        if (minutes <= 0) {
            minutes = defaultMinutes
        }

        button.text = "Add $minutes min"
        button.tag = minutes
        button.setOnClickListener {
            val timeToAdd = (it.tag as Int) * 60 * 1000L
            addTime(timeToAdd)
        }
        button.setOnLongClickListener {
            showEditDialog(button, presetKey)
            true
        }
    }

    private fun showEditDialog(button: Button, presetKey: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Preset Time")

        val currentMinutes = button.tag as Int
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(currentMinutes.toString())
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newMinutes = input.text.toString().toIntOrNull()
            if (newMinutes != null && newMinutes > 0) {
                val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt(presetKey, newMinutes).apply()
                button.text = "Add $newMinutes min"
                button.tag = newMinutes
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun addTime(milliseconds: Long) {
        timeLeftInMillis += milliseconds
        updateTimerText()
        if (timerRunning) {
            countDownTimer?.cancel()
            startTimer()
        }
        stopAlarm() // Ensure alarm is stopped if time is added
    }

    private fun startTimer() {
        stopAlarm() // Ensure any playing alarm is stopped
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
                    playAlarm()
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
        stopAlarm()
    }

    private fun updateTimerText() {
        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis))
        val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        timerText.text = timeFormatted
    }

    private fun playAlarm() {
        try {
            var notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            ringtone = RingtoneManager.getRingtone(requireContext(), notification)
            ringtone?.play()
            stopAlarmButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            if (ringtone != null && ringtone!!.isPlaying) {
                ringtone?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopAlarmButton.visibility = View.GONE
    }

    companion object {
        fun newInstance(sectionNumber: Int): TimerSectionFragment {
            val fragment = TimerSectionFragment()
            val args = Bundle()
            args.putInt("section_number", sectionNumber)
            fragment.arguments = args
            return fragment
        }
    }
}
