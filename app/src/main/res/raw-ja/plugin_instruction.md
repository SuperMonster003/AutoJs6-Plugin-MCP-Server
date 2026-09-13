MCP Server は, AutoJs6 を実行している Android デバイスを [Model Context Protocol](https://modelcontextprotocol.io) サーバーにします. Claude Code, Cursor, MCP Inspector などの PC 上の AI エージェントは USB または Wi-Fi でスマートフォンに接続し, ツールを使ってスクリプトの実行, ログの読み取り, アクセシビリティノードツリーの確認, タップと入力, スクリーンショットの取得, ファイルとアプリの操作を行います.

P4 開発プレビュー: 37 ツール中 33 が既定で有効です. AutoJs6 のドロワースイッチとプラグイン設定画面を提供します. 対応する P4 AutoJs6 ビルドが必要です. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使い方

1. AutoJs6 ビルド 5279 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. AutoJs6 のドロワーで MCP サーバーを有効にします. タイトルを長押しするかプラグインセンターの設定を開き, PC クライアント用の設定をコピーしてください.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.
5. 端末で最初のペアリング要求を承認します. 終了時はドロワー, 設定, 通知から停止できます.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) を参照してください.
