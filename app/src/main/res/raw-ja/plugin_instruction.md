MCP Server は, AutoJs6 を実行している Android デバイスを [Model Context Protocol](https://modelcontextprotocol.io) サーバーにします. Claude Code, Cursor, MCP Inspector などの PC 上の AI エージェントは USB または Wi-Fi でスマートフォンに接続し, ツールを使ってスクリプトの実行, ログの読み取り, アクセシビリティノードツリーの確認, タップと入力, スクリーンショットの取得, ファイルとアプリの操作を行います.

P3.4 開発プレビュー: 37 個のツールを提供し, 初期状態で 33 個が有効です. ファイル操作, エディターの位置指定, アプリ検索, クリップボード, アクセシビリティの自動有効化, 出力制限付き Shell が利用できます. ドロワーのスイッチと設定画面は P4 で実装予定です. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使い方

1. AutoJs6 ビルド 5279 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. この開発プレビューでは開発者向け文書の adb 制御とホストのテストセッションを使って接続します. ドロワーのスイッチとプラグインの設定画面は P4 で実装予定です.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) を参照してください.
