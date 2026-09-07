MCP Server は, AutoJs6 を実行している Android デバイスを [Model Context Protocol](https://modelcontextprotocol.io) サーバーにします. Claude Code, Cursor, MCP Inspector などの PC 上の AI エージェントは USB または Wi-Fi でスマートフォンに接続し, ツールを使ってスクリプトの実行, ログの読み取り, アクセシビリティノードツリーの確認, タップと入力, スクリーンショットの取得, ファイルとアプリの操作を行います.

プロジェクトは骨組みの段階です. このリリースは AutoJs6 のプラグインセンターにプラグインを登録し, ビルド, ドキュメント, テストの基盤を整えます. MCP エンドポイントとそのツールはまだ利用できません. 進捗は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) で項目ごとに追跡しています.

### 使い方

1. AutoJs6 ビルド 5279 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. AutoJs6 のドロワーまたはプラグインの設定ページから MCP サーバーをオンにします. スマートフォンにエンドポイントのアドレスとペアリングトークンが表示されます.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) を参照してください.
