<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Открывает автоматизацию устройства для ИИ-агентов по протоколу Model Context Protocol</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### Введение

******

MCP Server превращает Android-устройство с AutoJs6 в сервер [Model Context Protocol](https://modelcontextprotocol.io). ИИ-агенты на ПК, такие как Claude Code, Cursor или MCP Inspector, подключаются к телефону по USB или Wi-Fi и с помощью инструментов запускают скрипты, читают журналы, изучают дерево узлов специальных возможностей, нажимают и вводят текст, делают снимки экрана и работают с файлами и приложениями.

Сервер работает в собственном процессе плагина и доступен через единственную конечную точку Streamable HTTP. AutoJs6 передает плагину брокер возможностей через Binder, поэтому каждый вызов инструмента выполняется хостом с его существующими разрешениями, движками и службой специальных возможностей; плагин никогда не дублирует функциональность хоста.

******

### Состояние

******

Предварительная версия P4: 37 инструментов, 33 включены по умолчанию, переключатель в AutoJs6 и страница настроек плагина. Требуется соответствующая сборка AutoJs6 P4. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Планируемые возможности

******

Дорожная карта поэтапно предоставляет следующие возможности:

- Настройки телефона для состояния сервера, USB, порта и LAN, просмотра/копирования/замены токена, отзыва сопряжений, групп инструментов и root, режима разработчика, копирования конфигураций Claude Code / Cursor / Codex / HTTP и истории выпусков с оформлением AutoJs6. Сеть применяется перезапуском сервера; токены и разрешения действуют сразу. Секретные окна блокируют снимки экрана.
- Выполнение скриптов: запуск JavaScript из текста или файла внутри AutoJs6, просмотр и остановка движков, чтение последнего вывода консоли.
- Интерфейс специальных возможностей: выгрузка дерева узлов в компактном текстовом формате, поиск узлов с помощью синтаксиса селекторов AutoJs6, нажатие, долгое нажатие, прокрутка, ввод текста и глобальные клавиши, такие как Назад и Домой.
- Группа снимков экрана (P3.3): screen_capture возвращает изображения MCP с обрезкой, scale или maxWidth, JPEG / PNG / WebP и выбором качества. По умолчанию JPEG, качество 70, длинная сторона 1280 px. При превышении 4 MiB base64 качество или размер уменьшаются, изменения отражаются в метаданных. screen_state сообщает состояние экрана, размеры, ориентацию и плотность. Каталог содержит 37 инструмента. Резервный путь MediaProjection требует AutoJs6, собранный 2026-09-13 или позже, и согласие на телефоне, повторно используемое сеансом хоста.
- Инструменты рабочего каталога (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open с нумерацией строк и столбцов от 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast и shell_exec. Двоичное чтение возвращает base64, до 1 MiB исходных данных. Запись также ограничена бюджетом запроса хоста (обычно 96 KiB с экранированием JSON). Удаление и Shell выключены по умолчанию; root требует allowShellRoot и разрешение shell.root хоста. Нужна соответствующая сборка хоста P3.4.
- Ресурсы MCP (P3.5) предоставляют доступ только для чтения к рабочим файлам, встроенным примерам, сведениям об устройстве и последним сообщениям консоли с учетом сопряжения и групп. Чтение текста и двоичных данных сообщает об усечении. Шаблоны write_autojs6_script, automate_task и debug_selector доступны на английском и китайском, для остальных языков телефона используется английский.
- Способы подключения: USB через `adb forward`, локальная сеть с явным включением, stdio-мост на стороне ПК и необязательный публичный туннель с OAuth 2.1.
- Безопасность: сменяемый bearer-токен, подтверждение сопряжения на телефоне при первом использовании и переключатели инструментов по группам; по умолчанию сервер слушает только интерфейс обратной петли.

******

### Использование

******

1. Установите APK плагина из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) на устройство с AutoJs6 сборки 5279 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `MCP Server` распознан, и включите его. Официальные выпуски проходят проверку подписи автоматически.
3. Включите MCP Server в боковой панели AutoJs6. Удерживайте заголовок или откройте настройки из Центра плагинов. Скопируйте конфигурацию клиента ПК.
4. На ПК выполните `adb forward tcp:9637 tcp:9637` и укажите клиенту MCP адрес `http://127.0.0.1:9637/mcp`, используя токен как bearer-учетные данные.
5. Подтвердите первый запрос сопряжения на телефоне. По окончании остановите сервер из панели, настроек или уведомления.

> Переключатель MCP Server с подсказками установки, активации, разрешений и совместимости; синхронизация остановки из уведомления; сохранение настроек при переподключении; проверенный вход в общие настройки из панели и Центра плагинов. Восстанавливает сервер при открытии AutoJs6, кроме остановки пользователем при отсутствии хоста; без запуска при загрузке устройства.

******

### Настройка клиента

******

Claude Code регистрирует сервер одной командой; другие клиенты используют тот же URL и заголовок в своей конфигурации MCP:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

Включите MCP Server в боковой панели AutoJs6. Удерживайте заголовок или откройте настройки из Центра плагинов. Скопируйте конфигурацию клиента ПК. Подтвердите первый запрос сопряжения на телефоне. По окончании остановите сервер из панели, настроек или уведомления.

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Точки входа Binder защищены разрешением подписи `org.autojs.permission.PLUGIN`, поэтому привязаться к ним может только AutoJs6.
- Разрешение INTERNET используется только для собственного HTTP-слушателя плагина; плагин не выполняет исходящих запросов и не собирает данные.
- Вызовы инструментов проходят через брокер возможностей AutoJs6 и никогда не выходят за пределы того, что разрешено самому хосту; опасные группы, такие как команды оболочки и удаление файлов, остаются выключенными, пока пользователь их не включит.
- Резервное копирование отключено, а токены хранятся только в приватном хранилище плагина.

Получайте плагин только со страницы официальных [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) или из центра плагинов AutoJs6. Пакеты из неизвестных источников могут не пройти проверку хоста или нести риски, даже если номер версии выглядит одинаково.

******

### Интерфейс плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования совместимости:

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

`McpServerPluginService` реализует контракт mcp-server-api хоста `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` в процессе `:mcp_server` и отвечает на `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` отвечает на `org.autojs.plugin.INFO` объектом PluginInfo. `WakeActivity` позволяет хосту активировать плагин.

******

### Дорожная карта

******

Планы и прогресс плагина ведутся в виде списка с отметками в ROADMAP.md, организованного по этапам с критериями приемки и уровнями доказательств. Неотмеченные пункты выражают намерение, а не текущие возможности; обсуждение через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/09/13_

- `Подсказка` Предварительная версия P4: 37 инструментов, 33 включены по умолчанию, переключатель в AutoJs6 и страница настроек плагина. Требуется соответствующая сборка AutoJs6 P4. ROADMAP.md.
- `Функция` Настройки телефона для состояния сервера, USB, порта и LAN, просмотра/копирования/замены токена, отзыва сопряжений, групп инструментов и root, режима разработчика, копирования конфигураций Claude Code / Cursor / Codex / HTTP и истории выпусков с оформлением AutoJs6. Сеть применяется перезапуском сервера; токены и разрешения действуют сразу. Секретные окна блокируют снимки экрана.
- `Функция` Ресурсы MCP (P3.5) предоставляют доступ только для чтения к рабочим файлам, встроенным примерам, сведениям об устройстве и последним сообщениям консоли с учетом сопряжения и групп. Чтение текста и двоичных данных сообщает об усечении. Шаблоны write_autojs6_script, automate_task и debug_selector доступны на английском и китайском, для остальных языков телефона используется английский.
- `Функция` Инструменты рабочего каталога (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open с нумерацией строк и столбцов от 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast и shell_exec. Двоичное чтение возвращает base64, до 1 MiB исходных данных. Запись также ограничена бюджетом запроса хоста (обычно 96 KiB с экранированием JSON). Удаление и Shell выключены по умолчанию; root требует allowShellRoot и разрешение shell.root хоста. Нужна соответствующая сборка хоста P3.4.
- `Функция` Идентификатор плагина `mcp-server` со службой INFO, Wake Activity и каркасом службы `org.autojs.plugin.MCP_SERVER` для обнаружения хостом
- `Функция` README, инструкции центра плагинов и журнал изменений на 10 языках
- `Функция` Конечная точка Streamable HTTP по адресу `http://127.0.0.1:9637/mcp` с инструментом `device_ping`, размещенная в службе переднего плана, которую можно включать и выключать через adb или из хоста (предварительная версия для разработки)
- `Функция` Усиление транспорта конечной точки `/mcp`: адрес и порт привязки берутся из хранилища настроек сервера, тело запроса ограничено 1 MiB, простаивающие соединения закрываются через 60 с, а занятый порт или отклонённая привязка завершаются статусом `port_in_use` / `bind_failed` с подсказкой вместо сбоя
- `Функция` Защита от DNS rebinding перед транспортом SDK: в режиме loopback в `Host` принимаются только `localhost` / `127.0.0.1` / `[::1]`, в режиме LAN добавляются текущие IPv4-адреса устройства и необязательные дополнительные имена хостов с обновлением при смене сети; источники браузера отклоняются, и только переключатель "режим разработчика" пропускает loopback-источник Inspector через CORS
- `Функция` Идентификатор сервера `autojs6-mcp-server` с версией плагина и возможностями tools (`listChanged`), resources и prompts; `tools/list` сохраняет порядок регистрации, чтобы клиенты могли его кэшировать
- `Функция` Аутентификация по Bearer-токену для каждого запроса `/mcp`: 32-байтовый токен создаётся при первом запуске, оборачивается ключом AES-GCM из Android Keystore и хранится в приватном хранилище плагина, не попадающем в резервные копии; отсутствующий или неверный заголовок `Authorization` отклоняется после сравнения за постоянное время ответом `401` + `WWW-Authenticate: Bearer` и ошибкой JSON-RPC `-32001`; токен не попадает в журнал
- `Функция` Первичное сопряжение перед транспортом: несопряжённый клиент может выполнить `initialize` и получить списки tools, resources и prompts, но его первый `tools/call`, `resources/read`, `resources/subscribe` или `prompts/get` получает `PAIRING_REQUIRED` (`-32002`), пока запрос не подтверждён на телефоне в течение 60 с; отказ или тайм-аут дают `PAIRING_DENIED` (`-32003`) на 30 с; клиент опознаётся по имени из `clientInfo` (или `User-Agent`) и классу адреса (loopback / LAN), поэтому ротация токена сохраняет сопряжения, а сопрячь можно до 32 клиентов
- `Функция` Подтверждение сопряжения на телефоне идёт по двум каналам: уведомление с высоким приоритетом и действиями Разрешить / Отклонить, а также диалог при разблокированном экране; настройки сервера, токен и сопряжённые клиенты хранятся в атомарно заменяемых файлах, которые процесс сервера и страница настроек используют без устаревших кэшей
- `Функция` Каталог инструментов с групповыми переключателями (решение D6): `device_ping` (внутри плагина), `device_info` (`device.info` AutoJs6) и `script_run` (`engines.execScript` AutoJs6: выполняет JavaScript, ждёт завершения до `timeoutMs` и возвращает результат вместе с последними строками консоли, отправляя уведомления о ходе выполнения); каждый инструмент объявляет закрытую JSON Schema (`additionalProperties: false`), а аргументы проверяются до того, как что-либо попадёт в AutoJs6; групповые переключатели `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` хранятся в `tool_groups.json`, выключенная группа исчезает из `tools/list` со следующего запроса, а её инструменты отвечают `TOOL_DISABLED`
- `Функция` Мост к хосту: служба `org.autojs.plugin.MCP_SERVER` реализует настоящий Binder `IMcpServerPlugin` (`getInfo` / `getCapabilities` сообщают версию контракта 1, группы инструментов, версии протокола MCP и версию SDK; `openServer` принимает только установленный AutoJs6 с той же подписью и возвращает `IMcpServerSession` с `getStatus` / `updateConfig` / `stop` / `close`); вызовы инструментов идут через брокер возможностей хоста с монотонными идентификаторами запросов, тайм-аутом на вызов, лимитом в 4 одновременных вызова и сопоставлением категорий ошибок хоста с `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; при завершении AutoJs6 слушатель продолжает работать, а зависящие от хоста инструменты отвечают `HOST_UNAVAILABLE`, пока хост не подключится снова; статус и события (`pairing_requested`, `client_paired`, `tool_call`, `warning`) доходят до хоста через его обратный вызов
- `Функция` Уведомление службы переднего плана показывает конечную точку, состояние подключения AutoJs6 и число сопряжённых клиентов, а также действие Остановить; если уведомления заблокированы, конечную точку называет всплывающее сообщение; `dumpsys activity service` дополнительно печатает сеанс хоста, групповые переключатели и зарегистрированные инструменты
- `Функция` Завершена группа скриптов: `script_run_file` запускает файл скрипта на устройстве, `script_stop` / `script_stop_all` останавливают одно или все выполнения AutoJs6, `script_list` перечисляет запущенные, а `console_tail` возвращает новейшие строки консоли с курсором `nextSinceId` и фильтром по уровню; `script_run` и `script_run_file` теперь сообщают `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, исключение с номером строки и новейшие строки консоли, а во время ожидания каждые 2 с отправляется уведомление о ходе выполнения с новейшей строкой консоли
- `Функция` Ответы конечной точки MCP передаются потоком server-sent events (режим JSON-ответов SDK не используется), поэтому уведомление, относящееся к запросу, например индикатор выполнения работающего скрипта, доходит до клиента в ответе на этот запрос
- `Функция` Добавлена группа UI (roadmap P3.2): `ui_dump` возвращает активное окно как компактное дерево узлов со ссылками `#n` (`format` text / json / xml, `maxNodes` до 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` опрашивают селектор, `ui_current_window` и `ui_explain_selector` сообщают окно и причину неудачи селектора, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` действуют на `nodeRef` (узел находится заново по отпечатку, `NODE_REF_STALE`, если он исчез) или на `selector`, `ui_press_key` нажимает back / home / recents / notifications / quick_settings / power_dialog / lock_screen, а выключенная по умолчанию группа `ui_gesture` добавляет `ui_swipe`, `ui_gesture` и координатную форму инструментов клика (`TOOL_DISABLED`, пока группа выключена); снимок каталога инструментов вырос до 20 инструментов; координатным жестам нужен хост AutoJs6, собранный 2026-09-11 или позже (более старый хост случайным образом отвечает "the system cancelled ...")
- `Функция` Группа снимков экрана (P3.3): screen_capture возвращает изображения MCP с обрезкой, scale или maxWidth, JPEG / PNG / WebP и выбором качества. По умолчанию JPEG, качество 70, длинная сторона 1280 px. При превышении 4 MiB base64 качество или размер уменьшаются, изменения отражаются в метаданных. screen_state сообщает состояние экрана, размеры, ориентацию и плотность. Каталог содержит 22 инструмента. Резервный путь MediaProjection требует AutoJs6, собранный 2026-09-13 или позже, и согласие на телефоне, повторно используемое сеансом хоста.
- `Исправление` IDE rebuild больше не ищет APK для модульных тестов JVM. Задачи проверки APK автоматически собирают необходимые файлы и работают после clean.
- `Исправление` Пропорции значка центра плагинов различались в светлом и темном режимах; ночной режим также использует адаптивный значок, а размер слоев скорректирован для сохранения полного рисунка и отступов ic_launcher_round.png с изменением только цвета фона
- `Улучшение` Проверка сборки отклоняет непреднамеренные нативные зависимости и создает отчет JSON
- `Зависимость` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) на движке Ktor 3.5.1 CIO
- `Зависимость` Добавлен Ktor 3.5.1 `ktor-server-test-host` для JVM-тестов транспорта (только тестовая область)
- `Зависимость` Добавлен `mcp-server-api.aar` (модуль AutoJs6 `plugin-api/mcp-server-api`, сборка хоста 6.8.0 / 5279, MPL 2.0) как контракт Binder между AutoJs6 и плагином, хеш закреплён в `locks/host-api-aars.lock`
- `Зависимость` Обновление AAR common-plugin-api и mcp-server-api из хоста P4: необязательное расширение настроек v1, прежний порядок AIDL, фиксация SHA-256 и совместимость с SDK 36.

##### Полная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка и проверка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходного кода; обычные пользователи могут просто установить готовый APK со страницы Releases.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и собрать APK инструментальных тестов:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать выпускной APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Собрать выпускной артефакт и добавить версию и контрольную сумму CRC32 к имени файла:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что источники многоязычной документации и сгенерированные артефакты синхронизированы (также проверяется в CI):

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуются JDK 21 или новее и Android SDK 36; версии Gradle и плагинов централизованно управляются через `version.properties` и `io.github.supermonster003.autojs6-platform-versions`.

******

### Локализация и генерация документации

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

Языковые JSON-файлы в `.readme/` и `.changelog/` являются единственным источником README, инструкций центра плагинов и журнала изменений. Всегда редактируйте эти JSON-источники и перезапускайте `py .python/generate_markdown.py`; сгенерированные README, `plugin_instruction.md` и журнал изменений никогда не правятся вручную. Запустите `py .python/generate_markdown.py --check`, чтобы проверить все сгенерированные артефакты.

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE). Сторонние компоненты и их лицензии перечислены в [уведомлениях о сторонних компонентах](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Спецификация Model Context Protocol: https://modelcontextprotocol.io
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
