package com.khoilr.finalproject

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()
	private val storageRef = Firebase.storage.reference
	private val firestore = Firebase.firestore
	private var user: FirebaseUser? = null
	private lateinit var bottomNavigationView: BottomNavigationView
	private val readRequestCode = 100

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		user = auth.currentUser

		val loginLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					user = auth.currentUser
					replaceFragment(ProfileFragment(user!!))
				}
			}
		// Initialize the bottom app bar and the bottom navigation view
		bottomNavigationView = findViewById(R.id.bottom_navigation_view)

		// Set the listener for the bottom navigation view
		bottomNavigationView.setOnItemSelectedListener { item ->
			when (item.itemId) {
				// Handle the home item click
				R.id.home -> {
//					getImagesFromFirebase { images ->
//						replaceFragment(ImagesGridFragment(images))
//					}
					replaceFragment(ImagesGridFragment(getDummyImages()))
					true
				}
				// Handle the gallery item click
				R.id.gallery -> {
					replaceFragment(ImagesGridFragment(getLocalImages(this)))
					true
				}
				// Handle the profile item click
				R.id.profile -> {
					// open Login Activity
					if (user == null) {
						val intent = Intent(this, LoginActivity::class.java)
						loginLauncher.launch(intent)
					} else {
						replaceFragment(ProfileFragment(user!!))
					}
//					val intent = Intent(this, LoginActivity::class.java)
//					startActivity(intent)

//					replaceFragment(LoginFragment())
					true
				}

				else -> false
			}
		}

		// open home screen by default
		bottomNavigationView.selectedItemId = R.id.home
	}

	override fun onResume() {
		super.onResume()
		if (user != null && checkPermission())
			runBackgroundTask()
	}


	// Create a function to run a background task using coroutines
	private fun runBackgroundTask() = CoroutineScope(Dispatchers.IO).launch {
		// Get the local images as a list of ImageItem objects
		val images = getLocalImages(this@MainActivity.applicationContext)
		// Loop through each image and upload it to firebase storage
		for (image in images) {
			uploadImageToFirebase(image)
		}
	}

	// Create a function to upload an image to firebase storage and create a document in cloud firestore
	private fun uploadImageToFirebase(image: ImageItem) {
		// Get the firebase storage reference and create a child reference with the image name
		val imageRef = storageRef.child("images/${user?.uid}-${image.title}")

		imageRef.downloadUrl.addOnFailureListener {
			// Upload the image uri to the storage reference and add a success listener
			imageRef.putFile(image.url.toUri()).addOnSuccessListener {
				// Get the download url of the uploaded image and add a success listener
				imageRef.downloadUrl.addOnSuccessListener { url ->
					// Create a map of data to store in cloud firestore document
					val data = hashMapOf(
						"imageUrl" to url.toString(),
						"imageName" to image.title,
						"owner" to (user?.uid)
					)

					// Get the firebase firestore instance and collection reference
					val collectionRef = firestore.collection("images")

					// Add a new document to the collection with the data map and add a success listener
					collectionRef.add(data).addOnSuccessListener { document ->
						// Log a message with the document id
						Log.d("UploadImage", "Document added with id: ${document.id}")
					}.addOnFailureListener { exception ->
						// Log an error message with the exception
						Log.e("UploadImage", "Error adding document", exception)
					}
				}.addOnFailureListener { exception ->
					// Log an error message with the exception
					Log.e("UploadImage", "Error getting download url", exception)
				}
			}.addOnFailureListener { exception ->
				// Log an error message with the exception
				Log.e("UploadImage", "Error uploading image", exception)
			}
		}
	}

	private fun getImagesFromFirebase(callback: (MutableList<ImageItem>) -> Unit) {
		val imageItems = mutableListOf<ImageItem>()
		val collection = firestore.collection("images")
		collection.addSnapshotListener { query, exception ->
			if (exception != null) {
				Log.d("UploadImage", "Error getting documents: ", exception)
				return@addSnapshotListener
			}

			query?.documents?.forEach { document ->
				val owner =
					firestore.collection("users")
						.document(document.data?.get("owner").toString())
				var ownerName: String? = null
				owner.get().addOnSuccessListener { ownerName = it.data?.get("name").toString() }
				imageItems.add(
					if (ownerName != null) ImageItem(
						document.data?.get("imageUrl").toString(),
						document.data?.get("imageName").toString(),
						ownerName!!
					)
					else
						ImageItem(
							document.data?.get("imageUrl").toString(),
							document.data?.get("imageName").toString(),
						)
				)
			}
			callback(imageItems)
		}
	}

	private fun getDummyImages(): MutableList<ImageItem> {
		val randomHeights = (1..100).map { (250..450).random() }
		val randomWidths = (1..100).map { (250..450).random() }

		val randomImages = mutableListOf<ImageItem>()
		for (i in 0..99) {
			randomImages.add(
				ImageItem(
					"https://picsum.photos/${randomWidths[i]}/${randomHeights[i]}",
					"Image $i"
				)
			)
		}
		return randomImages
	}

	private fun permissionRequest() {
		// Check if the app has the read external storage permission
		val hasReadPermission: Boolean = checkPermission()

		// If not, request the permission
		if (!hasReadPermission) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ActivityCompat.requestPermissions(
				this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), readRequestCode
			)
			else ActivityCompat.requestPermissions(
				this, arrayOf(
					Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET
				), readRequestCode
			)
		}
	}

	private fun checkPermission(): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(
			this, Manifest.permission.READ_MEDIA_IMAGES
		) == PackageManager.PERMISSION_GRANTED
		else ContextCompat.checkSelfPermission(
			this, Manifest.permission.READ_EXTERNAL_STORAGE
		) == PackageManager.PERMISSION_GRANTED
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray,
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == readRequestCode)
		// Check if the permission was granted or denied
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) bottomNavigationView.selectedItemId =
				R.id.gallery
			else
			// Permission was denied, show a message to the user
				Toast.makeText(
					this,
					"Read external storage permission denied",
					Toast.LENGTH_SHORT
				)
					.show()
	}

	private fun replaceFragment(fragment: Fragment) {
		// Get the fragment manager
		val fragmentManager = supportFragmentManager

		// Begin a fragment transaction
		val fragmentTransaction = fragmentManager.beginTransaction()

		// Replace the container with the new fragment
		fragmentTransaction.replace(R.id.container, fragment)

		// Commit the transaction
		fragmentTransaction.commit()
	}

	private fun getLocalImages(context: Context): MutableList<ImageItem> {
		permissionRequest()

		// Get a content resolver
		val contentResolver = context.contentResolver

		// Define the columns to retrieve
		val projection = arrayOf(
			MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME
		)

		// Define the sort order
		val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} ASC"

		// Query the MediaStore
		val cursor = contentResolver.query(
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder
		)

		// Create a list to store the images
		val images = mutableListOf<ImageItem>()

		// Loop through the cursor and get the image uri and label
		cursor?.use {
			val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
			val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

			while (it.moveToNext()) {
				// Get the image id
				val id = it.getLong(idColumn)

				// Get the image uri
				val uri = ContentUris.withAppendedId(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
				)

				// Get the image label
				val name = it.getString(nameColumn)

				// Create an image object and add it to the list
				val image = ImageItem(uri.toString(), name)
				images.add(image)
			}
		}

		return images
	}
}

