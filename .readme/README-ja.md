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

_2026/09/11_

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
- `機能` グループスイッチ付きのツールカタログ (決定 D6): `device_ping` (プラグイン内), `device_info` (AutoJs6 の `device.info`), `script_run` (AutoJs6 の `engines.execScript`: JavaScript を実行し, 最大 `timeoutMs` まで終了を待って結果と最新のコンソール行を返し, 実行中は進捗通知を送ります); すべてのツールは閉じた JSON Schema (`additionalProperties: false`) を宣言し, 引数は AutoJs6 に届く前に検証されます; グループスイッチ `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` は `tool_groups.json` に保存され, オフにしたグループは次のリクエストから `tools/list` に現れず, そのツールは `TOOL_DISABLED` を返します
- `機能` ホストブリッジ: `org.autojs.plugin.MCP_SERVER` サービスが本物の `IMcpServerPlugin` Binder を実装します (`getInfo` / `getCapabilities` はコントラクトバージョン 1, ツールグループ, MCP プロトコルバージョン, SDK バージョンを報告し, `openServer` はインストール済みで同じ署名の AutoJs6 のみを受け入れ, `getStatus` / `updateConfig` / `stop` / `close` を持つ `IMcpServerSession` を返します); ツール呼び出しは単調増加のリクエスト id, 呼び出しごとのタイムアウト, 4 件の同時実行上限を伴ってホストの能力ブローカーを経由し, ホストのエラー分類は `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR` に対応付けられます; AutoJs6 が終了してもリスナーは動き続け, ホスト依存のツールはホストが再接続するまで `HOST_UNAVAILABLE` を返します; 状態とイベント (`pairing_requested`, `client_paired`, `tool_call`, `warning`) はコールバック経由でホストに届きます
- `機能` フォアグラウンドサービスの通知にエンドポイント, AutoJs6 の接続状態, ペアリング済みクライアント数と停止アクションを表示します; 通知がブロックされている場合はトーストでエンドポイントを知らせます; `dumpsys activity service` はホストセッション, ツールグループスイッチ, 登録済みツールも出力します
- `機能` スクリプトグループを完成: `script_run_file` は端末上のスクリプトファイルを実行し, `script_stop` / `script_stop_all` は AutoJs6 の実行を 1 つまたはすべて停止し, `script_list` は実行中のものを列挙し, `console_tail` は `nextSinceId` カーソルとレベルフィルター付きで最新のコンソール行を返します; `script_run` と `script_run_file` は `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 行番号付きの例外, 最新のコンソール行を返すようになり, 待機中は 2 秒ごとに最新のコンソール行を含む進捗通知を送ります
- `機能` MCP エンドポイントの応答は Server-Sent Events (SSE) でストリーム配信されるようになりました (SDK の JSON 応答モードは使いません). 実行中スクリプトの進捗ハートビートなど, リクエストに属する通知はそのリクエスト自身の応答でクライアントに届きます
- `機能` UI グループを追加 (roadmap P3.2): `ui_dump` は現在のウィンドウを `#n` 参照付きのコンパクトなノードツリーで返し (`format` は text / json / xml, `maxNodes` は最大 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` はセレクターをポーリングし, `ui_current_window` と `ui_explain_selector` はウィンドウとセレクターが失敗する理由を報告し, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` は `nodeRef` (フィンガープリントで再特定し, 消えていれば `NODE_REF_STALE`) または `selector` に作用し, `ui_press_key` は back / home / recents / notifications / quick_settings / power_dialog / lock_screen を押し, 既定で無効の `ui_gesture` グループは `ui_swipe`, `ui_gesture` とクリックツールの座標形式を追加します (グループが無効の間は `TOOL_DISABLED`); ツールカタログのスナップショットは 20 ツールに増えました; 座標ジェスチャーには 2026-09-11 以降にビルドされた AutoJs6 ホストが必要です (それより古いホストはランダムに "the system cancelled ..." と応答します)
- `依存関係` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) と Ktor 3.5.1 CIO エンジン
- `依存関係` JVM トランスポートテスト用に Ktor 3.5.1 `ktor-server-test-host` を追加 (テストスコープのみ)
- `依存関係` `mcp-server-api.aar` (AutoJs6 のモジュール `plugin-api/mcp-server-api`, ホストビルド 6.8.0 / 5279, MPL 2.0) を AutoJs6 とプラグイン間の Binder コントラクトとして追加し, `locks/host-api-aars.lock` でハッシュを固定

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
