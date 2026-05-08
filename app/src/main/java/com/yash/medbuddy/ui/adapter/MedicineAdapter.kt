package com.yash.medbuddy.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.yash.medbuddy.R
import com.yash.medbuddy.model.Medicine
import java.text.SimpleDateFormat
import java.util.Locale

class MedicineAdapter(
    private val list: MutableList<Medicine>,
    private val selectedDate: String,
    private val onTaken: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val time: TextView = view.findViewById(R.id.tvTime)
        val btnTaken: AppCompatImageButton = view.findViewById(R.id.btnTaken)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val med = list[position]

        holder.name.text = med.name

        // ✅ Properly format the multiple times
        val formattedTimes = med.time.split(",")
            .joinToString("  •  ") { t ->
                formatTo12Hour(t.trim())
            }
        holder.time.text = formattedTimes

        // 📅 Highlight selected date
        if (med.date.split(",").contains(selectedDate)) {
            holder.itemView.setBackgroundColor(0xFFE3F2FD.toInt())
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        // ✅ Taken logic
        if (med.isTaken == 1) {
            holder.btnTaken.isEnabled = false
            holder.btnTaken.alpha = 0.5f
        } else {
            holder.btnTaken.isEnabled = true
            holder.btnTaken.alpha = 1.0f
            holder.btnTaken.setOnClickListener { onTaken(med) }
        }
    }

    // Moved outside of onBindViewHolder so it's a class-level helper
    private fun formatTo12Hour(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = sdf24.parse(time24)
            sdf12.format(date!!)
        } catch (e: Exception) {
            time24
        }
    }
}