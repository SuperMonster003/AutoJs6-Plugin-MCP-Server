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

P4 開発プレビュー: 37 ツール中 33 が既定で有効です. AutoJs6 のドロワースイッチとプラグイン設定画面を提供します. 対応する P4 AutoJs6 ビルドが必要です. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### 予定している機能

******

ロードマップでは以下の機能を段階的に提供します:

- サーバー状態, USB 転送, ポートと LAN, トークンの表示/コピー/更新, ペアリング解除, ツールグループと root, 開発者モード, Claude Code / Cursor / Codex / 汎用 HTTP 設定のコピー, リリース履歴を提供し AutoJs6 の外観に追従します. ネットワーク変更はリスナーを再起動し, トークンと権限は即時反映します. 秘密情報の画面は撮影を防止します.
- スクリプト実行: AutoJs6 内でテキストまたはファイルから JavaScript を実行し, エンジンの一覧表示と停止, 最近のコンソール出力の読み取りを行います.
- アクセシビリティ UI: ノードツリーをコンパクトなテキスト形式で出力し, AutoJs6 のセレクター構文でノードを検索し, クリック, 長押し, スクロール, テキスト設定, 戻るやホームなどのグローバルキー操作を行います.
- スクリーンショットグループ (P3.3): screen_capture は切り抜き, scale または maxWidth, JPEG / PNG / WebP, 品質指定に対応した MCP 画像を返します. 既定値は JPEG 品質 70, 長辺 1280 px です. base64 が 4 MiB を超える場合は品質やサイズを下げて再試行し, 変更をメタデータに記録します. screen_state は画面の点灯状態, サイズ, 向き, 密度を返します. ツール数は 37 になりました. MediaProjection のフォールバックには 2026-09-13 以降にビルドされた AutoJs6 と端末での許可が必要で, 許可はホストセッションで再利用されます.
- 作業ディレクトリのツール (P3.4): files_list / stat / read / write / mkdir / rename / delete, 1 から始まる行と列を指定する editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, shell_exec. バイナリ読み取りは base64 で元データ最大 1 MiB. 書き込みはホストの要求上限にも従います (通常は JSON エスケープ込みで 96 KiB). 削除と Shell は初期状態で無効. root には allowShellRoot とホストの shell.root 許可が必要です. 対応する P3.4 ホストビルドが必要です.
- MCP リソース (P3.5) は読み取り専用の作業ファイル, ホストのサンプル参照, デバイス情報, 最近のコンソール出力を提供し, ペアリングとグループ設定に従います. テキストとバイナリの読み取りには切り詰め情報が含まれます. write_autojs6_script, automate_task, debug_selector は英語と中国語に対応し, その他の端末言語では英語を使用します.
- 接続経路: `adb forward` による USB 接続, 明示的に有効化するローカルネットワーク接続, PC 側の stdio ブリッジ, および OAuth 2.1 を備えたオプションの公開トンネル.
- セキュリティ: ローテーション可能な Bearer トークン, スマートフォン上での初回ペアリング確認, グループ単位のツールスイッチ. サーバーはデフォルトでループバックインターフェースのみを待ち受けます.

******

### 使い方

******

1. AutoJs6 ビルド 5279 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. AutoJs6 のドロワーで MCP サーバーを有効にします. タイトルを長押しするかプラグインセンターの設定を開き, PC クライアント用の設定をコピーしてください.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.
5. 端末で最初のペアリング要求を承認します. 終了時はドロワー, 設定, 通知から停止できます.

> MCP サーバースイッチにインストール, 有効化, 承認, 互換性の案内を追加. 通知の停止と同期し, 再接続時も設定を保持. ドロワーとプラグインセンターから権限確認済みの同じ設定画面を開きます. AutoJs6 起動時は以前の有効状態を復元し, ホスト不在中の停止を尊重. 端末起動時の自動開始は行いません.

******

### クライアント設定

******

Claude Code はコマンド 1 つでサーバーを登録できます. 他のクライアントは MCP 設定で同じ URL とヘッダーを使用します:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

AutoJs6 のドロワーで MCP サーバーを有効にします. タイトルを長押しするかプラグインセンターの設定を開き, PC クライアント用の設定をコピーしてください. 端末で最初のペアリング要求を承認します. 終了時はドロワー, 設定, 通知から停止できます.

