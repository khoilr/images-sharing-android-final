package com.khoilr.finalproject

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ImageFragment : Fragment() {
	private lateinit var imageView: ImageView
	private lateinit var titleTextView: TextView
	private lateinit var owner: TextView
	private lateinit var chipGroup: ChipGroup
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		// Inflate the layout for this fragment
		val view = inflater.inflate(R.layout.fragment_image, container, false)
		imageView = view.findViewById(R.id.imageView)
		titleTextView = view.findViewById(R.id.title)
		owner = view.findViewById(R.id.owner)
		chipGroup = view.findViewById(R.id.chipGroup)

		val imageItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			arguments?.getParcelable("imageItem", ImageItem::class.java)
		} else {
			@Suppress("DEPRECATION")
			arguments?.getParcelable("imageItem")
		}

		// Check if the image is not null
		if (imageItem != null) {
			// Update the text view with the title of the image
			titleTextView.text = imageItem.title

			owner.text = if (imageItem.owner == null) getString(R.string.uploaded_by, "Unknown")
			else getString(R.string.uploaded_by, imageItem.owner)

			// add chip into ChipGroup for each tag
			imageItem.labels!!.forEach { label ->
				val chip = Chip(requireContext())
				chip.text = label.text
				chipGroup.addView(chip)
			}

			// Use Glide library to load the image from the url
			Glide.with(requireContext())
				.load(imageItem.url)
				.into(imageView)
		}

		return view
	}
}