package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Initializing : AuthState()
    data class Authenticated(val user: FirebaseUser, val idToken: String? = null) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {
    private var auth: FirebaseAuth? = null
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState

    init {
        initializeFirebaseSafely()
    }

    private fun initializeFirebaseSafely() {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("spatial-context-notes")
                    .setApiKey("AIzaSySpatialContextLocalFallback")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            auth = FirebaseAuth.getInstance(app).apply {
                addAuthStateListener { firebaseAuth ->
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        _authState.value = AuthState.Authenticated(currentUser)
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase Auth initialization warning: ${e.message}", e)
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(webClientId: String? = null): Result<FirebaseUser> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .apply {
                    if (!webClientId.isNullOrBlank()) {
                        setServerClientId(webClientId)
                    }
                }
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                val currentFirebaseAuth = auth
                if (currentFirebaseAuth != null) {
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = currentFirebaseAuth.signInWithCredential(authCredential).await()
                    val user = authResult.user ?: throw IllegalStateException("Firebase user was null after sign-in")
                    _authState.value = AuthState.Authenticated(user, idToken)
                    Result.success(user)
                } else {
                    val errorMsg = "Firebase Auth not active on this device"
                    _authState.value = AuthState.Error(errorMsg)
                    Result.failure(IllegalStateException(errorMsg))
                }
            } else {
                val errorMsg = "Unexpected credential type: ${credential.type}"
                Log.e("AuthManager", errorMsg)
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("AuthManager", "User cancelled Google Sign-In: ${e.message}")
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential Manager error", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Credential retrieval failed")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in exception", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Authentication failed")
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: ClearCredentialException) {
            Log.w("AuthManager", "Failed to clear credential state", e)
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out error", e)
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    suspend fun getAccessToken(): String? {
        val authS = _authState.value
        return if (authS is AuthState.Authenticated) {
            authS.idToken ?: auth?.currentUser?.getIdToken(false)?.await()?.token
        } else {
            null
        }
    }
}
