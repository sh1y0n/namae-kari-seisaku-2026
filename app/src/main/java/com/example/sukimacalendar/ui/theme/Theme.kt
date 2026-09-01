package com.example.sukimacalendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


// ===============================================
// Theme.kt
// 役割: Color.ktとType.ktをまとめて、MaterialThemeという
//      「アプリ全体の見た目のルール」として1つにパッケージする。
//      MainActivityで SukimaCalendarTheme { ... } のように呼び出すことで、
//      その中の全画面に色・文字設定が自動的に適用される。
// ===============================================

private val LightColors = lightColorScheme(
    primary = SukimaLavender,
    secondary = SukimaGreen,
    background = SukimaBackground,
    surface = SukimaSurfaceGray,
    error = SukimaRedDot
)

@Composable
fun SukimaCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}