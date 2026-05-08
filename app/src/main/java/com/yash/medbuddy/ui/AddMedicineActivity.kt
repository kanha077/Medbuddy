package com.yash.medbuddy.ui

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.yash.medbuddy.R
import com.yash.medbuddy.database.DBHelper
import com.yash.medbuddy.model.Medicine
import com.yash.medbuddy.receiver.AlarmReceiver
import java.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView
class AddMedicineActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etDosage: EditText
    private lateinit var etInstruction: EditText
    private lateinit var btnAddDate: Button
    private lateinit var btnTime: Button
    private lateinit var btnSave: Button
    private lateinit var tvDates: TextView
    private lateinit var tvTimes: TextView

    private lateinit var db: DBHelper

    private val selectedDates = mutableListOf<String>()
    private val selectedTimes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        db = DBHelper(this)

        etName = findViewById(R.id.etName)
        etDosage = findViewById(R.id.etDosage)
        etInstruction = findViewById(R.id.etInstruction)
        btnAddDate = findViewById(R.id.btnAddDate)
        btnTime = findViewById(R.id.btnTime)
        btnSave = findViewById(R.id.btnSave)
        tvDates = findViewById(R.id.tvDates)
        tvTimes = findViewById(R.id.tvTimes)

        // 📅 Add Dates
        btnAddDate.setOnClickListener {
            val cal = Calendar.getInstance()

            DatePickerDialog(this, { _, y, m, d ->

                val date = String.format(
                    "%04d-%02d-%02d",
                    y, m + 1, d
                )

                if (!selectedDates.contains(date)) {
                    selectedDates.add(date)
                }

                tvDates.text = "Selected: ${selectedDates.joinToString(", ")}"

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // ⏰ Add Times
        btnTime.setOnClickListener {
            val cal = Calendar.getInstance()

            TimePickerDialog(this, { _, h, m ->
                val time = String.format("%02d:%02d", h, m)

                if (!selectedTimes.contains(time)) {
                    selectedTimes.add(time)
                }

                tvTimes.text = "Times: ${selectedTimes.joinToString(", ")}"

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 💾 SAVE
        btnSave.setOnClickListener {

            val name = etName.text.toString()
            val dosage = etDosage.text.toString()
            val instruction = etInstruction.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter medicine name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDates.isEmpty()) {
                Toast.makeText(this, "Add date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTimes.isEmpty()) {
                Toast.makeText(this, "Add time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medicine = Medicine(
                name = name,
                dosage = dosage,
                instruction = instruction,
                time = selectedTimes.joinToString(","),
                date = selectedDates.joinToString(",")
            )

            // 🔥 NEW: get user
            val prefs = getSharedPreferences("user", MODE_PRIVATE)
            val user = prefs.getString("name", "") ?: ""

            // 🔥 FIXED INSERT
            db.insertMedicine(medicine, user)

            // 🔔 ALARMS
            selectedDates.forEach { date ->
                selectedTimes.forEach { time ->
                    setAlarm(name, date, time)
                }
            }

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // --- ADD NAVIGATION LOGIC HERE ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Highlight the "Add" icon in the footer since we are on the Add screen
        bottomNav.selectedItemId = R.id.nav_add

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Go back to the main dashboard
                    finish()
                    true
                }
                R.id.nav_add -> {
                    // Already on the add screen, no need to do anything
                    true
                }
                else -> false
            }
        }
        // --- END OF NAVIGATION LOGIC ---
    } // End of onCreate


    private fun setAlarm(name: String, date: String, time: String) {

        val dateParts = date.split("-")
        val timeParts = time.split(":")

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.YEAR, dateParts[0].toInt())
        calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
        calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)

        // 🔥 Prevent instant trigger
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("medName", name)
        intent.putExtra("medTime", time)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (name + date + time).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}