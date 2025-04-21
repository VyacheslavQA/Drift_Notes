// Путь: app/src/main/java/com/example/driftnotes/fishing/PhotoPagerAdapter.kt
package com.example.driftnotes.fishing

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ItemPhotoBinding

class PhotoPagerAdapter(
    private var photos: List<String>,
    private val isFullscreen: Boolean = false,
    private val onPhotoClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    private val TAG = "PhotoPagerAdapter"

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String, position: Int) {
            try {
                Log.d(TAG, "Загрузка фото на позиции $position: $photoUrl")

                // Показываем прогресс при начале загрузки
                binding.progressBar.visibility = View.VISIBLE

                // Настраиваем обработчик клика, если не в полноэкранном режиме
                if (!isFullscreen && onPhotoClick != null) {
                    binding.imageViewPhoto.setOnClickListener {
                        onPhotoClick.invoke(position)
                    }
                }

                // Простая версия без сложных обработчиков загрузки
                Glide.with(binding.root.context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply {
                        if (isFullscreen) {
                            fitCenter()
                        } else {
                            centerCrop()
                        }
                    }
                    .into(binding.imageViewPhoto)

                // Скрываем индикатор загрузки с небольшой задержкой
                binding.root.postDelayed({
                    binding.progressBar.visibility = View.GONE
                }, 300)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке фото на позиции $position: ${e.message}", e)
                // В случае ошибки показываем заполнитель
                binding.imageViewPhoto.setImageResource(R.drawable.ic_photo_placeholder)
                // Скрываем прогресс
                binding.progressBar.visibility = View.GONE
            }
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
            holder.bind(photos[position], position)
        }
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<String>) {
        Log.d(TAG, "Обновление списка фотографий: ${newPhotos.size} шт")
        photos = newPhotos
        notifyDataSetChanged()
    }
}