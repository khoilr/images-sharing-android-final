package com.khoilr.finalproject

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class EditProfileFragment : Fragment() {
	private lateinit var avatarImageView: ImageView
	private lateinit var displayNameEdit: TextInputEditText
	private lateinit var email: TextInputEditText
	private lateinit var saveButton: Button
	private lateinit var editAvatarButton: ImageButton

	private val auth: FirebaseAuth = FirebaseAuth.getInstance()
	private val storageRef = Firebase.storage.reference
	private lateinit var imageUri: Uri
	private lateinit var imageRef: StorageReference
	private lateinit var uploadTask: UploadTask


	// Create a URI object for the image

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

		avatarImageView = view.findViewById(R.id.imageView)
		displayNameEdit = view.findViewById(R.id.display_name)
		email = view.findViewById(R.id.email)
		saveButton = view.findViewById(R.id.save_button)
		editAvatarButton = view.findViewById(R.id.edit_avatar_button)

		val user = auth.currentUser

		displayNameEdit.setText(user?.displayName)
		email.setText(user?.email)
		Glide.with(this).load(user?.photoUrl).into(avatarImageView)

		@Suppress("DEPRECATION") val imageCaptureLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				// Check if the result is OK
				if (result.resultCode == Activity.RESULT_OK) {
					// Get the data from the result
					val data = result.data

					// Check if the data is not null
					if (data != null) {
						// Get the bitmap from the data
						val bitmap = data.extras?.get("data") as Bitmap
						avatarImageView.setImageBitmap(bitmap)

						val byteArrayOutputStream = ByteArrayOutputStream()
						bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
						val byteArray = byteArrayOutputStream.toByteArray()
						imageRef = storageRef.child("images/${System.currentTimeMillis()}")
						uploadTask = imageRef.putBytes(byteArray)
					}
				}
			}

		val imagePickLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				// Check if the result is OK
				if (result.resultCode == Activity.RESULT_OK) {
					// Get the data from the result
					val data = result.data

					// Check if the data is not null
					if (data != null) {
						// Get the URI from the data
						imageUri = data.data!!
						avatarImageView.setImageURI(imageUri)
						imageRef = storageRef.child("avatars/${imageUri.lastPathSegment}")
						uploadTask = imageRef.putFile(imageUri)
					}
				}
			}

		editAvatarButton.setOnClickListener {
			val popupMenu = PopupMenu(context, editAvatarButton)
			popupMenu.menuInflater.inflate(R.menu.menu_edit_avatar, popupMenu.menu)
			popupMenu.setForceShowIcon(true)
			popupMenu.setOnMenuItemClickListener { item ->
				when (item.itemId) {
					R.id.take_picture -> {
						val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
						imageCaptureLauncher.launch(intent)
						true
					}

					R.id.choose_from_gallery -> {
						val intent =
							Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
						imagePickLauncher.launch(intent)
						true
					}

					else -> false
				}
			}
			popupMenu.show()
		}

		saveButton.setOnClickListener {
			uploadTask.addOnSuccessListener {
				imageRef.downloadUrl.addOnCompleteListener { uri ->
					user?.updateProfile(userProfileChangeRequest {
						displayName = displayNameEdit.text.toString()
						photoUri = uri.result
					})?.addOnCompleteListener { task ->
						if (task.isSuccessful) {
							Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
							// upload firestore database
							val fragmentManager = parentFragmentManager
							fragmentManager.popBackStack()
						}
					}
				}
			}.addOnFailureListener {
				Toast.makeText(requireContext(), "Fail", Toast.LENGTH_SHORT).show()
			}
		}

		return view
	}
}