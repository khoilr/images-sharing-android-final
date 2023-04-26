package com.khoilr.finalproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions


// Adapter class for image items
class ImageAdapter(
	private val context: Context,
	private val imageItems: MutableList<ImageItem>,
	private val labeling: Boolean,
) :
	RecyclerView.Adapter<ImageViewHolder>() {

	interface OnClickItemListener {
		fun onClickItem(imageItem: ImageItem)
	}


	var onClickItemListener: OnClickItemListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
		// Inflate the card layout for each item
		val view: View =
			LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
		return ImageViewHolder(view)
	}

	override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
		// Bind the data to the view holder
		val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
		val url = imageItems[position].url
		Glide
			.with(context)
			.asBitmap()
			.load(url)
			.signature(ObjectKey(url))
			.placeholder(R.drawable.skeleton_loading)
			.into(object : CustomTarget<Bitmap>() {
				override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
					holder.imageView.setImageBitmap(resource)

					if (!labeling) return

					val inputImage = InputImage.fromBitmap(resource, 0)
					labeler.process(inputImage)
						.addOnSuccessListener { labels ->
							// Handle labels
							imageItems[holder.bindingAdapterPosition].labels = labels
						}
						.addOnFailureListener { e ->
							Log.e("ImageLabeling", "Error: ${e.message}")
						}
				}

				override fun onLoadCleared(placeholder: Drawable?) {
					// Handle placeholder
				}

			})
		holder.textView.text = imageItems[position].title

		holder.itemView.setOnClickListener {
			onClickItemListener?.onClickItem(imageItems[position])
		}
	}

	override fun getItemCount(): Int {
		return imageItems.size
	}

	// Method to update the adapter's data with DiffUtil
	fun updateData(images: List<ImageItem>) {
		val diffResult = DiffUtil.calculateDiff(ImageDiffCallback(imageItems, images))
		imageItems.clear()
		imageItems.addAll(images)
		diffResult.dispatchUpdatesTo(this)
	}
}

// View holder class for image items
class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	val imageView: ImageView
	val textView: TextView

	init {
		imageView = itemView.findViewById(R.id.imageView)
		textView = itemView.findViewById(R.id.textView)
	}
}

class ImageDiffCallback(
	private val oldList: List<ImageItem>,
	private val newList: List<ImageItem>,
) : DiffUtil.Callback() {
	override fun getOldListSize(): Int {
		return oldList.size
	}

	override fun getNewListSize(): Int {
		return newList.size
	}

	override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		return oldList[oldItemPosition].url == newList[newItemPosition].url
	}

	override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		return oldList[oldItemPosition] == newList[newItemPosition]
	}
}
