package com.example.sukimacalendar.navigation

// ===============================================
// Screen.kt
// 役割: 「画面遷移のときに使う文字列(ルート名)」を
//      あちこちのファイルに直書きすると、
//      タイプミス(例: "setting"と"settings"の書き間違い)でバグを生みやすい。
//      なのでここに一箇所だけ定義して、他のファイルは必ずこれを参照する。
//
//      スライド6の画面遷移図に対応:
//      Auth → (ログイン後) Group / Calendar / Settings は下部ナビで行き来
//      Calendar → Notification, DateDetail(これは別画面ではなくボトムシート) へ分岐
// ===============================================
sealed class Screen(val route: String) {
    object Auth : Screen("auth")            // 認証・スタート画面
    object Group : Screen("group")          // グループ選択・管理画面
    object Calendar : Screen("calendar")    // カレンダーメイン画面(★最重要)
    object Settings : Screen("settings")    // 設定画面
    object Notification : Screen("notification") // 通知画面
}
