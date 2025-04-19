package com.elena.autoplanner.data.repository

import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.repository.AuthResult
import com.elena.autoplanner.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.*

class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
) : UserRepository {

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
                    if (task.exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        continuation.resume(AuthResult.Error("Re-authentication required to delete account."))
                    } else {
                        continuation.resume(AuthResult.Error(errorMsg))
                    }
                }
            }
    }

    private fun com.google.firebase.auth.FirebaseUser.toDomainUser(): User {
        return User(
            uid = this.uid,
            email = this.email,
            displayName = this.displayName
        )
    }
}