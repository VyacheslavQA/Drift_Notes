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

                // Проверяем наличие ProgressBar
                val progressBar = binding.root.findViewById<View>(R.id.progressBar)
                progressBar?.visibility = View.VISIBLE

                val requestOptions = if (isFullscreen) {
                    RequestOptions()
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .error(R.drawable.ic_photo_placeholder)
                        .fitCenter() // для полноэкранного режима используем fitCenter
                } else {
                    RequestOptions()
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .error(R.drawable.ic_photo_placeholder)
                        .centerCrop() // для просмотра в карточке используем centerCrop
                }

                // Улучшенная загрузка изображений с Glide
                Glide.with(binding.root.context)
                    .load(photoUrl)
                    .apply(requestOptions)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Кэшируем изображения
                    .into(binding.imageViewPhoto)

                // Скрываем прогресс-бар после загрузки
                progressBar?.visibility = View.GONE

                // Обработчик клика на фото для открытия в полноэкранном режиме
                if (!isFullscreen && onPhotoClick != null) {
                    binding.imageViewPhoto.setOnClickListener {
                        onPhotoClick.invoke(position)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке фото на позиции $position", e)
                // В случае ошибки показываем заполнитель
                binding.imageViewPhoto.setImageResource(R.drawable.ic_photo_placeholder)
                // Скрываем прогресс-бар
                binding.root.findViewById<View>(R.id.progressBar)?.visibility = View.GONE
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