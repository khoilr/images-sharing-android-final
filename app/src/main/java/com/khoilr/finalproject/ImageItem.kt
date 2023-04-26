package com.khoilr.finalproject

import android.os.Parcel
import android.os.Parcelable
import com.google.mlkit.vision.label.ImageLabel

data class ImageItem(
	val url: String,
	val title: String,
	val owner: String? = null,
	var labels: List<ImageLabel>? = null,
) : Parcelable {
	constructor(parcel: Parcel) : this(
		parcel.readString().toString(),
		parcel.readString().toString(),
		parcel.readString(),
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(url)
		parcel.writeString(title)
		parcel.writeString(owner)
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<ImageItem> {
		override fun createFromParcel(parcel: Parcel): ImageItem {
			return ImageItem(parcel)
		}

		override fun newArray(size: Int): Array<ImageItem?> {
			return arrayOfNulls(size)
		}
	}

}