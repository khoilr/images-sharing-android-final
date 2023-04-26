package com.khoilr.finalproject

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()
	private val firestore = Firebase.firestore

//	private lateinit var googleSignInClient: GoogleSignInClient
//	private val googleSignInLauncher =
//		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//			if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
//
//			// Handle the sign-in result
//			val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
//			try {
//				// Google Sign In was successful, authenticate with Firebase
//				val account = task.getResult(ApiException::class.java)
//
//				// Get the Google ID token and exchange it for a Firebase credential
//				val idToken = account.idToken
//				val credential = GoogleAuthProvider.getCredential(idToken, null)
//
//				// Sign in with Firebase using the credential
//				auth.signInWithCredential(credential)
//					.addOnCompleteListener(this) { authResultTask ->
//						if (authResultTask.isSuccessful) {
//							// Sign in success
//							val user = auth.currentUser
//							//								updateUI(user)
//						} else {
//							// Sign in failed
//							Log.w(
//								TAG, "signInWithCredential:failure", authResultTask.exception
//							)
//							//								updateUI(null)
//						}
//					}
//				Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
//			} catch (e: ApiException) {
//				// Google Sign In failed
//				Log.w(TAG, "Google sign in failed", e)
//			}
//		}

	private val email: TextInputEditText by lazy { findViewById(R.id.email_input) }
	private val password: TextInputEditText by lazy { findViewById(R.id.password_input) }
	private val loginButton: MaterialButton by lazy { findViewById(R.id.login_button) }
	private val googleButton: MaterialButton by lazy { findViewById(R.id.google_button) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_login)

//		// Initialize Google Sign In Client
//		val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//		.requestIdToken(getString(R.string.default_web_client_id))
//			.requestEmail().build()
//		googleSignInClient = GoogleSignIn.getClient(
//			this, gso
//		)

		// Button event handlers
		loginButton.setOnClickListener {
			signIn()
		}
		googleButton.setOnClickListener {
//			signInWithGoogle()
			Toast.makeText(
				this, "Google sign in is not available yet", Toast.LENGTH_SHORT
			).show()
		}
	}

//	private fun signInWithGoogle() {
//		val signInIntent = googleSignInClient.signInIntent
//		googleSignInLauncher.launch(signInIntent)
//	}

	// Create a sign in method
	private fun signIn() {// check if email or password is not inputted
		if (email.text.isNullOrEmpty() || password.text.isNullOrEmpty()) {
			// show dialog to ask user to input email and password
			Toast.makeText(
				this, "Please input email and password", Toast.LENGTH_SHORT
			).show()
			return
		}

		val emailValue = email.text.toString()
		val passwordValue = password.text.toString()

		// Sign in with email and password using Firebase Authentication
		auth.signInWithEmailAndPassword(emailValue, passwordValue).addOnCompleteListener { task ->
			if (task.isSuccessful) {
				// Sign in success
				Log.d(TAG, "signInWithEmail:success")
				Toast.makeText(
					this, "Sign in success", Toast.LENGTH_SHORT
				).show()
				auth.currentUser
				this.setResult(RESULT_OK)
				finish()
			} else {
				// Sign in failure
				Log.w(TAG, "signInWithEmail:failure", task.exception)
				// Check if the user account exists
				auth.fetchSignInMethodsForEmail(emailValue).addOnCompleteListener { taskChild ->
					if (taskChild.isSuccessful) {
						val result = taskChild.result
						val signInMethods = result.signInMethods
						if (signInMethods.isNullOrEmpty()) {
							// No user account exists with this email, create a new one
							createAccount(emailValue, passwordValue)
						} else {
							// User account exists but password is wrong, show a toast message
							Log.w(TAG, "signInWithEmail:failure", task.exception)
						}
					} else {
						// Error fetching sign in methods for email, show a toast message
						Log.w(TAG, "signInWithEmail:failure", task.exception)
					}
				}
			}
		}
	}

	// Create an account method
	private fun createAccount(email: String, password: String) {
		// Create a new user account with email and password using Firebase Authentication
		auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
			if (task.isSuccessful) {
				// Account creation success
				Toast.makeText(
					this, "Account created", Toast.LENGTH_SHORT
				).show()
				val user = auth.currentUser

				// create a user in users collection
				val userMap = hashMapOf(
					"email" to email,
					"uid" to user?.uid
				)
				user?.let { firestore.collection("users").document(it.uid).set(userMap) }

				this.setResult(RESULT_OK)
				finish()
				// Do something with the user object
			} else {
				// Account creation failure
				Log.w(TAG, "createUserWithEmail:failure", task.exception)
				// Show a toast message with the error
				Toast.makeText(
					this, "Error: " + task.exception, Toast.LENGTH_SHORT
				).show()
			}
		}
	}
}