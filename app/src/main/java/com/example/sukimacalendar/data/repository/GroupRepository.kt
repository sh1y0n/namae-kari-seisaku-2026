package com.example.sukimacalendar.data.repository

import com.example.sukimacalendar.data.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * 自分が参加しているグループの一覧をリアルタイム監視
     */
    fun observeMyGroups(): Flow<List<Group>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.map { doc ->
                    Group(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        inviteCode = doc.getString("inviteCode") ?: ""
                    )
                } ?: emptyList()
                trySend(groups)
            }

        awaitClose { registration.remove() }
    }

    /**
     * グループを作成（6桁のランダム招待コードとパスワードを設定）
     */
    suspend fun createGroup(name: String, password: String): Result<Unit> {
        val uid = auth.currentUserId()
            ?: return Result.failure(IllegalStateException("ログインしていません"))

        return try {
            val groupId = UUID.randomUUID().toString()
            // 6桁のランダムな招待コードを生成
            val inviteCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

            firestore.collection("groups").document(groupId)
                .set(
                    mapOf(
                        "name" to name,
                        "inviteCode" to inviteCode,
                        "password" to password,
                        "ownerId" to uid,
                        "memberIds" to listOf(uid)
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 6桁の招待コードとパスワードを指定してグループに参加する
     */
    suspend fun joinGroupWithCode(inviteCode: String, inputPassword: String): Result<Unit> {
        val uid = auth.currentUserId()
            ?: return Result.failure(IllegalStateException("ログインしていません"))

        return try {
            val querySnapshot = firestore.collection("groups")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(IllegalStateException("有効な招待コードが見つかりません"))
            }

            val doc = querySnapshot.documents[0]
            val correctPassword = doc.getString("password") ?: ""

            if (correctPassword != inputPassword) {
                return Result.failure(IllegalStateException("パスワードが間違っています"))
            }

            val docRef = doc.reference
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val memberIds = snapshot.get("memberIds") as? List<String> ?: emptyList()
                if (!memberIds.contains(uid)) {
                    val newMemberIds = memberIds + uid
                    transaction.update(docRef, "memberIds", newMemberIds)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * グループを削除
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * グループのパスワードを取得する
     */
    suspend fun getPassword(groupId: String): Result<String> {
        return try {
            val doc = firestore.collection("groups").document(groupId).get().await()
            val password = doc.getString("password") ?: ""
            Result.success(password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * グループのパスワードを変更する
     */
    suspend fun updatePassword(groupId: String, newPassword: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .update("password", newPassword)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun FirebaseAuth.currentUserId(): String? = this.currentUser?.uid