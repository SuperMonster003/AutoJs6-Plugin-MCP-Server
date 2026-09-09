******

### 릴리스 기록

******

# v1.0.0

###### 2026/09/10

* `힌트` 개발 미리보기: 플러그인은 AutoJs6 플러그인 센터에 등록되지만 MCP 엔드포인트와 도구는 아직 사용할 수 없습니다
* `기능` 호스트 발견을 위한 INFO 서비스, Wake Activity, `org.autojs.plugin.MCP_SERVER` 서비스 뼈대를 갖춘 플러그인 ID `mcp-server`
* `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록
* `기능` `http://127.0.0.1:9637/mcp`의 Streamable HTTP 엔드포인트와 `device_ping` 도구. adb 또는 호스트가 켜고 끌 수 있는 포그라운드 서비스가 제공 (개발 프리뷰)
* `기능` `/mcp` 엔드포인트의 전송 강화: 바인드 주소와 포트는 서버 설정 저장소에서 가져오고, 요청 본문은 1 MiB로 제한되며, 유휴 연결은 60초 후 닫히고, 포트가 이미 사용 중이거나 바인드가 거부되면 충돌 대신 힌트가 담긴 `port_in_use` / `bind_failed` 상태로 끝납니다
* `기능` SDK 전송 앞단의 DNS rebinding 보호: 루프백 모드는 `Host`로 `localhost` / `127.0.0.1` / `[::1]`만 허용하고, LAN 모드는 기기의 현재 IPv4 주소와 선택적 추가 호스트 이름을 더해 네트워크가 바뀔 때 갱신합니다; 브라우저 출처는 거부되며 "개발자 모드" 스위치만 Inspector의 루프백 출처를 CORS로 허용합니다
* `기능` 서버 식별자 `autojs6-mcp-server`에 플러그인 버전을 담고 tools (`listChanged`), resources, prompts 기능을 선언합니다; `tools/list`는 등록 순서를 유지해 클라이언트가 캐시할 수 있습니다
* `의존성` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`)과 Ktor 3.5.1 CIO 엔진
* `의존성` JVM 전송 테스트를 위해 Ktor 3.5.1 `ktor-server-test-host` 추가 (테스트 범위만)
