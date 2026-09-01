// ===============================================
// app/build.gradle.kts
// 役割: このアプリ(app)モジュールで実際に使うライブラリ一覧(dependencies)と、
//       ビルド設定(SDKバージョン、Composeを使うかどうか等)を定義する場所。
//       「AIが書いたコードをコピペしてそのまま動かす」ためには、
//       このファイルに書かれているライブラリが揃っている必要がある。
// ===============================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Firebaseの設定ファイル(google-services.json)を読み込むためのプラグイン。
    // google-services.json を app/ 直下に配置したら有効化する。
    // まだ配置していない場合、このプラグインが有効だとビルドが失敗するので、
    // その場合は下の行の先頭に // を付けて再びコメントアウトすること。
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sukimacalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sukimacalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-skeleton"
    }

    buildFeatures {
        // Jetpack Composeを使うために必須の設定。
        // これをtrueにしないと @Composable が一切使えない。
        compose = true
    }

    composeOptions {
        // Compose Compilerのバージョンを明示的に指定する。
        // これを書かないとAGPが古いデフォルト値(例: 1.3.2)を拾うことがあり、
        // 「このKotlinバージョンとは互換性がない」というビルドエラーの原因になる。
        // 1.5.14 は Kotlin 1.9.24 (build.gradle.ktsで指定しているバージョン) に対応する組み合わせ。
        // Kotlinのバージョンを変えたら、このバージョンも合わせて変更が必要。
        // 対応表: https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ---- Jetpack Compose 本体 ----
    // BOM(Bill of Materials): Compose関連ライブラリのバージョンを一括管理してくれる仕組み。
    // 個々のライブラリにバージョン番号を書かなくてよくなる。
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3") // ボタンやカードなど基本部品
    implementation("androidx.compose.ui:ui-tooling-preview") // @Previewでプレビュー表示するため
    implementation("androidx.activity:activity-compose:1.9.3") // ComposeをActivityで使うため

    // ---- 画面遷移(認証画面→カレンダー画面、のような画面切り替え) ----
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ---- Material Icons(下部ナビのアイコンなどに使用) ----
    implementation("androidx.compose.material:material-icons-extended")

    // ---- Firebase (Authでログイン、Firestoreでデータ保存に使用予定) ----
    // 現時点(骨組み段階)ではまだ画面から呼び出していないが、
    // 後でグループ作成・空き枠登録などを実装するときにすぐ使えるよう先に入れておく。
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // ---- 基本ライブラリ ----
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    // collectAsStateWithLifecycle()を使うために必要(GroupScreen.ktでFirestoreのFlowを監視する際に使用)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // ---- Firebaseの非同期処理(Task)をKotlinのsuspend関数(.await())として使うためのライブラリ ----
    // Repositoryクラス(AuthRepository, GroupRepository)内の .await() 呼び出しに必要。
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ---- デバッグ用(Composeのプレビュー・レイアウト検証) ----
    debugImplementation("androidx.compose.ui:ui-tooling")
}
