package com.example.sukimacalendar.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AvailabilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 選択された日付たちに、指定した時間帯とメモをまとめて保存する（同一ユーザー・同一グループ・同一日は上書き）
    suspend fun saveAvailabilities(
        groupId: String,
        dates: Set<String>,
        timeSlots: List<String>,
        memo: String
    ): Result<Unit> = runCatching {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("ログインしていません")

        // 🔴 日付が選択されていない場合はここで弾く
        if (dates.isEmpty()) {
            throw IllegalStateException("日付が選択されていません")
        }

        val batch = firestore.batch()

        dates.forEach { date ->
            // 🔴 ユーザーID、グループID、日付を組み合わせた一意のドキュメントIDを作成することで上書きを実現
            val docId = "${userId}_${groupId}_${date}"
            val docRef = firestore.collection("availabilities").document(docId)

            val data = mapOf(
                "userId" to userId,
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
}