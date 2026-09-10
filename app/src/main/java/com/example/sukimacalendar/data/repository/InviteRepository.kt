package com.example.sukimacalendar.data.repository

import com.example.sukimacalendar.data.model.Invite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class InviteRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 自分宛ての招待をリアルタイムで取得する
    fun observeMyInvites(): Flow<List<Invite>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // targetUids 配列に自分のIDが含まれているものを取得
        val listener = firestore.collection("invites")
            .whereArrayContains("targetUids", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val invites = snapshot?.documents?.mapNotNull { doc ->
                    val invite = doc.toObject(Invite::class.java)
                    invite?.copy(id = doc.id)
                } ?: emptyList()

                trySend(invites)
            }

        awaitClose { listener.remove() }
    }
}