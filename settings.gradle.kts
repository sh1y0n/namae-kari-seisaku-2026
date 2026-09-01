// ===============================================
// settings.gradle.kts
// 役割: プロジェクト全体でどのモジュール(app等)を使うか、
//       どこからライブラリをダウンロードするか(リポジトリ)を定義する場所。
//       基本的に新規プロジェクト作成時にAndroid Studioが自動生成するファイルで、
//       今回は「app」モジュール1つだけを使う構成。
// ===============================================
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SukimaCalendar"
include(":app")
