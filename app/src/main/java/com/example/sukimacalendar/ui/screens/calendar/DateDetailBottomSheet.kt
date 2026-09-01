package com.example.sukimacalendar.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailBottomSheet(
    date: String,          // ← 型（String）を指定する
    onDismiss: () -> Unit  // ← 型（関数）を指定する
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(text = "$date の詳細", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            // ここに予定の詳細や編集画面などを配置
        }
    }
}