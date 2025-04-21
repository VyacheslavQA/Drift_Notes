package com.example.driftnotes.fishing

import android.view.LayoutInflater
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

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String, position: Int) {
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

            // Используем более надежную загрузку изображений с Glide
            Glide.with(binding.imageViewPhoto.context)
                .load(photoUrl)
                .apply(requestOptions)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Кэшируем изображения
                .into(binding.imageViewPhoto)

            // Обработчик клика на фото для открытия в полноэкранном режиме
            if (!isFullscreen && onPhotoClick != null) {
                binding.imageViewPhoto.setOnClickListener {
                    onPhotoClick.invoke(position)
                }
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
        photos = newPhotos
        notifyDataSetChanged()
    }
}