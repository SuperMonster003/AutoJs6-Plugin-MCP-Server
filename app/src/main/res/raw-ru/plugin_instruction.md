MCP Server превращает Android-устройство с AutoJs6 в сервер [Model Context Protocol](https://modelcontextprotocol.io). ИИ-агенты на ПК, такие как Claude Code, Cursor или MCP Inspector, подключаются к телефону по USB или Wi-Fi и с помощью инструментов запускают скрипты, читают журналы, изучают дерево узлов специальных возможностей, нажимают и вводят текст, делают снимки экрана и работают с файлами и приложениями.

Проект находится на стадии каркаса: этот выпуск регистрирует плагин в центре плагинов AutoJs6 и подготавливает инфраструктуру сборки, документации и тестов. Конечная точка MCP и ее инструменты пока недоступны. Прогресс отслеживается по пунктам в [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Использование

1. Установите APK плагина из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) на устройство с AutoJs6 сборки 5278 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `MCP Server` распознан, и включите его. Официальные выпуски проходят проверку подписи автоматически.
3. Включите сервер MCP из боковой панели AutoJs6 или на странице настроек плагина; телефон покажет адрес конечной точки и токен сопряжения.
4. На ПК выполните `adb forward tcp:9637 tcp:9637` и укажите клиенту MCP адрес `http://127.0.0.1:9637/mcp`, используя токен как bearer-учетные данные.

Руководство по подключению и текущий прогресс смотрите в [README проекта](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) и [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
