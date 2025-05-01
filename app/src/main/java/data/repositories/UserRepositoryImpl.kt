package com.elena.autoplanner.data.repositories

import android.util.Log
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.repositories.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.*

class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomainUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): AuthResult<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.toDomainUser()?.let {
                AuthResult.Success(it)
            } ?: AuthResult.Error("Login failed: User data not found.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Invalid email or password.")
        } catch (e: Exception) {
            AuthResult.Error("Login failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun register(email: String, password: String): AuthResult<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.toDomainUser()?.let {
                AuthResult.Success(it)
            } ?: AuthResult.Error("Registration failed: User data not found.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthResult.Error("Password is too weak.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Invalid email format.")
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthResult.Error("Email already in use.")
        } catch (e: Exception) {
            AuthResult.Error("Registration failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): AuthResult<Unit> = suspendCoroutine { continuation ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            continuation.resume(AuthResult.Error("No user logged in to delete."))
            return@suspendCoroutine
        }

        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(AuthResult.Success(Unit))
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Failed to delete account."
                    // Check if re-authentication is required
                    if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                        continuation.resume(AuthResult.Error("Re-authentication required to delete account."))
                    } else {
                        continuation.resume(AuthResult.Error(errorMsg))
                    }
                }
            }
    }

    override suspend fun updateEmail(newEmail: String): AuthResult<Unit> {
        val user = firebaseAuth.currentUser ?: return AuthResult.Error("No user logged in.")
        return try {
            user.verifyBeforeUpdateEmail(newEmail)
                .await() // Use verifyBeforeUpdateEmail for better security
            // Email verification link sent. Actual update happens after user clicks the link.
            // Inform the user to check their email.
            AuthResult.Success(Unit) // Indicate verification email sent
        } catch (e: FirebaseAuthWeakPasswordException) { // Should not happen for email, but include for safety
            AuthResult.Error("Invalid email format.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Invalid email format.")
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthResult.Error("Email already in use by another account.")
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            AuthResult.Error("Re-authentication required to update email.")
        } catch (e: Exception) {
            Log.e(TAG, "Update email error", e)
            AuthResult.Error("Failed to update email: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun updatePassword(newPassword: String): AuthResult<Unit> {
        val user = firebaseAuth.currentUser ?: return AuthResult.Error("No user logged in.")
        return try {
            user.updatePassword(newPassword).await()
            AuthResult.Success(Unit)
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthResult.Error("New password is too weak.")
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            AuthResult.Error("Re-authentication required to update password.")
        } catch (e: Exception) {
            Log.e(TAG, "Update password error", e)
            AuthResult.Error("Failed to update password: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun updateDisplayName(newDisplayName: String): AuthResult<Unit> {
        val user = firebaseAuth.currentUser ?: return AuthResult.Error("No user logged in.")
        return try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build()
            user.updateProfile(profileUpdates).await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update display name error", e)
            AuthResult.Error("Failed to update display name: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun reauthenticate(password: String): AuthResult<Unit> {
        val user = firebaseAuth.currentUser ?: return AuthResult.Error("No user logged in.")
        val email =
            user.email ?: return AuthResult.Error("User email not found for re-authentication.")

        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            AuthResult.Success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Incorrect current password.")
        } catch (e: Exception) {
            Log.e(TAG, "Re-authentication error", e)
            AuthResult.Error("Re-authentication failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun FirebaseUser.toDomainUser(): User {
        return User(
            uid = this.uid,
            email = this.email,
            displayName = this.displayName
        )
    }
}