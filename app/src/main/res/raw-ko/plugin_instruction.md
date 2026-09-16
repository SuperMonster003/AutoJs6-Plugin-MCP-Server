MCP Server는 AutoJs6가 실행되는 Android 기기를 [Model Context Protocol](https://modelcontextprotocol.io) 서버로 만듭니다. Claude Code, Cursor, MCP Inspector 같은 PC의 AI 에이전트가 USB 또는 Wi-Fi로 휴대폰에 연결하고, 도구를 사용해 스크립트 실행, 로그 읽기, 접근성 노드 트리 확인, 탭과 입력, 스크린샷 촬영, 파일과 앱 작업을 수행합니다.

버전 1.0.2: 도구 37개 (기본 활성화 33개), MCP 리소스와 프롬프트, AutoJs6 서랍 스위치와 플러그인 설정 페이지. AutoJs6 6.8.0 (빌드 5279) 이상이 필요합니다. 선택적 autojs6://docs/ 리소스에는 AutoJs6 오프라인 문서 플러그인과 중계 메서드를 갖춘 호스트도 필요합니다. 진행 상황과 증거는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) 에 기록합니다.

### 사용 방법

1. AutoJs6 빌드 5279 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `MCP Server`가 인식되었는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. AutoJs6 서랍에서 MCP 서버를 켜세요. 제목을 길게 누르거나 플러그인 센터의 설정을 열어 PC 클라이언트 설정을 복사하세요.
4. PC에서 `adb forward tcp:9637 tcp:9637`를 실행하고, MCP 클라이언트가 토큰을 Bearer 자격 증명으로 사용해 `http://127.0.0.1:9637/mcp`에 연결하도록 설정합니다.
5. 휴대전화에서 첫 페어링 요청을 승인하세요. 사용 후 서랍, 설정 또는 알림에서 서버를 중지하세요.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)를 참고하세요.
