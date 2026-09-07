******

### リリース履歴

******

# v1.0.0

###### 2026/09/07

* `ヒント` 開発プレビュー: プラグインは AutoJs6 のプラグインセンターに登録されますが, MCP エンドポイントとそのツールはまだ利用できません
* `機能` ホスト検出用の INFO サービス, Wake Activity, `org.autojs.plugin.MCP_SERVER` サービスの骨組みを備えたプラグイン ID `mcp-server`
* `機能` 10 言語の README, プラグインセンターの説明, 変更履歴
* `機能` `http://127.0.0.1:9637/mcp` の Streamable HTTP エンドポイントと `device_ping` ツール. adb またはホストから起動と停止ができるフォアグラウンドサービスが提供 (開発プレビュー)
* `依存関係` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) と Ktor 3.5.1 CIO エンジン
