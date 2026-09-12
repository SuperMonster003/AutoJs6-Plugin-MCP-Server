MCP Server превращает Android-устройство с AutoJs6 в сервер [Model Context Protocol](https://modelcontextprotocol.io). ИИ-агенты на ПК, такие как Claude Code, Cursor или MCP Inspector, подключаются к телефону по USB или Wi-Fi и с помощью инструментов запускают скрипты, читают журналы, изучают дерево узлов специальных возможностей, нажимают и вводят текст, делают снимки экрана и работают с файлами и приложениями.

Предварительная версия до P3.3: реализованы MCP с аутентификацией и сопряжением, инструменты сценариев, интерфейса и снимков экрана. screen_capture возвращает JPEG, PNG или WebP; screen_state сообщает размеры и ориентацию. Переключатель в меню и настройки запланированы в P4. Прогресс и проверки на устройствах указаны в [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Использование

1. Установите APK плагина из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) на устройство с AutoJs6 сборки 5279 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `MCP Server` распознан, и включите его. Официальные выпуски проходят проверку подписи автоматически.
3. В этой предварительной версии используйте управление adb и тестовый сеанс хоста из заметок для разработчиков; переключатель в меню и настройки плагина запланированы в P4.
4. На ПК выполните `adb forward tcp:9637 tcp:9637` и укажите клиенту MCP адрес `http://127.0.0.1:9637/mcp`, используя токен как bearer-учетные данные.

Руководство по подключению и текущий прогресс смотрите в [README проекта](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) и [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
