// ===============================================
// build.gradle.kts (プロジェクトルート)
// 役割: 各モジュール(今回はappのみ)で共通して使うGradleプラグインの
//       バージョンをまとめて宣言する場所。
//       ここでは実際の依存関係(ライブラリ)は書かず、
//       「どのプラグインのどのバージョンを使えるようにするか」だけを宣言する。
//       実際の適用は app/build.gradle.kts 側で行う。
// ===============================================
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // Firebaseプロジェクト作成後、google-services.jsonを配置してから
    // app/build.gradle.kts側のコメントも外して有効化する(READMEの手順参照)。
    id("com.google.gms.google-services") version "4.4.2" apply false
}
