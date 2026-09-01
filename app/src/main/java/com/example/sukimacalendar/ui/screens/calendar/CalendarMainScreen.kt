package com.example.sukimacalendar.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sukimacalendar.data.model.sampleGroups
import com.example.sukimacalendar.ui.components.BottomNavBar
import com.example.sukimacalendar.ui.theme.SukimaBlueDot
import com.example.sukimacalendar.ui.theme.SukimaLavender
import com.example.sukimacalendar.ui.theme.SukimaRedDot
import com.example.sukimacalendar.ui.theme.SukimaSurfaceGray

// ===============================================
// CalendarMainScreen.kt(★一番重要な画面)
// 役割: PPTXスライド8の左側モックアップに対応。
//   上部: グループ切り替えタブ ("test group" / "test group2" / ＋)
//   中部: 月間カレンダー。日付ごとに空きドットを表示
//   下部: ナビゲーションバー(グループ・カレンダー・設定)
//
//   日付をタップすると、下からボトムシート(DateDetailBottomSheet.kt)がせり出す
//   ——という流れをこの画面が管理している。
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMainScreen(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenNotification: () -> Unit
) {
    // 今どのグループのタブを選んでいるか(index 0 = test group)
    var selectedGroupIndex by remember { mutableIntStateOf(0) }

    // ボトムシートを開くために「今タップされている日付」を保持。
    // null = 何も選ばれていない(ボトムシートは閉じている)
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("8月") },
                actions = {
                    IconButton(onClick = onOpenNotification) {
                        Icon(Icons.Filled.Notifications, contentDescription = "通知")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            // ---- グループ切り替えタブ ----
            GroupTabRow(
                groupNames = sampleGroups.map { it.name },
                selectedIndex = selectedGroupIndex,
                onSelect = { selectedGroupIndex = it }
            )

            // ---- 月間カレンダー本体 ----
            MonthGrid(
                onDayClick = { day -> selectedDay = day } // 日付タップでボトムシートを開く
            )
        }
    }

    // ---- 日付詳細ボトムシート ----
    // selectedDayがnullでない(=日付がタップされた)ときだけ表示する。
    selectedDay?.let { day ->
        DateDetailBottomSheet(
            date = "${day}日", // IntをStringに変換して渡す
            onDismiss = { selectedDay = null } // 閉じる時はnullに戻す
        )
    }
}

// ===============================================
// グループ切り替えタブ部分
// ===============================================
@Composable
private fun GroupTabRow(groupNames: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selectedIndex) {
        groupNames.forEachIndexed { index, name ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                text = { Text(name) }
            )
        }
        Tab(selected = false, onClick = { /* TODO: グループ作成画面へ */ }, text = { Text("＋") })
    }
}

// ===============================================
// 月間カレンダーのグリッド本体
// ===============================================
@Composable
private fun MonthGrid(onDayClick: (Int) -> Unit) {
    val weekLabels = listOf("日", "月", "火", "水", "木", "金", "土")

    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items((1..31).toList()) { day ->
                DayCell(day = day, onClick = { onDayClick(day) })
            }
        }
    }
}

// ===============================================
// カレンダー1マス分
// ===============================================
@Composable
private fun DayCell(day: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(SukimaSurfaceGray)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyMedium)

            if (day == 1) {
                Row {
                    Dot(color = SukimaBlueDot)
                    Dot(color = SukimaRedDot)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .padding(1.dp)
            .size(8.dp)
            .background(color, shape = CircleShape)
    )
}