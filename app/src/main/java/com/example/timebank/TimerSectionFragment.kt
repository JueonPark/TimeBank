package com.example.timebank

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class TimerSectionFragment : Fragment() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var add5SecButton: Button
    private lateinit var add1MinButton: Button
    private lateinit var add5MinButton: Button
    private lateinit var add10MinButton: Button
    private lateinit var resetButton: Button
    private lateinit var stopAlarmButton: Button

    private var timeLeftInMillis: Long = 0

    private val PREFS_NAME = "TimeBankPrefs"
    private var prefPrefixKey = ""
    private var sectionNumber = 1
    
    private var isTimerRunning = false
    private var isAlarmPlaying = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.BROADCAST_TIMER_UPDATE) {
                val id = intent.getIntExtra(TimerService.EXTRA_SECTION_ID, -1)
                if (id == sectionNumber) {
                    timeLeftInMillis = intent.getLongExtra(TimerService.EXTRA_TIME_LEFT, 0L)
                    val finished = intent.getBooleanExtra(TimerService.EXTRA_TIMER_FINISHED, false)
                    val running = intent.getBooleanExtra(TimerService.EXTRA_TIMER_RUNNING, false)
                    isAlarmPlaying = intent.getBooleanExtra(TimerService.EXTRA_ALARM_PLAYING, false)
                    
                    isTimerRunning = running
                    updateTimerText()
                    updateStartButtonState()
                    updateStopAlarmButtonState()

                    if (finished) {
                         startButton.text = "Start"
                    }
                }
            }
        }
    }

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
        add5SecButton = view.findViewById(R.id.add_5_sec_button)
        add1MinButton = view.findViewById(R.id.add_1_min_button)
        add5MinButton = view.findViewById(R.id.add_5_min_button)
        add10MinButton = view.findViewById(R.id.add_10_min_button)
        resetButton = view.findViewById(R.id.reset_button)
        stopAlarmButton = view.findViewById(R.id.stop_alarm_button)

        add5SecButton.setOnClickListener {
            addTime(5000)
        }

        setupPresetButtons()

        startButton.setOnClickListener {
             if (isTimerRunning) {
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

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(TimerService.BROADCAST_TIMER_UPDATE)
        requireActivity().registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED)
        
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_REQUEST_INFO
        serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
        requireContext().startService(serviceIntent)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(timerReceiver)
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timePrefKey = "section_${sectionNumber}_time_left"
        sharedPreferences.edit().putLong(timePrefKey, timeLeftInMillis).apply()
    }

    private fun updateStartButtonState() {
        if (isTimerRunning) {
            startButton.text = "Pause"
        } else {
            startButton.text = "Start"
        }
    }
    
    private fun updateStopAlarmButtonState() {
        if (isAlarmPlaying) {
            stopAlarmButton.visibility = View.VISIBLE
        } else {
            stopAlarmButton.visibility = View.GONE
        }
    }

    private fun setupPresetButtons() {
        setupPresetButton(add1MinButton, 5, R.id.add_1_min_button)
        setupPresetButton(add5MinButton, 30, R.id.add_5_min_button)
        setupPresetButton(add10MinButton, 120, R.id.add_10_min_button)
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
        
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_ADD_TIME
        serviceIntent.putExtra(TimerService.EXTRA_TIME_TO_ADD, milliseconds)
        serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
        
        stopAlarm()
    }

    private fun startTimer() {
        stopAlarm()
        if (timeLeftInMillis > 0) {
            val serviceIntent = Intent(requireContext(), TimerService::class.java)
            serviceIntent.action = TimerService.ACTION_START
            serviceIntent.putExtra(TimerService.EXTRA_TIME_LEFT, timeLeftInMillis)
            serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
        }
    }

    private fun pauseTimer() {
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_PAUSE
        serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
        requireContext().startService(serviceIntent)
    }

    private fun resetTimer() {
        timeLeftInMillis = 0
        updateTimerText()
        isTimerRunning = false
        updateStartButtonState()
        stopAlarm()
        
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_RESET
        serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
        requireContext().startService(serviceIntent)
    }

    private fun updateTimerText() {
        timerText.text = TimeUtil.formatTime(timeLeftInMillis)
    }

    private fun stopAlarm() {
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_STOP_ALARM
        serviceIntent.putExtra(TimerService.EXTRA_SECTION_ID, sectionNumber)
        requireContext().startService(serviceIntent)
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
