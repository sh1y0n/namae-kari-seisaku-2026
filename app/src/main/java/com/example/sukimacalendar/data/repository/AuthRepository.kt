package com.example.sukimacalendar.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ===============================================
// AuthRepository.kt
// 役割: 「ログイン」に関するFirebaseとのやり取りを一箇所にまとめる窓口。
//      画面(AuthScreen.kt)はここのメソッドを呼ぶだけで、
//      Firebase Auth / Firestoreの詳しい呼び出し方を意識しなくてよい。
//
// 認証方式: Firebase Anonymous Authentication(匿名認証)。
//      モックアップの入力欄が「ユーザー名だけ」だったため、
//      メール/パスワードは使わず、裏側で匿名アカウントを作成し、
//      Firestoreの users コレクションに表示名(displayName)だけを保存する方式にしている。
//
// Firestoreのデータ構造(このRepositoryが書き込む場所):
//   users/{uid} = { displayName: string }
// ===============================================
class AuthRepository {

    // FirebaseAuth.getInstance(): アプリ内でどこから呼んでも同じ「ログイン状態」を
    // 参照できるシングルトン(唯一のインスタンス)を取得している。
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // 今ログインしているユーザーのID(未ログインならnull)。
    // 他のRepository(GroupRepositoryなど)が「これは自分のデータか」を判定するのに使う。
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * ユーザー名を受け取り、
     *   1. まだ匿名ログインしていなければサインインする
     *   2. Firestoreの users/{uid} に表示名を保存する
     * という2ステップを行う。
     *
     * Result<Unit>で返す理由:
     *   成功/失敗をtry-catchで呼び出し側に書かせず、
     *   result.onSuccess { } / result.onFailure { } の形で扱えるようにするため。
     */
    suspend fun signInWithUsername(username: String): Result<Unit> {
        return try {
            // すでに匿名ログイン済み(アプリを再起動した場合など)ならスキップする。
            if (auth.currentUser == null) {
                auth.signInAnonymously().await() // ← .await()でTask(非同期処理)の完了を待つ
            }

            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("サインインしたのにuidが取得できませんでした")

            // set()は「新規作成 or 上書き」。ユーザー名を後で変更しても同じ書き方で使い回せる。
            firestore.collection("users").document(uid)
                .set(mapOf("displayName" to username))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
