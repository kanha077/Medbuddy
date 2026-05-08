package com.yash.medbuddy.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yash.medbuddy.R
import com.yash.medbuddy.database.DBHelper
import com.yash.medbuddy.model.Medicine
import com.yash.medbuddy.ui.adapter.MedicineAdapter
import java.util.*
import java.text.SimpleDateFormat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private lateinit var list: MutableList<Medicine>

    private lateinit var tvGreeting: TextView
    private lateinit var btnAdd: Button
    private var btnSwitchUser: Button? = null
    private lateinit var tvNextName: TextView
    private lateinit var tvNextTime: TextView
    private lateinit var btnMarkTaken: Button
    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var cardNextDose: CardView
    private lateinit var tvEmpty: TextView

    private lateinit var prefs: SharedPreferences
    private var currentSelectedDate = ""
    private var nextMedicine: Medicine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        db = DBHelper(this)
        prefs = getSharedPreferences("user", Context.MODE_PRIVATE)
        tvGreeting = findViewById(R.id.tvGreeting)
        btnAdd = findViewById(R.id.btnAdd)
        btnSwitchUser = findViewById(R.id.btnSwitchUser)
        recyclerView = findViewById(R.id.recyclerView)
        tvNextName = findViewById(R.id.tvNextName)
        tvNextTime = findViewById(R.id.tvNextTime)
        btnMarkTaken = findViewById(R.id.btnMarkTaken)
        calendarView = findViewById(R.id.calendarView)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        cardNextDose = findViewById(R.id.cardNextDose)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set Initial Date
        val today = Calendar.getInstance()
        currentSelectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
        tvSelectedDate.text = getString(R.string.today)

        showUserPopup()

        // Button Listeners
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        btnSwitchUser?.setOnClickListener {
            prefs.edit().clear().apply()
            tvGreeting.text = "Hello,"
            updateRecycler(mutableListOf())
            updateNextDose(emptyList())
            showUserPopup()
        }

        // ✅ UPDATED: Call recordDoseTaken instead of markTaken
        btnMarkTaken.setOnClickListener {
            nextMedicine?.let { med ->
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                db.recordDoseTaken(med.id, currentSelectedDate, currentTime)
                Toast.makeText(this, "${med.name} marked as taken", Toast.LENGTH_SHORT).show()
                loadData()
            }
        }

        calendarView.setOnDateChangeListener { _, y, m, d ->
            currentSelectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
            tvSelectedDate.text = getString(R.string.selected_date, d, m + 1, y)
            loadData()
        }

        // Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_add -> {
                    startActivity(Intent(this, AddMedicineActivity::class.java))
                    false
                }
                else -> false
            }
        }

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        val user = prefs.getString("name", "")
        if (!user.isNullOrEmpty()) loadData()
    }

    private fun showUserPopup() {
        val savedUser = prefs.getString("name", null)
        if (!savedUser.isNullOrEmpty()) {
            tvGreeting.text = "Hello, $savedUser"
            return
        }

        val input = EditText(this)
        input.hint = "Enter your name"
        AlertDialog.Builder(this)
            .setTitle("Who is using this app?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    prefs.edit().putString("name", name).apply()
                    tvGreeting.text = "Hello, $name"
                    loadData()
                } else showUserPopup()
            }
            .show()
    }

    private fun loadData() {
        val user = prefs.getString("name", "") ?: ""
        if (user.isEmpty()) return

        list = try { db.getMedicinesByUser(user) } catch (e: Exception) { mutableListOf() }

        val filtered = list.filter {
            it.date.split(",").contains(currentSelectedDate)
        }

        // 🔥 DYNAMIC CHECK: Update isTaken based on the logs table for the selected date
        filtered.forEach { med ->
            med.isTaken = if (db.isDoseTaken(med.id, currentSelectedDate)) 1 else 0
        }

        updateRecycler(filtered.toMutableList())
        updateNextDose(filtered)
    }

    private fun updateNextDose(data: List<Medicine>) {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val pendingDoses = mutableListOf<Pair<Medicine, String>>()

        for (med in data) {
            if (med.isTaken == 0) {
                med.time.split(",").forEach { pendingDoses.add(med to it.trim()) }
            }
        }

        val nextDose = pendingDoses
            .filter { it.second >= currentTime }
            .minByOrNull { it.second }
            ?: pendingDoses.minByOrNull { it.second }

        if (nextDose == null) {
            nextMedicine = null
            tvNextName.text = "All done for today!"
            tvNextTime.text = "--:--"
            btnMarkTaken.isEnabled = false
            return
        }

        nextMedicine = nextDose.first
        tvNextName.text = nextDose.first.name
        tvNextTime.text = formatTo12Hour(nextDose.second)
        btnMarkTaken.isEnabled = true
    }

    private fun formatTo12Hour(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf12.format(sdf24.parse(time24)!!)
        } catch (e: Exception) { time24 }
    }

    private fun updateRecycler(data: MutableList<Medicine>) {
        if (data.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        // ✅ UPDATED: Call recordDoseTaken in the adapter callback
        adapter = MedicineAdapter(data, currentSelectedDate) { med ->
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            db.recordDoseTaken(med.id, currentSelectedDate, currentTime)
            loadData()
        }
        recyclerView.adapter = adapter
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}