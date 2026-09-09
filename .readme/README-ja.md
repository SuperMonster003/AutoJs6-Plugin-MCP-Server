<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Model Context Protocol を通じて AI エージェントにデバイス自動化機能を公開</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語

******

現在の README.md は以下の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### はじめに

******

MCP Server は, AutoJs6 を実行している Android デバイスを [Model Context Protocol](https://modelcontextprotocol.io) サーバーにします. Claude Code, Cursor, MCP Inspector などの PC 上の AI エージェントは USB または Wi-Fi でスマートフォンに接続し, ツールを使ってスクリプトの実行, ログの読み取り, アクセシビリティノードツリーの確認, タップと入力, スクリーンショットの取得, ファイルとアプリの操作を行います.

サーバーはプラグイン自身のプロセス内で動作し, 単一の Streamable HTTP エンドポイントから利用できます. AutoJs6 は Binder 経由でプラグインに機能ブローカーを渡すため, すべてのツール呼び出しはホストが既存の権限, エンジン, アクセシビリティサービスを使って実行します. プラグインがホストの機能を複製することはありません.

******

### 現在の状態

******

プロジェクトは骨組みの段階です. このリリースは AutoJs6 のプラグインセンターにプラグインを登録し, ビルド, ドキュメント, テストの基盤を整えます. MCP エンドポイントとそのツールはまだ利用できません. 進捗は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) で項目ごとに追跡しています.

******

### 予定している機能

******

ロードマップでは以下の機能を段階的に提供します:

- スクリプト実行: AutoJs6 内でテキストまたはファイルから JavaScript を実行し, エンジンの一覧表示と停止, 最近のコンソール出力の読み取りを行います.
- アクセシビリティ UI: ノードツリーをコンパクトなテキスト形式で出力し, AutoJs6 のセレクター構文でノードを検索し, クリック, 長押し, スクロール, テキスト設定, 戻るやホームなどのグローバルキー操作を行います.
- スクリーンショット: マルチモーダルモデルに適したサイズ上限で画面を PNG または JPEG として取得します.
- ファイル, アプリ, デバイス: AutoJs6 の作業ディレクトリ内のファイルの読み書き, アプリの起動, 前面ウィンドウの照会, デバイス情報の報告を行います.
- 接続経路: `adb forward` による USB 接続, 明示的に有効化するローカルネットワーク接続, PC 側の stdio ブリッジ, および OAuth 2.1 を備えたオプションの公開トンネル.
- セキュリティ: ローテーション可能な Bearer トークン, スマートフォン上での初回ペアリング確認, グループ単位のツールスイッチ. サーバーはデフォルトでループバックインターフェースのみを待ち受けます.

******

### 使い方

******

1. AutoJs6 ビルド 5279 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. AutoJs6 のドロワーまたはプラグインの設定ページから MCP サーバーをオンにします. スマートフォンにエンドポイントのアドレスとペアリングトークンが表示されます.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.

> 手順 3 と 4 は計画中のワークフローで, 対応するロードマップの段階が完了すると利用可能になります. プラグインは Android 7.0 (API 24) 以降に対応します.

******

### クライアント設定

******

Claude Code はコマンド 1 つでサーバーを登録できます. 他のクライアントは MCP 設定で同じ URL とヘッダーを使用します:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

トークンはスマートフォンに表示された値に置き換えてください. このコマンドはサーバーを起動できるようになってから有効です (`現在の状態` を参照).

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder のエントリポイントは署名権限 `org.autojs.permission.PLUGIN` で保護されており, AutoJs6 だけがバインドできます.
- INTERNET 権限はプラグイン自身の HTTP リスナーにのみ使用されます. プラグインは外向きのリクエストを行わず, データを収集しません.
- ツール呼び出しは AutoJs6 の機能ブローカーを経由し, ホスト自身に許可された範囲を決して超えません. シェルコマンドやファイル削除などの危険なグループは, ユーザーが有効にするまでオフのままです.
- バックアップは無効化され, トークンはプラグインのプライベートストレージにのみ保存されます.

プラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストおよびプラグインの開発者向けです. ホストはこれらの識別子を使ってプラグインを検出し, 互換性を交渉します:

```text
application id: io.github.supermonster003.autojs6.plugin.mcp.server
plugin id: mcp-server
engine: mcp-server
variant: default
service action: org.autojs.plugin.MCP_SERVER
service category: mcp-server
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mcp.server.api.IMcpServerPlugin
minimum host build: 5279 (6.8.0)
default endpoint: http://127.0.0.1:9637/mcp
```

