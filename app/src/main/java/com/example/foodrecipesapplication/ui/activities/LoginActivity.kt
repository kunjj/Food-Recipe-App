package com.example.foodrecipesapplication.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.foodrecipesapplication.R
import com.example.foodrecipesapplication.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private var binding: ActivityLoginBinding? = null
    private val TAG = "LoginActivity"

    @Inject
    lateinit var request: GetCredentialRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        this.binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(this.binding?.root)
        checkIfUserIsSignedIn()
        this.binding!!.googleSignInButton.setOnClickListener {
            performSignIn()
        }
    }

    private fun checkIfUserIsSignedIn() {
        if (Firebase.auth.currentUser != null) redirectToRecipeActivity()
    }

    private fun redirectToRecipeActivity() =
        startActivity(Intent(this, RecipeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        })

    private fun performSignIn() = lifecycleScope.launch {
        withContext(NonCancellable) {
            try {
                val credentialManager = CredentialManager.create(context = this@LoginActivity)
                    .getCredential(context = this@LoginActivity, request = request)
                handleSignIn(credentialManager)
            } catch (e: GetCredentialException) {
                handleFailure(e)
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        val userCredential =
                            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        Firebase.auth.signInWithCredential(userCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "signInWithCredential:success")
                                    redirectToRecipeActivity()
                                } else Log.w(TAG, "signInWithCredential:failure", task.exception)
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else Log.e(TAG, "Unexpected type of credential")
            }

            else -> Log.e(TAG, "Unexpected type of credential")
        }
    }

    private fun handleFailure(e: GetCredentialException) = e.printStackTrace()

    override fun onDestroy() {
        super.onDestroy()
        this.binding = null
    }
}