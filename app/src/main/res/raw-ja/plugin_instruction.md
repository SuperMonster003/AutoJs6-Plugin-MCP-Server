MCP Server は, AutoJs6 を実行している Android デバイスを [Model Context Protocol](https://modelcontextprotocol.io) サーバーにします. Claude Code, Cursor, MCP Inspector などの PC 上の AI エージェントは USB または Wi-Fi でスマートフォンに接続し, ツールを使ってスクリプトの実行, ログの読み取り, アクセシビリティノードツリーの確認, タップと入力, スクリーンショットの取得, ファイルとアプリの操作を行います. 対応する AutoJs6 プラグインをインストールすると, 画面や画像の文字とバーコードの認識, 画像と色の検索, ピンインへの変換, メディア情報の読み取りも行えます. プラグインは自らツールを記述することもでき (簡体字と繁体字の変換など), MCP Server がプラグインごとのグループで提供します.

バージョン 1.3.0: 組み込みツール 39 個 (既定で 35 個が有効) とプラグイン機能ツール 8 個, MCP リソースとプロンプト, AutoJs6 のドロワースイッチとプラグイン設定画面. AutoJs6 6.8.0 (ビルド 5298) 以降が必要です. プラグイン機能ツールは対応する AutoJs6 プラグインがインストールされ有効なときだけ表示され, プラグイン機能ツールと登録済みスクリプトのツールはそれらに対応しないホストでは自動的に隠れます. オプションの autojs6://docs/ リソースには AutoJs6 オフラインドキュメントプラグインと中継メソッドを持つホストも必要です. AutoJs6 プラグインが自ら記述するツール (3-Hanzi CC の opencc_convert など) は, AutoJs6 6.8.0 以降でプラグインごとのグループとして追加で一覧に出ます. 進捗と証跡は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) に記録しています.

### 使い方

1. AutoJs6 ビルド 5298 (6.8.0) 以降を搭載したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `MCP Server` が認識されていることを確認して有効にします. 公式リリースパッケージは署名検証を自動的に通過します.
3. AutoJs6 のドロワーで MCP サーバーを有効にします. タイトルを長押しするかプラグインセンターの設定を開き, PC クライアント用の設定をコピーしてください.
4. PC で `adb forward tcp:9637 tcp:9637` を実行し, MCP クライアントを `http://127.0.0.1:9637/mcp` に向け, トークンを Bearer 資格情報として指定します.
5. 端末で最初のペアリング要求を承認します. 終了時はドロワー, 設定, 通知から停止できます.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) を参照してください.
