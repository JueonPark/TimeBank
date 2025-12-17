package com.example.timebank

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navView = findViewById(R.id.bottom_navigation)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.section_1 -> {
                    loadFragment(TimerSectionFragment.newInstance(1))
                    true
                }
                R.id.section_2 -> {
                    loadFragment(TimerSectionFragment.newInstance(2))
                    true
                }
                R.id.section_3 -> {
                    loadFragment(TimerSectionFragment.newInstance(3))
                    true
                }
                R.id.section_4 -> {
                    loadFragment(TimerSectionFragment.newInstance(4))
                    true
                }
                else -> false
            }
        }

        setupLongClickListeners()
        loadSectionTitles()

        // Load the default fragment
        if (savedInstanceState == null) {
            loadFragment(TimerSectionFragment.newInstance(1))
            navView.selectedItemId = R.id.section_1
        }
    }

    private fun setupLongClickListeners() {
        val menu = navView.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val view = navView.findViewById<View>(menuItem.itemId)
            view?.setOnLongClickListener { 
                showRenameDialog(menuItem.itemId)
                true
            }
        }
    }

    private fun showRenameDialog(menuItemId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Section Name")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newName = input.text.toString()
            if (newName.isNotBlank()) {
                saveSectionTitle(menuItemId, newName)
                navView.menu.findItem(menuItemId).title = newName
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun saveSectionTitle(menuItemId: Int, title: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(menuItemId.toString(), title)
            apply()
        }
    }

    private fun loadSectionTitles() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        val menu = navView.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val title = sharedPref.getString(menuItem.itemId.toString(), null)
            if (title != null) {
                menuItem.title = title
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
