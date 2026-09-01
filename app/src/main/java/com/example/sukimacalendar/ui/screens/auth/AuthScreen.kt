package com.example.sukimacalendar.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sukimacalendar.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSignUpMode) "アカウント作成" else "スキマカレンダー ログイン",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("ユーザーID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (userId.isBlank() || password.isBlank()) {
                        errorMessage = "IDとパスワードを入力してください"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        val result = if (isSignUpMode) {
                            authRepository.signUp(userId, password)
                        } else {
                            authRepository.login(userId, password)
                        }

                        isLoading = false
                        if (result.isSuccess) {
                            onAuthSuccess()
                        } else {
                            errorMessage = "エラー: ${result.exceptionOrNull()?.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(text = if (isSignUpMode) "登録してはじめる" else "ログイン")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                isSignUpMode = !isSignUpMode
                errorMessage = null
            }) {
                Text(
                    text = if (isSignUpMode) "すでにアカウントをお持ちですか？ ログイン"
                    else "アカウントをお持ちでないですか？ 新規登録"
                )
            }
        }
    }
}