`McpServerPluginService` は `org.autojs.plugin.MCP_SERVER` アクション (カテゴリ `mcp-server`) に応答し, `:mcp_server` プロセスで動作します. AIDL コントラクト `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` はホストの `mcp-server-api` モジュールで定義され, ロードマップの P1 段階で導入されます. それまでサービスはコントラクトの記述子のみを公開します. `McpServerPluginInfoService` は標準の `PluginInfo` で `org.autojs.plugin.INFO` に応答し, `WakeActivity` は新規インストールしたアプリを停止状態に保つデバイスでホストがプラグインプロセスを起動できるようにします.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/09/10_

- `ヒント` 開発プレビュー: プラグインは AutoJs6 のプラグインセンターに登録されますが, MCP エンドポイントとそのツールはまだ利用できません
- `機能` ホスト検出用の INFO サービス, Wake Activity, `org.autojs.plugin.MCP_SERVER` サービスの骨組みを備えたプラグイン ID `mcp-server`
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
- `機能` `http://127.0.0.1:9637/mcp` の Streamable HTTP エンドポイントと `device_ping` ツール. adb またはホストから起動と停止ができるフォアグラウンドサービスが提供 (開発プレビュー)
- `機能` `/mcp` エンドポイントのトランスポート強化: バインドアドレスとポートはサーバー設定ストアから取得し, リクエスト本文は 1 MiB まで, アイドル接続は 60 秒で閉じ, ポートが使用中またはバインドが拒否された場合はクラッシュせず `port_in_use` / `bind_failed` 状態とヒントで終了します
- `機能` SDK トランスポート前段の DNS rebinding 保護: ループバックモードでは `Host` として `localhost` / `127.0.0.1` / `[::1]` のみを受け付け, LAN モードではデバイスの現在の IPv4 アドレスと任意の追加ホスト名を加えてネットワーク変化時に更新します; ブラウザのオリジンは拒否され, "開発者モード" スイッチのみが Inspector のループバックオリジンを CORS で許可します
- `機能` サーバー識別子 `autojs6-mcp-server` にプラグインのバージョンを添え, tools (`listChanged`), resources, prompts の機能を宣言します; `tools/list` は登録順を保ち, クライアントがキャッシュできるようにします
- `機能` すべての `/mcp` リクエストに対する Bearer トークン認証: 初回起動時に 32 バイトのトークンを生成し, Android Keystore の AES-GCM 鍵で包んでプラグインのプライベートかつバックアップ対象外のストレージに保存します; `Authorization` ヘッダーが欠落または不正な場合は定数時間比較の後に `401` + `WWW-Authenticate: Bearer` と JSON-RPC `-32001` エラーで拒否します; トークンはログに書き込まれません
- `機能` トランスポート前段の初回ペアリング: 未ペアリングのクライアントは `initialize` と tools, resources, prompts の一覧取得はできますが, 最初の `tools/call`, `resources/read`, `resources/subscribe`, `prompts/get` は 60 秒以内にスマートフォンで確認されるまで `PAIRING_REQUIRED` (`-32002`) を返します; 拒否またはタイムアウト後は 30 秒間 `PAIRING_DENIED` (`-32003`) を返します; クライアントは `clientInfo` の名前 (欠落時は `User-Agent`) とアドレス種別 (ループバック / LAN) で識別されるため, トークンのローテーションで既存のペアリングは失われず, 最大 32 クライアントをペアリングできます
- `機能` スマートフォンでのペアリング確認は 2 つの経路で行います: 許可 / 拒否アクション付きの高優先度通知と, 画面ロック解除中のダイアログです; サーバー設定, トークン, ペアリング済みクライアントはアトミックに置き換えられるファイルに保存され, サーバープロセスと設定ページが古いキャッシュなしで共有します
- `依存関係` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) と Ktor 3.5.1 CIO エンジン
- `依存関係` JVM トランスポートテスト用に Ktor 3.5.1 `ktor-server-test-host` を追加 (テストスコープのみ)

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

このセクションはソースからプラグインをビルドしたい開発者向けです. 通常のユーザーは Releases ページのビルド済み APK をインストールするだけで済みます.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM ユニットテストを実行し, インストルメンテーションテスト APK をビルドする:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする:

```powershell
.\gradlew.bat :app:assembleRelease
```

リリース成果物を収集し, ファイル名にバージョンと CRC32 ダイジェストを追加する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントのソースと生成物が同期していることを検証する (CI でも実施):

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降と Android SDK 36 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

******

### ローカライズとドキュメント生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` と `.changelog/` の言語 JSON ファイルが README, プラグインセンターの説明, 変更履歴の唯一のソースです. 常にこれらの JSON ソースを編集して `py .python/generate_markdown.py` を再実行してください. 生成された README, `plugin_instruction.md`, 変更履歴は手で編集しません. `py .python/generate_markdown.py --check` を実行するとすべての生成物を検証できます.

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE) の下で提供されます. サードパーティのコンポーネントとそのライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md) に記載しています.

******

### リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- Model Context Protocol 仕様: https://modelcontextprotocol.io
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md
