package com.example.sukimacalendar.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.sukimacalendar.ui.theme.SukimaRedDot
import com.example.sukimacalendar.ui.theme.SukimaSurfaceGray
import java.time.YearMonth

// ===============================================
// CalendarMainScreen.kt (一番重要な画面)
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMainScreen(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenNotification: () -> Unit
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    var selectedDateStr by remember { mutableStateOf<String?>(null) }

    // 240ヶ月分（前後10年）のページャーを用意。中央（120ページ目）を現在月に設定
    val pagerState = rememberPagerState(initialPage = 120) { 240 }

    // 現在表示中のYearMonthを計算
    val currentYearMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - 120).toLong())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentYearMonth.year}年 ${currentYearMonth.monthValue}月") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ---- グループ切り替えタブ ----
            GroupTabRow(
                groupNames = sampleGroups.map { it.name },
                selectedIndex = selectedGroupIndex,
                onSelect = { selectedGroupIndex = it }
            )

            // ---- 横スワイプで月を変更できるHorizontalPager ----
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val targetYearMonth = YearMonth.now().plusMonths((page - 120).toLong())
                MonthGrid(
                    yearMonth = targetYearMonth,
                    onDayClick = { day ->
                        selectedDateStr = "${targetYearMonth.year}年${targetYearMonth.monthValue}月${day}日"
                    }
                )
            }
        }
    }

    // ---- 日付詳細ボトムシート ----
    selectedDateStr?.let { dateStr ->
        CalendarDateDetailBottomSheet(
            date = dateStr,
            onDismiss = { selectedDateStr = null }
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
// 月間カレンダーのグリッド本体（Column & Rowベースで縦いっぱいに均等配置）
// ===============================================
@Composable
private fun MonthGrid(yearMonth: YearMonth, onDayClick: (Int) -> Unit) {
    val weekLabels = listOf("日", "月", "火", "水", "木", "金", "土")

    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
    val startDayOffset = firstDayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    // セルリストを作成（null = 空白セル）
    val calendarCells = buildList {
        repeat(startDayOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(day)
        }
        // 7の倍数になるように末尾をnullで埋めてきれいなグリッドにする
        while (size % 7 != 0) {
            add(null)
        }
    }

    // 週間（7日ごと）の行に分割
    val weeks = calendarCells.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 曜日ヘッダー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // 週ごとの行を weight(1f) で縦方向いっぱいに均等配置
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { cellDay ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                        ) {
                            if (cellDay != null) {
                                DayCell(
                                    day = cellDay,
                                    onClick = { onDayClick(cellDay) }
                                )
                            }
                        }
                    }
                }
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
            .fillMaxSize()
            .background(SukimaSurfaceGray)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(2.dp))

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
            .size(6.dp)
            .background(color, shape = CircleShape)
    )
}

// ===============================================
// 日付詳細ボトムシート
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDateDetailBottomSheet(
    date: String,
    onDismiss: () -> Unit
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
            // ここに予定の詳細や空き時間登録画面などを配置
        }
    }
}