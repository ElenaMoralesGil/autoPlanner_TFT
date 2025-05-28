package com.elena.autoplanner.data.repositories

import com.elena.autoplanner.domain.results.AuthResult
import com.google.firebase.auth.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class UserRepositoryImplTest {

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth
    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        userRepository = UserRepositoryImpl(firebaseAuth)
    }

    @Test
    fun `login success returns user`() = runTest {
        val email = "test@test.com"
        val password = "password123"
        whenever(firebaseUser.uid).thenReturn("user123")
        whenever(firebaseUser.email).thenReturn(email)
        whenever(firebaseUser.displayName).thenReturn("Test User")
        val mockFirebaseAuthResult = mock<com.google.firebase.auth.AuthResult>()
        whenever(mockFirebaseAuthResult.user).thenReturn(firebaseUser)
        val successfulTask: Task<com.google.firebase.auth.AuthResult> =
            Tasks.forResult(mockFirebaseAuthResult)

        whenever(firebaseAuth.signInWithEmailAndPassword(email, password))
            .thenReturn(successfulTask)

        val result = userRepository.login(email, password)

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).data
        assertEquals("user123", user.uid)
        assertEquals(email, user.email)
        assertEquals("Test User", user.displayName)
    }

    @Test
    fun `login with invalid credentials returns error`() = runTest {
        val email = "test@test.com"
        val password = "wrongpassword"

        val exception =
            FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "Invalid email")
        val failedTask: Task<com.google.firebase.auth.AuthResult> = Tasks.forException(exception)

        whenever(firebaseAuth.signInWithEmailAndPassword(email, password))
            .thenReturn(failedTask)

        val result = userRepository.login(email, password)

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid email or password.", (result as AuthResult.Error).message)
    }

    @Test
    fun `register success creates new user`() = runTest {
        val email = "new@test.com"
        val password = "password123"
        whenever(firebaseUser.uid).thenReturn("newuser123")
        whenever(firebaseUser.email).thenReturn(email)
        whenever(firebaseUser.displayName).thenReturn(null)
        val mockFirebaseAuthResult = mock<com.google.firebase.auth.AuthResult>()
        whenever(mockFirebaseAuthResult.user).thenReturn(firebaseUser)
        val successfulTask: Task<com.google.firebase.auth.AuthResult> =
            Tasks.forResult(mockFirebaseAuthResult)

        whenever(firebaseAuth.createUserWithEmailAndPassword(email, password))
            .thenReturn(successfulTask)

        val result = userRepository.register(email, password)

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).data
        assertEquals("newuser123", user.uid)
        assertEquals(email, user.email)
        assertNull(user.displayName)
    }

    @Test
    fun `register with weak password returns error`() = runTest {
        val email = "test@test.com"
        val password = "123"

        val exception = FirebaseAuthWeakPasswordException(
            "ERROR_WEAK_PASSWORD",
            "Password too weak",
            "Password should be at least 6 characters"
        )
        val failedTask: Task<com.google.firebase.auth.AuthResult> = Tasks.forException(exception)

        whenever(firebaseAuth.createUserWithEmailAndPassword(email, password))
            .thenReturn(failedTask)

        val result = userRepository.register(email, password)

        assertTrue(result is AuthResult.Error)
        assertEquals("Password is too weak.", (result as AuthResult.Error).message)
    }

    @Test
    fun `logout calls firebase auth signOut`() = runTest {
        userRepository.logout()
        verify(firebaseAuth).signOut()
    }
}