******

### 接続経路

******

USB: `adb forward tcp:9637 tcp:9637` で端末のポートを PC に対応付けます. 複数台の場合は `-s <serial>` を付け (`adb devices` で確認), エミュレーターでも同様です. どちらかの側でポートが使用中なら, 設定ページでポートを変更して新しいポートを転送してください. 接続カードにはコピーできる転送コマンドがあります.

ローカルネットワーク: 設定ページで "ローカルネットワーク接続を許可" を有効にします. 設定ページに端末の現在のアドレス (Wi-Fi の変化に追従) と, クライアントが同じネットワークに参加する必要があることが表示されます. ゲストネットワーク, AP 分離, PC のファイアウォールがよくある障害です. ローカルネットワークからのペアリング要求は明示され, サーバーがネットワークから到達可能な間は毎日通知で知らせます. 通知はオフにできます.

どちらの経路も同じトークンと同じ端末側ペアリングを使います. HTTP トランスポートのないクライアントは, クライアント接続の節で説明する stdio ブリッジを使います.

******

### クライアント接続

******

設定ページは以下の各クライアント向けに実際のトークン入りの設定をコピーします. ここの断片では `<token>` をプレースホルダーにしています. どのクライアントも Authorization ヘッダー付きの Streamable HTTP で通信し, 新しいクライアントの最初の呼び出しは端末で確認します. 検証済み: Claude Code, Codex CLI, MCP Inspector. その他のクライアントも同じ URL とヘッダーを使いますが, メンテナーによる検証はまだです.

Claude Code: "クライアント設定" に示したコマンドを実行します (設定ページはトークン入りでコピーします). その後 `claude mcp list` で `autojs6` が Connected と表示されます.

Cursor: 以下のエントリーを `mcp.json` に追加します:

