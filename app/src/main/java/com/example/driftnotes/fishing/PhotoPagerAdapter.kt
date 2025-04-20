package com.example.driftnotes.fishing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ItemPhotoBinding

class PhotoPagerAdapter(
    private var photos: List<String>
) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String) {
            // Используем более надежную загрузку изображений с Glide
            Glide.with(binding.imageViewPhoto.context)
                .load(photoUrl)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Кэшируем изображения
                .centerCrop() // Обеспечиваем заполнение всей области
                .into(binding.imageViewPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        if (position < photos.size) {
            holder.bind(photos[position])
        }
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<String>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}