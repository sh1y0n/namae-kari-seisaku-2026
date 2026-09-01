package com.example.sukimacalendar.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sukimacalendar.data.repository.AuthRepository
import kotlinx.coroutines.launch

// ===============================================
// AuthScreen.kt
// 役割: アプリを最初に開いたときの画面。ユーザー名を入力してログイン/登録する。
//
// 今の実装状態(DB接続後):
//   ボタンを押すと AuthRepository.signInWithUsername() を呼び、
//   実際にFirebase Anonymous Authでサインインし、Firestoreにユーザー名を保存する。
//   通信中はボタンをローディング表示にし、失敗したらエラーメッセージを出す。
//
// onLoginSuccess: ログイン成功時に呼ぶ関数。呼び出し元(AppNavigation.kt)から渡される。
// ===============================================
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // rememberCoroutineScope(): ボタンのonClickのような「Composableではない場所」から
    // suspend関数(signInWithUsername)を呼ぶために必要なコルーチンスコープ。
    val scope = rememberCoroutineScope()

    // remember { AuthRepository() }: 画面が再描画されるたびに新しいインスタンスを作らないようにする。
    val authRepository = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "隙間カレンダー",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "友達との「いつ空いてる？」をもっと気軽に。",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                errorMessage = null // 入力し直したら前のエラーは消す
            },
            label = { Text("ユーザー名") },
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmed = userName.trim()
                if (trimmed.isEmpty()) {
                    errorMessage = "ユーザー名を入力してください"
                    return@Button
                }
                isLoading = true
                errorMessage = null

                // scope.launch: ここでコルーチンを開始し、suspend関数を呼べるようにする。
                // UIスレッドをブロックせずに通信が終わるのを待てる。
                scope.launch {
                    val result = authRepository.signInWithUsername(trimmed)
                    isLoading = false
                    result
                        .onSuccess { onLoginSuccess() }
                        .onFailure { e -> errorMessage = "ログインに失敗しました: ${e.message}" }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("ログイン / 新規登録")
            }
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
