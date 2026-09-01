package com.example.sukimacalendar.data.repository

import com.example.sukimacalendar.data.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

// ===============================================
// GroupRepository.kt
// 役割: 「グループ」に関するFirestoreとのやり取りを一箇所にまとめる窓口。
//      グループ画面(GroupScreen.kt)・カレンダー画面のタブ表示から使われる。
//
// Firestoreのデータ構造(このRepositoryが読み書きする場所):
//   groups/{groupId} = {
//     name: string,            … グループ名(「test group」など)
//     ownerId: string,         … 作成した人のuid
//     memberIds: [string, ...] … 参加しているメンバーのuid一覧
//   }
// ===============================================
class GroupRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * 自分が参加しているグループの一覧を「リアルタイムで」監視する。
     * 誰かがグループ名を変えたり、新しく招待されたりすると、
     * 何もしなくても自動的に最新のリストがFlowから流れてくる。
     *
     * Flow<List<Group>>で返す理由:
     *   一回きりの取得(get)ではなく、変化がある度に画面を再描画したいので、
     *   Composeの collectAsState() と相性のよい Flow の形にしている。
     *
     * callbackFlow を使う理由:
     *   Firestoreのリアルタイム購読(addSnapshotListener)はコールバック形式のAPIなので、
     *   それをFlowに変換するための標準的な書き方がcallbackFlow。
     */
    fun observeMyGroups(): Flow<List<Group>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // 未ログインなら空リストを1回流して終了する。
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // whereArrayContains: memberIdsという配列フィールドの中に
        // 自分のuidが含まれているドキュメント(=自分が入っているグループ)だけを絞り込む。
        val registration = firestore.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // エラーが起きたらFlowをエラーで閉じる
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.map { doc ->
                    Group(id = doc.id, name = doc.getString("name") ?: "")
                } ?: emptyList()
                trySend(groups) // 最新のグループ一覧をFlowに流す
            }

        // Composeの画面が破棄されてFlowの購読が終わったときに、
        // Firestoreのリスナーもちゃんと解除する(メモリリーク防止)。
        awaitClose { registration.remove() }
    }

    /**
     * 新しいグループを作成する。作成者は自動的にそのグループの最初のメンバーになる。
     */
    suspend fun createGroup(name: String): Result<Unit> {
        val uid = auth.currentUserId()
            ?: return Result.failure(IllegalStateException("ログインしていません"))

        return try {
            // UUIDでドキュメントIDを先に自分で発行している(Firestoreの自動採番も使えるが、
            // 作成前にIDを知りたい場面があるため明示的に発行する方式にしている)。
            val groupId = UUID.randomUUID().toString()
            firestore.collection("groups").document(groupId)
                .set(
                    mapOf(
                        "name" to name,
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
}

// FirebaseAuthの現在のuidを取り出す小さなヘルパー(nullなら未ログイン扱い)
private fun FirebaseAuth.currentUserId(): String? = this.currentUser?.uid
