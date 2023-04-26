package com.khoilr.finalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class ProfileFragment(private val user: FirebaseUser) : Fragment() {
	private val auth = FirebaseAuth.getInstance()

	private lateinit var displayName: TextView
	private lateinit var email: TextView
	private lateinit var imageView: ImageView

	private lateinit var signOutButton: Button
	private lateinit var editProfileButton: Button

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		// Inflate the layout for this fragment
		val view = inflater.inflate(R.layout.fragment_profile, container, false)

		displayName = view.findViewById(R.id.display_name)
		email = view.findViewById(R.id.email)
		imageView = view.findViewById(R.id.image_view)


		displayName.text =
			if (!user.displayName.isNullOrEmpty()) user.displayName
			else "No name, please edit your profile"
		email.text = user.email
		Glide.with(this).load(user.photoUrl).into(imageView)

		signOutButton = view.findViewById(R.id.sign_out_button)
		editProfileButton = view.findViewById(R.id.edit_profile_button)

		signOutButton.setOnClickListener {
			auth.signOut()
			val intent = Intent(context, MainActivity::class.java)
			startActivity(intent)
		}
		editProfileButton.setOnClickListener {
			// Create an instance of the new fragment
			val editProfileFragment = EditProfileFragment()

//			val bundle = Bundle()
//			bundle.putString("user"`)
//			editProfileFragment.arguments = bundle

			// Get the FragmentManager and FragmentTransaction
			val fragmentManager = requireActivity().supportFragmentManager
			val fragmentTransaction = fragmentManager.beginTransaction()

			// Replace the current fragment with the new one
			fragmentTransaction.replace(R.id.container, editProfileFragment)

			// Add the transaction to the back stack
			fragmentTransaction.addToBackStack(null)

			// Commit the transaction
			fragmentTransaction.commit()
		}

		return view
	}
}
