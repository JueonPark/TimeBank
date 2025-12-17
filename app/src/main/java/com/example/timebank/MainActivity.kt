package com.example.timebank

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.section_1 -> {
                loadFragment(TimerSectionFragment.newInstance(1))
                return@OnNavigationItemSelectedListener true
            }
            R.id.section_2 -> {
                loadFragment(TimerSectionFragment.newInstance(2))
                return@OnNavigationItemSelectedListener true
            }
            R.id.section_3 -> {
                loadFragment(TimerSectionFragment.newInstance(3))
                return@OnNavigationItemSelectedListener true
            }
            R.id.section_4 -> {
                loadFragment(TimerSectionFragment.newInstance(4))
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        // Load the default fragment
        if (savedInstanceState == null) {
            loadFragment(TimerSectionFragment.newInstance(1))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