```json
{
  "mcpServers": {
    "autojs6": {
      "url": "http://127.0.0.1:9637/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

Codex CLI: トークンを環境変数 `AUTOJS6_MCP_TOKEN` に入れ (設定ページは対応する PowerShell コマンドをコピーします), サーバーを `config.toml` に追加するか, `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN` を実行します:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: CLI モードは追加設定なしで使え, Web UI は自身の Node プロキシ経由で端末に到達します. ブラウザーページがエンドポイントへ直接接続する場合だけ, 設定ページで開発者モードを有効にしてください:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI などのクライアント: それぞれの MCP 設定で同じ URL とヘッダーを使います. 設定ページには `"type": "http"` 付きの汎用 JSON 断片があります.

Claude Desktop などの stdio 専用クライアント: `npm install -g autojs6-mcp-bridge` でブリッジをインストールし, `autojs6-mcp-bridge --serial <serial>` を stdio サーバーとして登録して, その環境変数ブロックに `AUTOJS6_MCP_TOKEN` を入れます (Claude Desktop と Claude Code の断片は[ブリッジの README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) を参照). ブリッジ 0.1.0 はプラグイン 1.0.0 と組み合わせ, クライアントのプロトコルバージョンをそのまま渡します. Claude Code 2.1.257 の stdio 接続で検証済みです.

******

### よくある質問

******

- 401 Unauthorized: トークンが未設定, 誤入力, またはローテーション済みです. 設定ページから設定をコピーし直してください. トークンをローテーションした後は全クライアントに新しい値が必要です.
- ペアリングのタイムアウト: 新しいクライアントの最初の呼び出しは, 端末で許可されるまで約 1 分待ちます. 端末のロックを解除し, ダイアログまたは通知のアクションで許可してから, 呼び出しを繰り返してください. 拒否すると短いクールダウンに入り, その後の呼び出しで再度確認されます.
- HOST_UNAVAILABLE: AutoJs6 が起動していないか, プラグインセッションが閉じています. AutoJs6 を開き, ドロワーのスイッチをオンのままにし, 設定ページで接続状態を確認してください.
- アクセシビリティが無効: `ui_*` ツールと画面キャプチャには AutoJs6 のアクセシビリティサービスが必要です. `device_ensure_accessibility` を呼ぶか, システムのアクセシビリティ設定でサービスを有効にしてください.
- ポート使用中: ドロワーに `port_in_use` と表示されます. 設定ページでポートを変更し, 新しいポートを adb で転送してください.
- ローカルネットワークに到達できない: ローカルネットワークアクセスを有効にし, 設定ページに表示されたアドレスを使い, PC と端末をゲスト分離のない同じネットワークに置き, PC のファイアウォールでポートを許可してください. 端末の Wi-Fi 省電力により呼び出しごとに数百ミリ秒が加わります.
- 画面が消えるとサーバーが消える: アプリのバッテリー使用を制限する端末 (HyperOS と MIUI はサイドロードしたアプリにデフォルトでそうします) は, バッテリー駆動で画面が消えてから約 1 分後にフォアグラウンドサービスを停止します. その場合は設定ページに警告と "バッテリー設定" ボタンが表示されるので, そこで MCP Server を "制限なし" にしてください. 任意の "アイドル時に自動停止" (デフォルトはオフ) も, 選んだ分数リクエストがないとサーバーを停止し, その旨の通知を残します.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder のエントリポイントと設定ページは署名権限 `org.autojs.permission.PLUGIN` で保護されており, AutoJs6 だけが到達できます. ペアリングダイアログ, そのレシーバー, リリース履歴ページはエクスポートされていません. リスナーを持つフォアグラウンドサービスだけが adb (`android.permission.DUMP`) を受け付け, 開発者の起動 / 停止スイッチになります.
- INTERNET 権限はプラグイン自身の HTTP リスナーにのみ使用されます. プラグインは外向きのリクエストを行わず, データを収集しません. 平文 HTTP はネットワークセキュリティ構成によりループバックアドレス宛てにのみ許可されます.
- サーバーはデフォルトで 127.0.0.1 のみを待ち受けます. ローカルネットワークアクセスは有効にするまでオフのままで, 有効にしてもトークン, ペアリング確認, Host 許可リスト, レート制限はローカルネットワーク上でも適用され, オンの間は毎日通知で知らせます.
- アクセストークンは安全な乱数源から生成され, Android Keystore の AES-GCM 鍵で包んでプラグインのプライベートでバックアップされないストレージに保存されます. バックアップと端末間転送は無効です. 設定ページは末尾 4 文字だけを表示し, 完全なトークンのダイアログはスクリーンショットを遮断し, コピーはクリップボードで機密として扱われます.
- ログにはトークン, リクエスト本文, ファイル内容, スクリーンショットが決して含まれません. プラグインが記録するのはツール名, クライアント名, トークンのフィンガープリントだけです. 実際のファイルとスクリーンショットの呼び出し中に 2 台の端末で logcat により検証しました (docs/dev/p6-security-audit.md).
- ツール呼び出しは AutoJs6 の機能ブローカーを経由し, ホスト自身に許可された範囲を決して超えません. シェルコマンド, ファイル削除, ジェスチャーは各グループを有効にするまでオフのままで, root シェルにはさらに専用のスイッチとホストの許可が必要です.
- ペアリングは設定ページで 1 件ずつ, または一括で取り消せます. 取り消されたクライアントは次のツール呼び出しの前にスマートフォンで再度確認が必要です. トークンをローテーションしてもペアリングは保持されますが, 古いトークンを使い続けるクライアントは遮断されます.

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

`McpServerPluginService` は `:mcp_server` プロセスでホストの mcp-server-api 契約 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` を実装し, `org.autojs.plugin.MCP_SERVER` (category `mcp-server`) に応答します. `McpServerPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo を返します. ホストは `WakeActivity` でプラグインを有効化できます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.1

_2026/09/15_

- `改善` compileSdk を 37 (Android 17) に引き上げ, targetSdk はターゲット依存の動作を検証するまで 36 のまま
- `改善` MCP 適合性 (P6): 2 台の端末で公式の @modelcontextprotocol/conformance スイート 0.1.16 をステートフルな /mcp パスに対して実行しました. 32 のサーバーシナリオのうち 9 が合格 (initialize, ping, tools/list, テキストとエラーのツール結果, resources/list, prompts/list, 並行 SSE ストリーム, DNS rebinding 保護); 18 はスイート付属の参照フィクスチャ (test_* ツール, プロンプト, test:// リソース. このサーバーは未知ツールの結果, -32602 または isError で応答します) を呼び, 5 はこのサーバーが宣言しない機能 (logging, completions, リソース購読) を必要とします. ループバックの Origin ヘッダーはスイートの期待どおりどのモードでも受け付けるようになりました. CORS ヘッダーとプリフライト応答は引き続き開発者モード限定です. 2026-07-28 のステートレスモデルにはルートがありません (Roadmap D9). 詳細は docs/dev/p6-conformance.md.
- `改善` セキュリティ監査 (P6): チェックリスト 7 項目 (トークンの保存, ログの秘匿, エクスポートされたコンポーネント, 平文の範囲, ローカルネットワークのデフォルトオフ, ペアリングの取り消し, ツールグループのデフォルトオフ) をコードと 2 台の端末で検証し, docs/dev/p6-security-audit.md に記録しました. README のセキュリティ節はこれらの境界を説明するようになりました. 平文 HTTP はアプリ全体の usesCleartextTraffic フラグの代わりにネットワークセキュリティ構成でループバックアドレスに限定されます. プラグインはクライアント接続を開かず, リスナーにはこのフラグは不要です.

#### v1.0.0

_2026/09/15_

- `ヒント` P4 開発プレビュー: 37 ツール中 33 が既定で有効です. AutoJs6 のドロワースイッチとプラグイン設定画面を提供します. 対応する P4 AutoJs6 ビルドが必要です. ROADMAP.md.
- `機能` サーバー状態, USB 転送, ポートと LAN, トークンの表示/コピー/更新, ペアリング解除, ツールグループと root, 開発者モード, Claude Code / Cursor / Codex / 汎用 HTTP 設定のコピー, リリース履歴を提供し AutoJs6 の外観に追従します. ネットワーク変更はリスナーを再起動し, トークンと権限は即時反映します. 秘密情報の画面は撮影を防止します.
- `機能` MCP リソース (P3.5) は読み取り専用の作業ファイル, ホストのサンプル参照, デバイス情報, 最近のコンソール出力を提供し, ペアリングとグループ設定に従います. テキストとバイナリの読み取りには切り詰め情報が含まれます. write_autojs6_script, automate_task, debug_selector は英語と中国語に対応し, その他の端末言語では英語を使用します.
- `機能` 作業ディレクトリのツール (P3.4): files_list / stat / read / write / mkdir / rename / delete, 1 から始まる行と列を指定する editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, shell_exec. バイナリ読み取りは base64 で元データ最大 1 MiB. 書き込みはホストの要求上限にも従います (通常は JSON エスケープ込みで 96 KiB). 削除と Shell は初期状態で無効. root には allowShellRoot とホストの shell.root 許可が必要です. 対応する P3.4 ホストビルドが必要です.
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
- `機能` スクリーンショットグループ (P3.3): screen_capture は切り抜き, scale または maxWidth, JPEG / PNG / WebP, 品質指定に対応した MCP 画像を返します. 既定値は JPEG 品質 70, 長辺 1280 px です. base64 が 4 MiB を超える場合は品質やサイズを下げて再試行し, 変更をメタデータに記録します. screen_state は画面の点灯状態, サイズ, 向き, 密度を返します. ツール数は 22 になりました. MediaProjection のフォールバックには 2026-09-13 以降にビルドされた AutoJs6 と端末での許可が必要で, 許可はホストセッションで再利用されます.
- `機能` ローカルネットワーク経路 (P5.1): ローカルネットワークアクセスを有効にすると設定ページに端末の現在のアドレス (Wi-Fi 変更時に更新) と同一ネットワーク / ファイアウォールのヒントを表示. ローカルネットワークからのペアリング要求はダイアログと通知で明示. サーバーがまだローカルネットワークから到達可能であることを毎日通知し, リスナーを再起動せずにオフにできる. README に USB とローカルネットワークの経路を記載.
- `機能` クライアントごとのレート制限 (P6): クライアントごとに毎秒最大 20 リクエスト, 毎分最大 30 回の screen_capture. 上限を超えたリクエストは Retry-After ヘッダーと retryAfterMs を含む JSON-RPC RATE_LIMITED エラー付きの HTTP 429 を受け取り, 上限を超えたスクリーンショットは retryAfterMs 付きの RATE_LIMITED ツール結果として応答されるため, モデルは待機できます. ホストの grant によるアクセシビリティ照会レートはこれに加えて有効です.
- `機能` アイドル時自動停止とバッテリー (P6): 設定ページに "アイドル時に自動停止" (デフォルトはオフ, 5 / 15 / 30 / 60 / 120 分) を追加しました. リスナーは選んだ時間クライアントからのリクエストがないと自ら停止し, プラグイン内部の idle_timeout 理由を記録し, 自動で消える通知を 1 件残し, ユーザーによる停止とは数えません. 処理中のリクエスト (実行中のツール呼び出し) はアイドルと見なされず, ストリームを開いたままリクエストを送らないクライアントはサーバーを動かし続けません. Android がこのアプリのバッテリー使用を制限している場合 (HyperOS と MIUI はサイドロードしたアプリにデフォルトでそうし, バッテリー駆動で画面が消えてから約 1 分後にフォアグラウンドサービスを停止します), 設定ページに警告と "バッテリー設定" ボタンを表示し, AutoJs6 は warning イベントを受け取ります. アイドル時の CPU 時間と 1 時間アイドルの batterystats 推定値は docs/dev/p6-battery-and-residency.md に記録しています.
- `修正` IDE の rebuild で JVM 単体テスト用の APK を探す問題を解消. APK 検証タスクが必要な成果物を自動生成し, clean 後も直接実行可能.
- `修正` プラグインセンターのライトモードとダークモードでアイコンの比率が異なる問題; 夜間もアダプティブアイコンを使用し, レイヤーのサイズを調整して ic_launcher_round.png 全体の図形と余白を維持し, 背景色のみを切り替える
- `修正` 設定ページのキーボード Tab 移動でツールバーの戻るボタンが飛ばされる問題を解消. Tab の循環が戻るボタンと全コントロールを含むようにした (Android 7 を含む). デバイステストでスクリーンリーダー用ラベルとキーボード操作を確認.
- `修正` script_run と script_run_file の arguments マップが値の型を JSON Schema の配列で宣言しており, 一部の MCP クライアントが拒否または弱体化させる問題を修正. スキーマは単一型の anyOf 分岐を使うようにした. README にクライアント接続とよくある質問の章, 検証済みクライアントの一覧を追加.
- `修正` 悪意あるリクエスト本文はパーサーが再帰する前に拒否されます. 64 段より深くネストした JSON, リクエスト id が重複する本文, 同一セッションで処理中のリクエスト id (以前は先のリクエストが応答を失っていた) は 400 と JSON-RPC エラーを返し, 有効なセッションのない GET ストリームは空のイベントストリームではなく 400 / 404 を返します. JVM テストとデバイステスト (API 28 / 31 / 33 / 35) が超長, 超深, 不正な UTF-8, 未知のメソッド, ヘッダーの組み合わせ, 巨大な base64, 64 同時セッションを検証します.
- `修正` ライフサイクルマトリクス (P6): 以前のリスナープロセスのセッション id を持つリクエスト (リスナーが強制終了された後, またはトークンローテーションで再起動した後) はトランスポートの 404 に委ねてクライアントに再度 initialize させ, すでにペアリング済みのクライアントに対してリクエストの User-Agent 名義で余計なペアリング確認を出さなくなりました. 死んだリスナープロセスが通知シェードに残したペアリング通知は次のリスナーの起動時に消去されます. ホストの強制終了, リスナーの強制終了, 両方の同時終了, システム設定からの強制停止, 有効なセッション中のトークンローテーション, ホストとリスナーの終了をまたぐ保留中のペアリングについて, 期待される状態と復旧手順を docs/dev/lifecycle-matrix.md に記録し, 実機で検証しました.
- `改善` 意図しないネイティブ依存関係をビルド時に拒否し, JSON レポートを生成
- `依存関係` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) と Ktor 3.5.1 CIO エンジン
- `依存関係` JVM トランスポートテスト用に Ktor 3.5.1 `ktor-server-test-host` を追加 (テストスコープのみ)
- `依存関係` `mcp-server-api.aar` (AutoJs6 のモジュール `plugin-api/mcp-server-api`, ホストビルド 6.8.0 / 5279, MPL 2.0) を AutoJs6 とプラグイン間の Binder コントラクトとして追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` P4 ホストの common-plugin-api と mcp-server-api AAR を更新: 任意の設定拡張 v1, AIDL 順序維持, SHA-256 固定, SDK 36 互換性保持.

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

ビルドには JDK 21 以降と Android SDK 37 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

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


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
