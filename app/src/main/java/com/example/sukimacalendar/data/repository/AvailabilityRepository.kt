package com.example.sukimacalendar.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AvailabilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveAvailabilities(
        groupId: String,
        dates: Set<String>,
        timeSlots: List<String>,
        memo: String
    ): Result<Unit> = runCatching {
        val currentUser = auth.currentUser ?: throw IllegalStateException("ログインしていません")
        val userId = currentUser.uid
        val userName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "メンバー"

        if (dates.isEmpty()) {
            throw IllegalStateException("日付が選択されていません")
        }

        val batch = firestore.batch()

        dates.forEach { date ->
            val docId = "${userId}_${groupId}_${date}"
            val docRef = firestore.collection("availabilities").document(docId)

            val data = mapOf(
                "userId" to userId,
                "userName" to userName,
                "groupId" to groupId,
                "date" to date,
                "timeSlots" to timeSlots,
                "memo" to memo,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            batch.set(docRef, data)
        }

        batch.commit().await()
    }

    fun observeAvailabilitiesForGroup(groupId: String): Flow<List<Map<String, Any>>> =
        callbackFlow {
            val listener = firestore.collection("availabilities")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.plus("id" to doc.id)
                    } ?: emptyList()

                    trySend(list)
                }
            awaitClose { listener.remove() }
        }
}