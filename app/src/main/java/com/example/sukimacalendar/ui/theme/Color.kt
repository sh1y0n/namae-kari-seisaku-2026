package com.example.sukimacalendar.ui.theme

import androidx.compose.ui.graphics.Color

// ===============================================
// Color.kt
// 役割: アプリ全体で使う色を一箇所にまとめておくファイル。
//      Figma/PPTXのモックアップで使われていた
//      「グリーン(下部ナビ)」「ラベンダー(空きドット/選択日)」「赤(誘われ中)」を再現。
//      各画面のコードでは Color(0xFF...) を直書きせず、ここの名前を呼び出す。
// ===============================================

val SukimaGreen = Color(0xFF6FCF97)      // 下部ナビゲーションバーの背景色
val SukimaLavender = Color(0xFFB6B4E8)   // カレンダーで選択中の日/空きドット(自分)の色
val SukimaBlueDot = Color(0xFF5AC8FA)    // 空き状況ドット(メンバー)
val SukimaRedDot = Color(0xFFE85D5D)     // 「誘われている/埋まっている」を示すドット
val SukimaBackground = Color(0xFFFFFFFF)
val SukimaSurfaceGray = Color(0xFFE0E0E0) // カレンダーの空白マス
