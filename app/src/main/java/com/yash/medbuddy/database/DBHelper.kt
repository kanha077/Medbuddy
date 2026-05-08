package com.yash.medbuddy.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.yash.medbuddy.model.Medicine

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "medbuddy.db", null, 4) { // 🔥 Version 4 for New Table

    override fun onCreate(db: SQLiteDatabase) {
        // Master table for medicine schedules
        db.execSQL(
            """
            CREATE TABLE medicines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                dosage TEXT,
                instruction TEXT,
                time TEXT,
                date TEXT,
                user TEXT,
                isTaken INTEGER DEFAULT 0
            )
            """
        )

        // NEW TABLE: Logs every specific intake per date
        db.execSQL(
            """
            CREATE TABLE medicine_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                med_id INTEGER,
                taken_date TEXT,
                taken_time TEXT
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS medicine_logs")
            db.execSQL(
                """
                CREATE TABLE medicine_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    med_id INTEGER,
                    taken_date TEXT,
                    taken_time TEXT
                )
                """
            )
        }
    }

    fun insertMedicine(med: Medicine, user: String) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("name", med.name)
        cv.put("dosage", med.dosage)
        cv.put("instruction", med.instruction)
        cv.put("time", med.time)
        cv.put("date", med.date)
        cv.put("user", user)
        cv.put("isTaken", 0) // Always default to 0 in master table
        db.insert("medicines", null, cv)
    }

    fun getMedicinesByUser(user: String): MutableList<Medicine> {
        val list = mutableListOf<Medicine>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM medicines WHERE user=?", arrayOf(user))

        if (cursor.moveToFirst()) {
            do {
                list.add(Medicine(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    dosage = cursor.getString(2),
                    instruction = cursor.getString(3),
                    time = cursor.getString(4),
                    date = cursor.getString(5),
                    isTaken = 0 // We will update this value dynamically in MainActivity
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // ✅ NEW: Records a specific intake for a specific date
    fun recordDoseTaken(id: Int, date: String, time: String) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("med_id", id)
        cv.put("taken_date", date)
        cv.put("taken_time", time)
        db.insert("medicine_logs", null, cv)
    }

    // ✅ NEW: Checks if this medicine was taken on THIS specific date
    fun isDoseTaken(id: Int, date: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM medicine_logs WHERE med_id=? AND taken_date=?",
            arrayOf(id.toString(), date)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}