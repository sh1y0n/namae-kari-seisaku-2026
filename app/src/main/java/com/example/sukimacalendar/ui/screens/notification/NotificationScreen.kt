package com.example.sukimacalendar.ui.screens.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sukimacalendar.data.model.Invite
import com.example.sukimacalendar.data.repository.AvailabilityRepository
import com.example.sukimacalendar.data.repository.InviteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBack: () -> Unit) {
    val inviteRepository = remember { InviteRepository() }
    val availabilityRepository = remember { AvailabilityRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    // 自分宛てのお誘いをリアルタイムで取得
    val invites by inviteRepository.observeMyInvites()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知 (${invites.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (invites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("新しいお誘いはありません", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(invites) { invite ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "【${invite.groupName}】からのお誘い",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${invite.date} に誘われています",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (invite.message.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "メッセージ: ${invite.message}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (currentUserId == null) return@launch

                                                // 1. 誘った側(fromUserId)と誘われた側(currentUserId)の両方の空きデータを削除対象にする
                                                val targetUserIds = listOfNotNull(invite.fromUserId, currentUserId).distinct()

                                                // 2. 両者のその日時の空きデータを削除する（他の人から空きが消える）
                                                availabilityRepository.deleteAvailabilitiesForUsers(
                                                    groupId = invite.groupId,
                                                    userIds = targetUserIds,
                                                    dates = setOf(invite.date)
                                                ).onSuccess {
                                                    // 3. お誘いデータを削除して完了にする
                                                    FirebaseFirestore.getInstance().collection("invites")
                                                        .document(invite.id)
                                                        .delete()
                                                        .addOnSuccessListener {
                                                            snackbarMessage = "お誘いを承認し、予定を確定しました！"
                                                        }
                                                        .addOnFailureListener { e ->
                                                            snackbarMessage = "お誘いの削除に失敗しました: ${e.localizedMessage}"
                                                        }
                                                }.onFailure { e ->
                                                    snackbarMessage = "予定の確定処理に失敗しました: ${e.localizedMessage}"
                                                }
                                            }
                                        }
                                    ) {
                                        Text("承認")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            // 拒否（お誘いデータを削除するだけ）
                                            scope.launch {
                                                FirebaseFirestore.getInstance().collection("invites")
                                                    .document(invite.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        snackbarMessage = "お誘いを断りました"
                                                    }
                                                    .addOnFailureListener { e ->
                                                        snackbarMessage = "エラーが発生しました: ${e.localizedMessage}"
                                                    }
                                            }
                                        }
                                    ) {
                                        Text("拒否")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}