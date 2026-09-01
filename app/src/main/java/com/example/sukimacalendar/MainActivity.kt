package com.example.sukimacalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.sukimacalendar.navigation.AppNavigation
import com.example.sukimacalendar.ui.theme.SukimaCalendarTheme

// ===============================================
// MainActivity.kt
// 役割: アプリ起動時に最初に呼ばれる「入れ物」。
//      Composeで作る場合、Activityはこの1つだけでよく、
//      中身の画面切り替えは全部 AppNavigation() (navigation/AppNavigation.kt)に任せる。
//
//      つまりこのファイルがやっているのは実質2行だけ:
//        1. テーマを適用する(SukimaCalendarTheme)
//        2. ナビゲーションを起動する(AppNavigation)
// ===============================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SukimaCalendarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
