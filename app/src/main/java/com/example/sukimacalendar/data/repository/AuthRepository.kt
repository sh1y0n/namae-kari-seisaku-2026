package com.example.sukimacalendar.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun login(userId: String, password: String): Result<Unit> {
        return try {
            val email = "$userId@sukimacalendar.local"
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(userId: String, password: String): Result<Unit> {
        return try {
            val email = "$userId@sukimacalendar.local"
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}