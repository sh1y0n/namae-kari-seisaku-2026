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

        // memberIds 配列に含まれているかを監視
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
     * グループを作成（UIDとdisplayNameを members マップリストとして保存）
     */
    suspend fun createGroup(name: String, password: String): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("ログインしていません"))
        val uid = currentUser.uid
        val userName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "メンバー"

        return try {
            val groupId = UUID.randomUUID().toString()
            val inviteCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

            val initialMember = mapOf(
                "uid" to uid,
                "name" to userName
            )

            firestore.collection("groups").document(groupId)
                .set(
                    mapOf(
                        "name" to name,
                        "inviteCode" to inviteCode,
                        "password" to password,
                        "ownerId" to uid,
                        "memberIds" to listOf(uid),
                        "members" to listOf(initialMember) // 👈 名前をセットで保存
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 招待コードでグループに参加（自分の displayName も members に追加）
     */
    suspend fun joinGroupWithCode(inviteCode: String, inputPassword: String): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("ログインしていません"))
        val uid = currentUser.uid
        val userName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "メンバー"

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

                @Suppress("UNCHECKED_CAST")
                val members = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()

                if (!memberIds.contains(uid)) {
                    val newMemberIds = memberIds + uid
                    val newMembers = members + mapOf("uid" to uid, "name" to userName)

                    transaction.update(docRef, mapOf(
                        "memberIds" to newMemberIds,
                        "members" to newMembers
                    ))
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
     * パスワード取得
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
     * パスワード変更
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

    /**
     * グループのメンバー一覧（保存されている name 付きのマップリスト）を取得
     */
    suspend fun getGroupMembers(groupId: String): Result<List<Map<String, String>>> {
        return try {
            val doc = firestore.collection("groups").document(groupId).get().await()
            @Suppress("UNCHECKED_CAST")
            val members = doc.get("members") as? List<Map<String, String>> ?: emptyList()

            // 万が一古いグループで members フィールドがない場合のフォールバック
            if (members.isEmpty()) {
                val memberIds = doc.get("memberIds") as? List<String> ?: emptyList()
                val fallbackMembers = memberIds.map { uid ->
                    mapOf("uid" to uid, "name" to "メンバー (${uid.take(4)})")
                }
                return Result.success(fallbackMembers)
            }

            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * メンバー削除（memberIds と members の両方から除外）
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val memberIds = snapshot.get("memberIds") as? List<String> ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val members = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()

                val newMemberIds = memberIds.filter { it != userId }
                val newMembers = members.filter { it["uid"] != userId }

                transaction.update(docRef, mapOf(
                    "memberIds" to newMemberIds,
                    "members" to newMembers
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun FirebaseAuth.currentUserId(): String? = this.currentUser?.uid