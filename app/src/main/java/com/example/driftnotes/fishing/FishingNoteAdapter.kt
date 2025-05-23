package com.example.driftnotes.fishing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ItemFishingNoteBinding
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.DateFormatter
import java.util.Locale

class FishingNoteAdapter(
    private val notes: List<FishingNote>,
    private val onItemClick: (FishingNote) -> Unit
) : RecyclerView.Adapter<FishingNoteAdapter.FishingNoteViewHolder>() {

    inner class FishingNoteViewHolder(
        private val binding: ItemFishingNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: FishingNote) {
            binding.textViewLocation.text = note.location

            // Отображаем дату или диапазон дат с помощью DateFormatter
            if (note.isMultiDay && note.endDate != null) {
                binding.textViewDate.text = DateFormatter.formatDateRange(note.date, note.endDate)
            } else {
                binding.textViewDate.text = DateFormatter.formatDate(note.date)
            }

            binding.textViewTackle.text = note.tackle

            // Загрузка первого фото (если есть)
            if (note.photoUrls.isNotEmpty()) {
                Glide.with(binding.imageViewPhoto.context)
                    .load(note.photoUrls[0])
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.imageViewPhoto)

                binding.imageViewPhoto.visibility = android.view.View.VISIBLE
            } else {
                binding.imageViewPhoto.visibility = android.view.View.GONE
            }

            // Обработчик нажатия на элемент
            binding.root.setOnClickListener {
                onItemClick(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FishingNoteViewHolder {
        val binding = ItemFishingNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FishingNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FishingNoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size
}