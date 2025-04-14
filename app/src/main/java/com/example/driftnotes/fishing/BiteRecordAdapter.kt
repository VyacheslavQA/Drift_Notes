package com.example.driftnotes.fishing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driftnotes.R
import com.example.driftnotes.models.BiteRecord
import java.text.SimpleDateFormat
import java.util.Locale

class BiteRecordAdapter(
    private var bites: List<BiteRecord>,
    private val onBiteDeleteClick: (BiteRecord) -> Unit
) : RecyclerView.Adapter<BiteRecordAdapter.BiteViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    class BiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTime: TextView = view.findViewById(R.id.textViewBiteTime)
        val textFishType: TextView = view.findViewById(R.id.textViewBiteFishType)
        val textWeight: TextView = view.findViewById(R.id.textViewBiteWeight)
        val textNotes: TextView = view.findViewById(R.id.textViewBiteNotes)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDeleteBite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bite_record, parent, false)
        return BiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: BiteViewHolder, position: Int) {
        val bite = bites[position]

        // Устанавливаем время
        holder.textTime.text = timeFormat.format(bite.time)

        // Устанавливаем тип рыбы (если есть)
        if (bite.fishType.isNotEmpty()) {
            holder.textFishType.text = bite.fishType
            holder.textFishType.visibility = View.VISIBLE
        } else {
            holder.textFishType.visibility = View.GONE
        }

        // Устанавливаем вес (если есть)
        if (bite.weight > 0) {
            holder.textWeight.text = String.format("%.1f кг", bite.weight)
            holder.textWeight.visibility = View.VISIBLE
        } else {
            holder.textWeight.visibility = View.GONE
        }

        // Устанавливаем примечания (если есть)
        if (bite.notes.isNotEmpty()) {
            holder.textNotes.text = bite.notes
            holder.textNotes.visibility = View.VISIBLE
        } else {
            holder.textNotes.visibility = View.GONE
        }

        // Обработчик удаления
        holder.buttonDelete.setOnClickListener {
            onBiteDeleteClick(bite)
        }
    }

    override fun getItemCount() = bites.size

    fun updateBites(newBites: List<BiteRecord>) {
        bites = newBites
        notifyDataSetChanged()
    }
}