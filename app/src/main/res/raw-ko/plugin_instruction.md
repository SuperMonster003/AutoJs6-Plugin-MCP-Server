MCP Server는 AutoJs6가 실행되는 Android 기기를 [Model Context Protocol](https://modelcontextprotocol.io) 서버로 만듭니다. Claude Code, Cursor, MCP Inspector 같은 PC의 AI 에이전트가 USB 또는 Wi-Fi로 휴대폰에 연결하고, 도구를 사용해 스크립트 실행, 로그 읽기, 접근성 노드 트리 확인, 탭과 입력, 스크린샷 촬영, 파일과 앱 작업을 수행합니다.

P3.5 개발 미리보기: 도구 37개 중 33개가 기본 활성화됩니다. 파일 작업, 편집기 위치 지정, 앱 조회, 클립보드, 접근성 자동 활성화, 출력이 제한된 Shell을 사용할 수 있습니다. 서랍 스위치와 설정 화면은 P4에서 구현할 예정입니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 사용 방법

1. AutoJs6 빌드 5279 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `MCP Server`가 인식되었는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. 이 개발 미리 보기에서는 개발 문서에 설명된 adb 제어와 호스트 테스트 세션으로 연결합니다. 서랍 스위치와 플러그인 설정 화면은 P4에서 구현할 예정입니다.
4. PC에서 `adb forward tcp:9637 tcp:9637`를 실행하고, MCP 클라이언트가 토큰을 Bearer 자격 증명으로 사용해 `http://127.0.0.1:9637/mcp`에 연결하도록 설정합니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)를 참고하세요.
