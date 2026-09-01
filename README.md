# 隙間(すきま)カレンダー — UI骨組み(Jetpack Compose)

## これは何か

PPTXの企画書に沿って作った、7画面ぶんの **ナビゲーション付きUI骨組み** です。
実際のFirebase通信(ログイン・データ保存)はまだ繋がっていません。
ボタンを押すと画面は正しく切り替わりますが、データは全部ダミー(`data/model/Models.kt`)です。

## Android Studioでの開き方

1. Android Studio Otter 2 (2025.2.2 Patch 1) で `File > Open` → このフォルダ(`app`が入っている一番上の階層)を選択
2. 初回はGradleの同期(Sync)が自動で走ります。数分かかることがあります
3. 同期が終わったら、上部の実行ボタン(▶)でエミュレータ or 実機に流し込めます

## 今すぐ動かせる範囲

- 起動 → 認証画面(ユーザー名入力欄 + ログインボタン)
- ログインボタンを押す → カレンダーメイン画面へ
- カレンダーの日付をタップ → 下からボトムシートがせり出す(朝昼夜の選択、メンバー一覧、誘うボタン)
- 下部ナビの「グループ」「カレンダー」「設定」タブを行き来できる
- カレンダー画面右上のベルアイコン → 通知画面へ

## ファイル構成(どこに何があるか)

```
app/src/main/java/com/example/sukimacalendar/
├─ MainActivity.kt              … アプリの入り口(2行の薄いラッパー)
├─ navigation/
│   ├─ Screen.kt                … 画面名(ルート)の一覧
│   └─ AppNavigation.kt         … 画面遷移の司令塔(NavHost)
├─ ui/
│   ├─ theme/                   … 色・文字サイズなど見た目のルール
│   ├─ components/BottomNavBar.kt … 下部ナビ(3タブ)の共通部品
│   └─ screens/
│       ├─ auth/AuthScreen.kt              … 認証・スタート画面
│       ├─ group/GroupScreen.kt            … グループ選択・管理画面
│       ├─ calendar/CalendarMainScreen.kt  … ★カレンダーメイン画面
│       ├─ calendar/DateDetailBottomSheet.kt … ★日付詳細ボトムシート
│       ├─ settings/SettingsScreen.kt      … 設定画面
│       └─ notification/NotificationScreen.kt … 通知画面
└─ data/model/Models.kt         … 今はダミーデータ。後でFirestoreのデータ構造に合わせる
```

各ファイルの先頭に「役割」「今の実装状態」をコメントで書いてあります。
コード中の `// TODO:` が、次にFirebase連携などを実装するときに手を入れる場所の目印です。

## 次にやること(サブ実装〜本実装への流れの目安)

1. ✅ Firebaseプロジェクトを作成し、`google-services.json` を `app/` 直下に配置(コンソール側の作業。手順は会話ログ参照)
2. ✅ `AuthScreen.kt` を Firebase Anonymous Authentication に接続済み(匿名ログイン + `users/{uid}` に表示名を保存)
3. ✅ `GroupScreen.kt` を Firestore に接続済み(自分が入っているグループをリアルタイム表示 + 作成ダイアログ)
4. 次: `CalendarMainScreen.kt` / `DateDetailBottomSheet.kt` をFirestoreに接続
   - 空き枠登録: `groups/{groupId}/days/{yyyy-MM-dd}` に `{ uid: [slots] }` のようなマップを保存する設計を想定
   - メンバーの空き状況一覧をこのドキュメントから読み込む
5. 次: 「誘う」ボタンを押したときの招待(invitations)機能
6. `CalendarMainScreen.kt` の月間カレンダーを、実際の月・曜日計算(`java.time.LocalDate`など)に対応させる

## Firestoreのセキュリティルール

`firestore.rules` に最低限のルール(ログイン必須・自分のグループのみ読み書き可)を用意しています。
Firebaseコンソール → Firestore Database → ルール タブに貼り付けて公開してください。
「テストモードのまま」だと誰でも全データを読み書きできてしまうので、必ず設定してください。

## 注意点

- `minSdk = 26` に設定しています(Android 8.0以降)
- Compose BOM / Firebase BOM のバージョンは2025年時点の安定版を指定しています。
  Android Studio側で「新しいバージョンがあります」と出た場合はアップデートして問題ありません
- ログイン方式は「匿名認証(Anonymous Auth)+ ユーザー名だけをFirestoreに保存」という方式です。
  アプリを削除して再インストールすると別の匿名アカウント扱いになり、以前のデータには戻れません。
  本番運用する場合は、匿名アカウントをGoogleログイン等に「アップグレード」する仕組み
  (`linkWithCredential`)を後で追加することを推奨します。
