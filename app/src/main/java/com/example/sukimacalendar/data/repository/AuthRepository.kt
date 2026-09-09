package com.example.sukimacalendar.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
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

    // 引数に userName を追加し、登録後に displayName へ保存する
    suspend fun signUp(userId: String, password: String, userName: String): Result<Unit> {
        return try {
            val email = "$userId@sukimacalendar.local"
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            // Firebase Auth のプロフィール（displayName）に入力された名前をセット
            val profileUpdates = userProfileChangeRequest {
                displayName = userName
            }
            authResult.user?.updateProfile(profileUpdates)?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}