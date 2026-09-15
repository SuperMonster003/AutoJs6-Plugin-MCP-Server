<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Model Context Protocol을 통해 AI 에이전트에 기기 자동화 기능을 제공</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### 소개

******

MCP Server는 AutoJs6가 실행되는 Android 기기를 [Model Context Protocol](https://modelcontextprotocol.io) 서버로 만듭니다. Claude Code, Cursor, MCP Inspector 같은 PC의 AI 에이전트가 USB 또는 Wi-Fi로 휴대폰에 연결하고, 도구를 사용해 스크립트 실행, 로그 읽기, 접근성 노드 트리 확인, 탭과 입력, 스크린샷 촬영, 파일과 앱 작업을 수행합니다.

서버는 플러그인 자체 프로세스 안에서 실행되며 단일 Streamable HTTP 엔드포인트로 접근합니다. AutoJs6는 Binder를 통해 플러그인에 기능 브로커를 전달하므로 모든 도구 호출은 호스트가 기존 권한, 엔진, 접근성 서비스로 실행합니다. 플러그인은 호스트 기능을 복제하지 않습니다.

******

### 현재 상태

******

P4 개발 미리 보기: 도구 37개 중 33개가 기본 활성화되며 AutoJs6 서랍 스위치와 플러그인 설정 페이지를 제공합니다. 해당 P4 AutoJs6 빌드가 필요합니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### 예정된 기능

******

로드맵은 다음 기능을 단계별로 제공합니다:

- 서버 상태, USB 전달, 포트와 LAN, 토큰 표시/복사/교체, 페어링 해제, 도구 그룹과 root 권한, 개발자 모드, Claude Code / Cursor / Codex / 일반 HTTP 설정 복사, 출시 기록을 제공하며 AutoJs6 테마를 따릅니다. 네트워크 변경은 리스너를 재시작하고 토큰과 권한은 즉시 적용됩니다. 비밀 정보 창은 스크린샷을 차단합니다.
- 스크립트 실행: AutoJs6 안에서 텍스트 또는 파일의 JavaScript를 실행하고, 엔진을 나열하거나 중지하며, 최근 콘솔 출력을 읽습니다.
- 접근성 UI: 노드 트리를 간결한 텍스트 형식으로 덤프하고, AutoJs6 선택자 문법으로 노드를 찾고, 클릭, 길게 누르기, 스크롤, 텍스트 설정, 뒤로와 홈 같은 전역 키를 누릅니다.
- 스크린샷 그룹 (P3.3): screen_capture는 자르기, scale 또는 maxWidth, JPEG / PNG / WebP, 품질 설정을 지원하는 MCP 이미지를 반환합니다. 기본값은 JPEG 품질 70, 긴 변 1280 px입니다. base64가 4 MiB를 초과하면 품질이나 크기를 낮춰 재시도하고 메타데이터에 변경을 표시합니다. screen_state는 화면 켜짐 상태, 크기, 방향, 밀도를 반환합니다. 도구 목록은 37개입니다. MediaProjection 대체 경로에는 2026-09-13 이후 빌드한 AutoJs6와 휴대전화의 승인이 필요하며 호스트 세션에서 승인을 재사용합니다.
- 작업 디렉터리 도구 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 1부터 시작하는 행과 열을 받는 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, shell_exec. 바이너리 읽기는 base64이며 원본 데이터 최대 1 MiB입니다. 쓰기는 호스트 요청 한도도 따릅니다 (보통 JSON 이스케이프 포함 96 KiB). 삭제와 Shell은 기본 비활성화이며 root에는 allowShellRoot와 호스트 shell.root 권한이 추가로 필요합니다. 일치하는 P3.4 호스트 빌드가 필요합니다.
- MCP 리소스 (P3.5)는 페어링 및 그룹 설정에 따라 읽기 전용 작업 파일, 호스트 예제 탐색, 기기 정보, 최근 콘솔 출력을 제공합니다. 텍스트 및 바이너리 읽기는 잘림 상태를 보고합니다. write_autojs6_script, automate_task, debug_selector는 영어와 중국어를 지원하며, 다른 기기 언어에서는 영어를 사용합니다.
- 연결 경로: `adb forward`를 통한 USB, 명시적으로 켜야 하는 로컬 네트워크, PC 측 stdio 브리지, 그리고 OAuth 2.1을 갖춘 선택적 공개 터널.
- 보안: 교체 가능한 Bearer 토큰, 휴대폰에서의 최초 페어링 확인, 그룹별 도구 스위치. 서버는 기본적으로 루프백 인터페이스에서만 수신합니다.

******

### 사용 방법

******

1. AutoJs6 빌드 5279 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `MCP Server`가 인식되었는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. AutoJs6 서랍에서 MCP 서버를 켜세요. 제목을 길게 누르거나 플러그인 센터의 설정을 열어 PC 클라이언트 설정을 복사하세요.
4. PC에서 `adb forward tcp:9637 tcp:9637`를 실행하고, MCP 클라이언트가 토큰을 Bearer 자격 증명으로 사용해 `http://127.0.0.1:9637/mcp`에 연결하도록 설정합니다.
5. 휴대전화에서 첫 페어링 요청을 승인하세요. 사용 후 서랍, 설정 또는 알림에서 서버를 중지하세요.

> 설치, 활성화, 승인, 호환성 안내를 갖춘 MCP 서버 스위치, 알림 중지 동기화, 재연결 시 설정 유지, 서랍과 플러그인 센터에서 권한 확인 후 동일한 설정 페이지 열기를 제공합니다. AutoJs6를 열면 이전 활성 상태를 복원하되 호스트 부재 중 사용자가 중지했다면 유지합니다. 기기 부팅 시 자동 시작하지 않습니다.

******

### 클라이언트 설정

******

Claude Code는 명령 하나로 서버를 등록합니다. 다른 클라이언트는 MCP 설정에서 같은 URL과 헤더를 사용합니다:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

AutoJs6 서랍에서 MCP 서버를 켜세요. 제목을 길게 누르거나 플러그인 센터의 설정을 열어 PC 클라이언트 설정을 복사하세요. 휴대전화에서 첫 페어링 요청을 승인하세요. 사용 후 서랍, 설정 또는 알림에서 서버를 중지하세요.

******

### 연결 경로

******

USB: `adb forward tcp:9637 tcp:9637` 로 휴대전화 포트를 PC에 매핑합니다. 기기가 여러 대이면 `-s <serial>` 을 붙이고 (`adb devices` 로 확인), 에뮬레이터도 같은 방식입니다. 어느 쪽이든 포트가 사용 중이면 설정 페이지에서 포트를 바꾸고 새 포트를 포워딩하세요. 연결 카드에서 포워딩 명령을 바로 복사할 수 있습니다.

로컬 네트워크: 설정 페이지에서 "로컬 네트워크 연결 허용" 을 켭니다. 그러면 설정 페이지에 휴대전화의 현재 주소 (Wi-Fi 변경을 따라감) 와 클라이언트가 같은 네트워크에 있어야 한다는 안내가 표시됩니다. 게스트 네트워크, AP 격리, PC 방화벽이 흔한 장애 요인입니다. 로컬 네트워크에서 온 페어링 요청은 표시되며, 서버가 네트워크에서 접근 가능한 동안 매일 알림으로 상기시킵니다. 알림은 끌 수 있습니다.

두 경로 모두 같은 토큰과 같은 휴대전화 측 페어링을 사용합니다. HTTP 전송이 없는 클라이언트는 클라이언트 연결 절에서 설명하는 stdio 브리지를 사용합니다.

******

### 클라이언트 연결

******

설정 페이지는 아래 각 클라이언트용으로 실제 토큰이 들어간 설정을 복사합니다. 여기의 조각은 `<token>` 을 자리 표시자로 사용합니다. 모든 클라이언트는 Authorization 헤더가 있는 Streamable HTTP로 통신하며, 새 클라이언트의 첫 호출은 휴대전화에서 확인합니다. 검증됨: Claude Code, Codex CLI, MCP Inspector. 나머지 클라이언트는 같은 URL과 헤더를 사용하지만 관리자가 아직 테스트하지 않았습니다.

Claude Code: "클라이언트 설정" 에 있는 명령을 실행합니다 (설정 페이지는 토큰이 포함된 명령을 복사합니다). 이후 `claude mcp list` 가 `autojs6` 를 Connected로 표시합니다.

Cursor: 아래 항목을 `mcp.json` 에 추가합니다:

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

Codex CLI: 토큰을 환경 변수 `AUTOJS6_MCP_TOKEN` 에 넣고 (설정 페이지에서 해당 PowerShell 명령을 복사할 수 있음) 서버를 `config.toml` 에 추가하거나, `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN` 을 실행합니다:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: CLI 모드는 추가 설정이 필요 없고, 웹 UI는 자체 Node 프록시를 통해 휴대전화에 접근합니다. 브라우저 페이지가 엔드포인트에 직접 연결할 때만 설정 페이지에서 개발자 모드를 켜세요:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI 등의 클라이언트: 각자의 MCP 설정에서 같은 URL과 헤더를 사용합니다. 설정 페이지는 `"type": "http"` 가 포함된 범용 JSON 조각을 제공합니다.

Claude Desktop 등 stdio 전용 클라이언트: `npm install -g autojs6-mcp-bridge` 로 브리지를 설치하고 `autojs6-mcp-bridge --serial <serial>` 을 stdio 서버로 등록하며 환경 변수 블록에 `AUTOJS6_MCP_TOKEN` 을 넣습니다 (Claude Desktop 과 Claude Code 조각은 [브리지 README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) 참조). 브리지 0.1.0 은 플러그인 1.0.0 과 짝을 이루며 클라이언트의 프로토콜 버전을 그대로 전달합니다. Claude Code 2.1.257 의 stdio 연결로 검증했습니다.

******

### 자주 묻는 질문

******

- 401 Unauthorized: 토큰이 없거나 잘못 입력되었거나 교체되었습니다. 설정 페이지에서 설정을 다시 복사하세요. 토큰을 교체한 뒤에는 모든 클라이언트에 새 값이 필요합니다.
- 페어링 시간 초과: 새 클라이언트의 첫 호출은 휴대전화에서 허용할 때까지 약 1분을 기다립니다. 휴대전화 잠금을 해제하고 대화상자나 알림 동작을 수락한 뒤 호출을 반복하세요. 거부하면 짧은 대기 시간이 시작되고, 그 뒤의 호출에서 다시 묻습니다.
- HOST_UNAVAILABLE: AutoJs6가 실행 중이 아니거나 플러그인 세션이 닫혔습니다. AutoJs6를 열고 서랍 스위치를 켠 상태로 두며 설정 페이지에서 연결 상태를 확인하세요.
- 접근성 꺼짐: `ui_*` 도구와 화면 캡처에는 AutoJs6 접근성 서비스가 필요합니다. `device_ensure_accessibility` 를 호출하거나 시스템 접근성 설정에서 서비스를 켜세요.
- 포트 사용 중: 서랍에 `port_in_use` 가 표시됩니다. 설정 페이지에서 포트를 바꾸고 새 포트를 adb로 포워딩하세요.
- 로컬 네트워크에 접근 불가: 로컬 네트워크 접근을 켜고, 설정 페이지에 표시된 주소를 사용하며, PC와 휴대전화를 게스트 격리가 없는 같은 네트워크에 두고, PC 방화벽에서 포트를 허용하세요. 휴대전화의 Wi-Fi 절전은 호출마다 수백 밀리초를 더합니다.
- 화면이 꺼진 뒤 서버가 사라짐: 앱의 배터리 사용을 제한하는 기기 (HyperOS 와 MIUI 는 사이드로드한 앱에 기본적으로 그렇게 합니다) 는 배터리 사용 중 화면이 꺼지고 약 1 분 뒤 포그라운드 서비스를 중지합니다. 그러면 설정 페이지에 경고와 "배터리 설정" 버튼이 표시되니 거기서 MCP Server 를 "제한 없음" 으로 선택하세요. 선택 사항인 "유휴 시 자동 중지" (기본값 끄기) 도 선택한 분 동안 요청이 없으면 서버를 중지하고 그 사실을 알리는 알림을 남깁니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되므로 AutoJs6만 바인딩할 수 있습니다.
- INTERNET 권한은 플러그인 자체 HTTP 리스너에만 사용됩니다. 플러그인은 외부 요청을 보내지 않으며 데이터를 수집하지 않습니다.
- 도구 호출은 AutoJs6 기능 브로커를 거치며 호스트 자체에 허용된 범위를 절대 넘지 않습니다. 셸 명령과 파일 삭제 같은 위험한 그룹은 사용자가 켜기 전까지 꺼져 있습니다.
- 백업은 비활성화되어 있으며 토큰은 플러그인의 비공개 저장소에만 보관됩니다.

플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것입니다. 호스트는 이 식별자로 플러그인을 발견하고 호환성을 협상합니다:

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

`McpServerPluginService`는 `:mcp_server` 프로세스에서 호스트 mcp-server-api 계약 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin`를 구현하고 `org.autojs.plugin.MCP_SERVER` (category `mcp-server`)에 응답합니다. `McpServerPluginInfoService`는 `org.autojs.plugin.INFO`에 PluginInfo를 반환합니다. 호스트는 `WakeActivity`로 플러그인을 활성화합니다.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.1

_2026/09/15_

- `개선` compileSdk 를 37 (Android 17) 로 올리며, targetSdk 는 대상 버전에 의존하는 동작을 검증할 때까지 36 으로 유지

#### v1.0.0

_2026/09/15_

- `힌트` P4 개발 미리 보기: 도구 37개 중 33개가 기본 활성화되며 AutoJs6 서랍 스위치와 플러그인 설정 페이지를 제공합니다. 해당 P4 AutoJs6 빌드가 필요합니다. ROADMAP.md.
- `기능` 서버 상태, USB 전달, 포트와 LAN, 토큰 표시/복사/교체, 페어링 해제, 도구 그룹과 root 권한, 개발자 모드, Claude Code / Cursor / Codex / 일반 HTTP 설정 복사, 출시 기록을 제공하며 AutoJs6 테마를 따릅니다. 네트워크 변경은 리스너를 재시작하고 토큰과 권한은 즉시 적용됩니다. 비밀 정보 창은 스크린샷을 차단합니다.
- `기능` MCP 리소스 (P3.5)는 페어링 및 그룹 설정에 따라 읽기 전용 작업 파일, 호스트 예제 탐색, 기기 정보, 최근 콘솔 출력을 제공합니다. 텍스트 및 바이너리 읽기는 잘림 상태를 보고합니다. write_autojs6_script, automate_task, debug_selector는 영어와 중국어를 지원하며, 다른 기기 언어에서는 영어를 사용합니다.
- `기능` 작업 디렉터리 도구 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 1부터 시작하는 행과 열을 받는 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, shell_exec. 바이너리 읽기는 base64이며 원본 데이터 최대 1 MiB입니다. 쓰기는 호스트 요청 한도도 따릅니다 (보통 JSON 이스케이프 포함 96 KiB). 삭제와 Shell은 기본 비활성화이며 root에는 allowShellRoot와 호스트 shell.root 권한이 추가로 필요합니다. 일치하는 P3.4 호스트 빌드가 필요합니다.
- `기능` 호스트 발견을 위한 INFO 서비스, Wake Activity, `org.autojs.plugin.MCP_SERVER` 서비스 뼈대를 갖춘 플러그인 ID `mcp-server`
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록
- `기능` `http://127.0.0.1:9637/mcp`의 Streamable HTTP 엔드포인트와 `device_ping` 도구. adb 또는 호스트가 켜고 끌 수 있는 포그라운드 서비스가 제공 (개발 프리뷰)
- `기능` `/mcp` 엔드포인트의 전송 강화: 바인드 주소와 포트는 서버 설정 저장소에서 가져오고, 요청 본문은 1 MiB로 제한되며, 유휴 연결은 60초 후 닫히고, 포트가 이미 사용 중이거나 바인드가 거부되면 충돌 대신 힌트가 담긴 `port_in_use` / `bind_failed` 상태로 끝납니다
- `기능` SDK 전송 앞단의 DNS rebinding 보호: 루프백 모드는 `Host`로 `localhost` / `127.0.0.1` / `[::1]`만 허용하고, LAN 모드는 기기의 현재 IPv4 주소와 선택적 추가 호스트 이름을 더해 네트워크가 바뀔 때 갱신합니다; 브라우저 출처는 거부되며 "개발자 모드" 스위치만 Inspector의 루프백 출처를 CORS로 허용합니다
- `기능` 서버 식별자 `autojs6-mcp-server`에 플러그인 버전을 담고 tools (`listChanged`), resources, prompts 기능을 선언합니다; `tools/list`는 등록 순서를 유지해 클라이언트가 캐시할 수 있습니다
- `기능` 모든 `/mcp` 요청에 대한 Bearer 토큰 인증: 첫 시작 시 32바이트 토큰을 생성해 Android Keystore의 AES-GCM 키로 감싼 뒤 플러그인의 비공개이며 백업되지 않는 저장소에 보관합니다; `Authorization` 헤더가 없거나 잘못되면 상수 시간 비교 후 `401` + `WWW-Authenticate: Bearer`와 JSON-RPC `-32001` 오류로 거부합니다; 토큰은 로그에 기록되지 않습니다
- `기능` 전송 앞단의 최초 페어링: 페어링되지 않은 클라이언트는 `initialize`와 tools, resources, prompts 목록 조회는 할 수 있지만, 첫 `tools/call`, `resources/read`, `resources/subscribe` 또는 `prompts/get`은 60초 안에 휴대폰에서 확인될 때까지 `PAIRING_REQUIRED` (`-32002`)를 반환합니다; 거부되거나 시간이 초과되면 30초 동안 `PAIRING_DENIED` (`-32003`)를 반환합니다; 클라이언트는 `clientInfo` 이름 (없으면 `User-Agent`)과 주소 종류 (루프백 / LAN)로 식별되므로 토큰을 교체해도 기존 페어링이 유지되며, 최대 32개의 클라이언트를 페어링할 수 있습니다
- `기능` 휴대폰에서의 페어링 확인은 두 경로로 이루어집니다: 허용 / 거부 동작이 있는 높은 우선순위 알림과, 화면이 잠금 해제된 동안의 대화 상자입니다; 서버 설정, 토큰, 페어링된 클라이언트는 원자적으로 교체되는 파일에 저장되어 서버 프로세스와 설정 페이지가 오래된 캐시 없이 공유합니다
- `기능` 그룹 스위치가 있는 도구 카탈로그 (결정 D6): `device_ping` (플러그인 내부), `device_info` (AutoJs6 `device.info`), `script_run` (AutoJs6 `engines.execScript`: JavaScript를 실행하고 최대 `timeoutMs`까지 종료를 기다린 뒤 결과와 최신 콘솔 줄을 반환하며, 실행 중에는 진행 알림을 보냅니다); 모든 도구는 닫힌 JSON Schema (`additionalProperties: false`)를 선언하고 인수는 AutoJs6에 도달하기 전에 검증됩니다; 그룹 스위치 `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell`은 `tool_groups.json`에 저장되며, 꺼진 그룹은 다음 요청부터 `tools/list`에서 사라지고 해당 도구는 `TOOL_DISABLED`로 응답합니다
- `기능` 호스트 브리지: `org.autojs.plugin.MCP_SERVER` 서비스가 실제 `IMcpServerPlugin` Binder를 구현합니다 (`getInfo` / `getCapabilities`는 계약 버전 1, 도구 그룹, MCP 프로토콜 버전, SDK 버전을 보고하고, `openServer`는 설치된 동일 서명의 AutoJs6만 허용하며 `getStatus` / `updateConfig` / `stop` / `close`를 가진 `IMcpServerSession`을 반환합니다); 도구 호출은 단조 증가하는 요청 id, 호출별 시간 제한, 4개 동시 실행 상한과 함께 호스트 기능 브로커를 거치며, 호스트 오류 범주는 `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`로 매핑됩니다; AutoJs6가 종료되어도 리스너는 계속 실행되고 호스트 기반 도구는 호스트가 다시 연결될 때까지 `HOST_UNAVAILABLE`로 응답합니다; 상태와 이벤트 (`pairing_requested`, `client_paired`, `tool_call`, `warning`)는 콜백을 통해 호스트에 전달됩니다
- `기능` 포그라운드 서비스 알림에 엔드포인트, AutoJs6 연결 상태, 페어링된 클라이언트 수와 중지 동작을 표시합니다; 알림이 차단된 경우 토스트로 엔드포인트를 알립니다; `dumpsys activity service`는 호스트 세션, 도구 그룹 스위치, 등록된 도구도 함께 출력합니다
- `기능` 스크립트 그룹 완성: `script_run_file`은 기기의 스크립트 파일을 실행하고, `script_stop` / `script_stop_all`은 AutoJs6 실행을 하나 또는 전부 중지하며, `script_list`는 실행 중인 항목을 나열하고, `console_tail`은 `nextSinceId` 커서와 레벨 필터와 함께 최신 콘솔 줄을 반환합니다; `script_run`과 `script_run_file`은 이제 `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 줄 번호가 포함된 예외, 최신 콘솔 줄을 반환하며, 대기 중에는 2초마다 최신 콘솔 줄을 담은 진행 알림을 보냅니다
- `기능` MCP 엔드포인트의 응답이 서버 전송 이벤트 (SSE) 로 스트리밍됩니다 (SDK 의 JSON 응답 모드는 사용하지 않음). 실행 중인 스크립트의 진행 하트비트처럼 요청에 속한 알림은 해당 요청 자신의 응답으로 클라이언트에 전달됩니다
- `기능` UI 그룹 추가 (roadmap P3.2): `ui_dump`는 현재 창을 `#n` 참조가 달린 간결한 노드 트리로 반환하고 (`format`은 text / json / xml, `maxNodes`는 최대 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for`는 선택자를 폴링하며, `ui_current_window`와 `ui_explain_selector`는 창과 선택자가 실패하는 이유를 보고하고, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll`은 `nodeRef` (지문으로 다시 찾으며 사라지면 `NODE_REF_STALE`) 또는 `selector`에 작용하며, `ui_press_key`는 back / home / recents / notifications / quick_settings / power_dialog / lock_screen을 누르고, 기본적으로 꺼져 있는 `ui_gesture` 그룹은 `ui_swipe`, `ui_gesture`와 클릭 도구의 좌표 형식을 추가합니다 (그룹이 꺼져 있으면 `TOOL_DISABLED`); 도구 카탈로그 스냅샷은 20개 도구로 늘어났습니다; 좌표 제스처에는 2026-09-11 이후에 빌드된 AutoJs6 호스트가 필요합니다 (더 오래된 호스트는 무작위로 "the system cancelled ..."로 응답합니다)
- `기능` 스크린샷 그룹 (P3.3): screen_capture는 자르기, scale 또는 maxWidth, JPEG / PNG / WebP, 품질 설정을 지원하는 MCP 이미지를 반환합니다. 기본값은 JPEG 품질 70, 긴 변 1280 px입니다. base64가 4 MiB를 초과하면 품질이나 크기를 낮춰 재시도하고 메타데이터에 변경을 표시합니다. screen_state는 화면 켜짐 상태, 크기, 방향, 밀도를 반환합니다. 도구 목록은 22개입니다. MediaProjection 대체 경로에는 2026-09-13 이후 빌드한 AutoJs6와 휴대전화의 승인이 필요하며 호스트 세션에서 승인을 재사용합니다.
- `기능` 로컬 네트워크 경로 (P5.1): 로컬 네트워크 접근을 켜면 설정 페이지에 휴대전화의 현재 주소 (Wi-Fi 변경 시 갱신) 와 같은 네트워크 / 방화벽 안내를 표시. 로컬 네트워크에서 온 페어링 요청은 대화상자와 알림에 표시. 서버가 아직 로컬 네트워크에서 접근 가능하다는 알림을 매일 보내며 리스너를 재시작하지 않고 끌 수 있음. README에 USB 와 로컬 네트워크 경로를 기록.
- `기능` 클라이언트별 속도 제한 (P6): 클라이언트당 초당 최대 20 요청, 분당 최대 30 회의 screen_capture. 한도를 넘는 요청은 Retry-After 헤더와 retryAfterMs 를 담은 JSON-RPC RATE_LIMITED 오류가 있는 HTTP 429 를 받고, 한도를 넘는 스크린샷은 retryAfterMs 가 있는 RATE_LIMITED 도구 결과로 응답되어 모델이 기다릴 수 있습니다. 호스트 grant 의 접근성 조회 속도는 그 위에 계속 적용됩니다.
- `기능` 유휴 자동 중지와 배터리 (P6): 설정 페이지에 "유휴 시 자동 중지" (기본값 끄기, 5 / 15 / 30 / 60 / 120 분) 를 추가했습니다. 리스너는 선택한 시간 동안 클라이언트 요청이 없으면 스스로 중지하고, 플러그인 내부의 idle_timeout 사유를 기록하며, 자동으로 사라지는 알림 하나를 남기고, 사용자 중지로 세지 않습니다. 처리 중인 요청 (실행 중인 도구 호출) 은 유휴로 보지 않으며, 스트림만 열어 두고 요청을 보내지 않는 클라이언트는 서버를 계속 실행시키지 않습니다. Android 가 이 앱의 배터리 사용을 제한하면 (HyperOS 와 MIUI 는 사이드로드한 앱에 기본적으로 그렇게 하며, 배터리 사용 중 화면이 꺼지고 약 1 분 뒤 포그라운드 서비스를 중지합니다) 설정 페이지에 경고와 "배터리 설정" 버튼을 표시하고 AutoJs6 는 warning 이벤트를 받습니다. 유휴 CPU 시간과 1 시간 유휴의 batterystats 추정치는 docs/dev/p6-battery-and-residency.md 에 기록했습니다.
- `수정` IDE rebuild 시 JVM 단위 테스트용 APK를 찾던 문제 해결. APK 검증 작업이 필요한 산출물을 자동으로 빌드하므로 clean 후에도 바로 실행 가능.
- `수정` 플러그인 센터의 밝은 모드와 어두운 모드에서 아이콘 비율이 달라지는 문제; 야간에도 적응형 아이콘을 사용하고 레이어 크기를 조정하여 ic_launcher_round.png 의 전체 그림과 여백을 유지하며 배경색만 변경
- `수정` 설정 페이지의 키보드 Tab 이동이 툴바 뒤로 버튼을 건너뛰던 문제 해결. 이제 Tab 순환이 뒤로 버튼과 모든 컨트롤을 포함 (Android 7 포함). 기기 테스트로 스크린 리더 레이블과 키보드 조작을 확인.
- `수정` script_run 과 script_run_file 의 arguments 맵이 값 타입을 JSON Schema 배열로 선언해 일부 MCP 클라이언트가 거부하거나 약화하던 문제 수정. 이제 스키마는 단일 타입 anyOf 분기를 사용. README에 클라이언트 연결과 자주 묻는 질문 장, 검증된 클라이언트 표 추가.
- `수정` 악의적인 요청 본문은 파서가 재귀하기 전에 거부됩니다. 64단계보다 깊게 중첩된 JSON, 요청 id 가 중복된 본문, 같은 세션에서 아직 처리 중인 요청 id (이전에는 먼저 온 요청이 응답을 받지 못했음) 는 400 과 JSON-RPC 오류를 반환하고, 유효한 세션이 없는 GET 스트림은 빈 이벤트 스트림 대신 400 / 404 를 반환합니다. JVM 테스트와 기기 테스트 (API 28 / 31 / 33 / 35) 가 초과 길이, 과도한 깊이, 잘못된 UTF-8, 알 수 없는 메서드, 헤더 조합, 큰 base64, 64 동시 세션을 검증합니다.
- `수정` 수명 주기 매트릭스 (P6): 이전 리스너 프로세스의 세션 id 를 담은 요청 (리스너가 종료되었거나 토큰 교체로 다시 시작된 뒤) 은 전송 계층의 404 에 맡겨 클라이언트가 다시 initialize 하도록 하며, 이미 페어링된 클라이언트에 대해 요청의 User-Agent 이름으로 불필요한 페어링 확인을 띄우지 않습니다. 죽은 리스너 프로세스가 알림창에 남긴 페어링 알림은 다음 리스너가 시작될 때 지워집니다. 호스트 종료, 리스너 종료, 둘의 동시 종료, 시스템 설정에서의 강제 중지, 활성 세션 중의 토큰 교체, 호스트 종료와 리스너 종료를 가로지르는 대기 중 페어링에 대해 기대 상태와 복구 경로를 docs/dev/lifecycle-matrix.md 에 기록하고 실제 기기에서 검증했습니다.
- `개선` 빌드 시 의도하지 않은 네이티브 의존성을 거부하고 JSON 보고서 생성
- `의존성` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`)과 Ktor 3.5.1 CIO 엔진
- `의존성` JVM 전송 테스트를 위해 Ktor 3.5.1 `ktor-server-test-host` 추가 (테스트 범위만)
- `의존성` `mcp-server-api.aar` (AutoJs6 모듈 `plugin-api/mcp-server-api`, 호스트 빌드 6.8.0 / 5279, MPL 2.0)을 AutoJs6와 플러그인 사이의 Binder 계약으로 추가하고 `locks/host-api-aars.lock`에 해시를 고정
- `의존성` P4 호스트의 common-plugin-api 및 mcp-server-api AAR 업데이트: 선택적 설정 확장 v1, AIDL 순서 유지, SHA-256 잠금, SDK 36 호환성 유지.

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

이 섹션은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다. 일반 사용자는 Releases 페이지의 미리 빌드된 APK를 설치하면 됩니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 계측 테스트 APK 빌드:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

릴리스 산출물을 수집하고 파일 이름에 버전과 CRC32 다이제스트를 추가:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서 소스와 생성된 산출물이 동기화되어 있는지 검증 (CI에서도 적용):

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상과 Android SDK 37이 필요합니다. Gradle과 플러그인 버전은 `version.properties`와 `io.github.supermonster003.autojs6-platform-versions`로 중앙에서 관리됩니다.

******

### 현지화와 문서 생성

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

`.readme/`와 `.changelog/`의 언어 JSON 파일이 README, 플러그인 센터 안내, 변경 기록의 유일한 소스입니다. 항상 이 JSON 소스를 편집하고 `py .python/generate_markdown.py`를 다시 실행하세요. 생성된 README, `plugin_instruction.md`, 변경 기록 산출물은 절대 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`를 실행하면 모든 생성 산출물을 검증할 수 있습니다.

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE)에 따라 제공됩니다. 서드파티 구성 요소와 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- Model Context Protocol 사양: https://modelcontextprotocol.io
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
