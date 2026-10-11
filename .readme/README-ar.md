<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>يتيح أتمتة الجهاز لوكلاء الذكاء الاصطناعي عبر Model Context Protocol</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

يحول MCP Server جهاز Android الذي يعمل عليه AutoJs6 إلى خادم [Model Context Protocol](https://modelcontextprotocol.io). يتصل وكلاء الذكاء الاصطناعي على الحاسوب, مثل Claude Code أو Cursor أو MCP Inspector, بالهاتف عبر USB أو Wi-Fi ويستخدمون الأدوات لتشغيل البرامج النصية, وقراءة السجلات, وفحص شجرة عقد إمكانية الوصول, والنقر والكتابة, والتقاط لقطات الشاشة, والتعامل مع الملفات والتطبيقات.

يعمل الخادم داخل عملية المكون الإضافي الخاصة ويتم الوصول إليه عبر نقطة نهاية Streamable HTTP واحدة. يسلم AutoJs6 المكون الإضافي وسيط قدرات عبر Binder, لذا ينفذ المضيف كل استدعاء أداة بصلاحياته ومحركاته وخدمة إمكانية الوصول الحالية; ولا يكرر المكون الإضافي وظائف المضيف أبدا.

******

### الحالة

******

الإصدار 1.0.4: 37 أداة (33 مفعلة افتراضيا), موارد وقوالب MCP, مفتاح في قائمة AutoJs6 وصفحة إعدادات للإضافة. يتطلب AutoJs6 6.8.0 (البناء 5279) أو أحدث; كما تحتاج موارد autojs6://docs/ الاختيارية إلى إضافة AutoJs6 Offline Docs ومضيف يملك طرق التمرير. يسجل التقدم والأدلة في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### الميزات

******

توفر الإضافة القدرات التالية:

- إعدادات الهاتف لحالة الخادم وUSB والمنفذ والشبكة المحلية وعرض الرمز ونسخه وتغييره وإلغاء الاقتران ومجموعات الأدوات وصلاحيات root ووضع المطور وإعدادات Claude Code / Cursor / Codex / VS Code / HTTP القابلة للنسخ (وكرمز QR أيضا) وسجل الإصدارات مع مظهر AutoJs6. تغييرات الشبكة تعيد تشغيل المستمع, وتطبق الرموز والأذونات فورا. نوافذ الأسرار تمنع لقطات الشاشة.
- تنفيذ البرامج النصية: تشغيل JavaScript من نص أو من ملف داخل AutoJs6, وعرض المحركات وإيقافها, وقراءة مخرجات وحدة التحكم الأخيرة.
- واجهة إمكانية الوصول: تفريغ شجرة العقد بتنسيق نصي مضغوط, والعثور على العقد بصيغة محددات AutoJs6, والنقر, والضغط المطول, والتمرير, وتعيين النص, والضغط على المفاتيح العامة مثل رجوع والشاشة الرئيسية.
- مجموعة لقطات الشاشة (P3.3): تعيد screen_capture صور MCP مع القص وscale أو maxWidth وتنسيقات JPEG / PNG / WebP والتحكم في الجودة. الإعداد الافتراضي JPEG بجودة 70 وضلع أطول 1280 px. عند تجاوز base64 حجم 4 MiB تنخفض الجودة أو الأبعاد مع بيان التعديلات في البيانات الوصفية. تعرض screen_state حالة الشاشة والأبعاد والاتجاه والكثافة. يضم الدليل 37 أداة. يتطلب البديل MediaProjection إصدار AutoJs6 مبنيا في 2026-09-13 أو بعده وموافقة على الهاتف تعيد جلسة المضيف استخدامها. مع annotate: true ترسم مراجع #n الخاصة بآخر ui_dump / ui_find على الصورة (إطار وتسمية لكل عقدة) لمطابقتها مع ما يظهر على الشاشة.
- أدوات مجلد العمل (P3.4): files_list / stat / read / write / mkdir / rename / delete, و editor_open بسطر وعمود يبدأ ترقيمهما من 1, و app_launch / list, و clipboard_get / set, و device_ensure_accessibility, و toast, و shell_exec. القراءة الثنائية تعيد base64 بحد أقصى 1 MiB للبيانات الأصلية. تخضع الكتابة أيضا لميزانية طلب المضيف (عادة 96 KiB بما فيها ترميز JSON). الحذف و Shell معطلان افتراضيا; يتطلب root أيضا allowShellRoot ومنح shell.root من المضيف. يلزم بناء المضيف المطابق لـ P3.4.
- توفر موارد MCP (P3.5) ملفات العمل للقراءة فقط, وتصفح أمثلة المضيف, والتوثيق دون اتصال عند تثبيت مكون AutoJs6 Offline Docs الإضافي, ومعلومات الجهاز, ومخرجات وحدة التحكم الأخيرة, مع احترام الاقتران وإعدادات المجموعات. تتضمن قراءة النص والبيانات الثنائية معلومات الاقتطاع. توفر القوالب write_autojs6_script و automate_task و debug_selector إرشادات بالإنجليزية والصينية, مع استخدام الإنجليزية للغات الهاتف الأخرى.
- مسارات الاتصال: USB عبر `adb forward`, والشبكة المحلية بتفعيل صريح, وجسر stdio على جانب الحاسوب للعملاء الذين لا يملكون نقل HTTP.
- الأمان: رمز bearer قابل للتدوير, وتأكيد الاقتران على الهاتف عند أول استخدام, ومفاتيح تبديل للأدوات حسب المجموعة; ولا يستمع الخادم افتراضيا إلا على واجهة الاسترجاع المحلي.

******

### الأدوات

******

الجدول أدناه مولد من لقطة كتالوج أدوات الإضافة (`app/src/test/resources/tool-catalog.snapshot.json`); الأوصاف هي النصوص الإنجليزية التي يتلقاها العملاء, ويمكن إيقاف كل مجموعة من صفحة الإعدادات:

| الأداة | المجموعة | الافتراضي | الوصف |
|---|---|---|---|
| `device_ping` | `device` | مفعل | Confirms that the AutoJs6 MCP Server plugin is reachable and returns its version, the device model, the Android API level, and the device time. |
| `device_info` | `device` | مفعل | Returns the device build, screen, battery, memory, AutoJs6 host version and process, accessibility service state, screen state, locale, and time zone as AutoJs6 reports them (schema autojs6-bridge-device-info-v1). No hardware identifiers. |
| `script_run` | `script` | مفعل | Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_run_file` | `script` | مفعل | Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) and by default waits for it to finish. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_stop` | `script` | مفعل | Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported. |
| `script_stop_all` | `script` | مفعل | Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped. |
| `script_list` | `script` | مفعل | Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime. |
| `console_tail` | `script` | مفعل | Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries after sinceId or at least a level. nextSinceId in the result continues from where this call ended. |
| `ui_dump` | `ui` | مفعل | Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the uiautomator-style export. |
| `ui_find` | `ui` | مفعل | Finds the nodes of the active window that match every condition of the selector, optionally waiting up to timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count is not an error; ui_explain_selector tells which condition fails. |
| `ui_current_window` | `ui` | مفعل | Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and the accessibility windows with their type, title, bounds, and focus. |
| `ui_explain_selector` | `ui` | مفعل | Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when ui_find returns nothing. |
| `ui_wait_for` | `ui` | مفعل | Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or dismisses a dialog. |
| `ui_click` | `ui` | مفعل | Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on. Give nodeRef or selector, not both. |
| `ui_long_click` | `ui` | مفعل | Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled). Give nodeRef or selector, not both. |
| `ui_set_text` | `ui` | مفعل | Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is not editable or not enabled. Give nodeRef or selector, not both. |
| `ui_scroll` | `ui` | مفعل | Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. performed counts the steps the node accepted, fewer than requested means it reached the end. Give nodeRef or selector, not both. |
| `ui_press_key` | `ui` | مفعل | Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification shade), quick_settings, power_dialog, or lock_screen (Android 9 or later). |
| `ui_swipe` | `ui_gesture` | معطل | Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump bounds or a screenshot. Part of the ui_gesture group, which is off by default. |
| `ui_gesture` | `ui_gesture` | معطل | Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point is the touch down, the last the lift. Part of the ui_gesture group, which is off by default. |
| `screen_capture` | `screen` | مفعل | Capture the phone screen as an MCP image with dimensions, size, duration and capture source. Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. With annotate the #n references of the latest ui_dump / ui_find are drawn on the image. At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs. |
| `screen_state` | `screen` | مفعل | Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent. |
| `files_list` | `files` | مفعل | Lists workspace files with metadata. Results are bounded and report truncation. |
| `files_stat` | `files` | مفعل | Returns existence, type, size, and modification time of a workspace path. |
| `files_read` | `files` | مفعل | Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit. |
| `files_write` | `files` | مفعل | Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing. |
| `files_mkdir` | `files` | مفعل | Creates a workspace directory and missing parents, then refreshes the host explorer. |
| `files_rename` | `files` | مفعل | Moves a workspace file or directory to another workspace path and refreshes the host explorer. |
| `files_delete` | `files_delete` | معطل | Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted. |
| `editor_open` | `files` | مفعل | Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor. |
| `app_launch` | `device` | مفعل | Opens an installed Android application. Provide exactly one of packageName or appName. |
| `app_list` | `device` | مفعل | Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply. |
| `clipboard_get` | `device` | مفعل | Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background. |
| `clipboard_set` | `device` | مفعل | Replaces clipboard text, including an empty string to clear it. |
| `device_ensure_accessibility` | `device` | مفعل | Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance. |
| `toast` | `device` | مفعل | Shows a short Android toast on the phone. |
| `shell_exec` | `shell` | معطل | Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together. |

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. شغل MCP Server من قائمة AutoJs6. اضغط مطولا على عنوانه لفتح الإعدادات, أو استخدم إعداداته في مركز الإضافات. انسخ إعداد عميل الكمبيوتر.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.
5. أكد طلب الاقتران الأول على الهاتف. أوقف الخادم من القائمة أو الإعدادات أو الإشعار عند الانتهاء.

> مفتاح MCP Server مع إرشادات التثبيت والتنشيط والتفويض والتوافق, ومزامنة الإيقاف من الإشعار وحفظ الإعدادات عند إعادة الاتصال, وفتح الإعدادات بعد فحص الإذن من القائمة ومركز الإضافات. يستعيد الخادم المفعّل عند فتح AutoJs6 إلا إذا أوقفه المستخدم أثناء غياب المضيف, دون بدء عند إقلاع الجهاز.

<p align="center">
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/drawer-en.png?raw=true" alt="مفتاح MCP Server في قائمة AutoJs6" width="300" />
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/settings-en.png?raw=true" alt="صفحة إعدادات MCP Server" width="300" />
</p>

******

### إعداد العميل

******

يسجل Claude Code الخادم بأمر واحد; وتستخدم العملاء الأخرى نفس عنوان URL والترويسة في إعدادات MCP الخاصة بها:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

شغل MCP Server من قائمة AutoJs6. اضغط مطولا على عنوانه لفتح الإعدادات, أو استخدم إعداداته في مركز الإضافات. انسخ إعداد عميل الكمبيوتر. أكد طلب الاقتران الأول على الهاتف. أوقف الخادم من القائمة أو الإعدادات أو الإشعار عند الانتهاء.

******

### مسارات الاتصال

******

USB: يربط الأمر `adb forward tcp:9637 tcp:9637` منفذ الهاتف بالكمبيوتر; مع عدة أجهزة أضف `-s <serial>` (اعرفه عبر `adb devices`), وتعمل المحاكيات بنفس الطريقة. إذا كان المنفذ مشغولا في أي جهة, غيره في صفحة الإعدادات ثم اربط المنفذ الجديد. تقدم بطاقة الاتصال أمر الربط الجاهز للنسخ.

الشبكة المحلية: فعل "السماح باتصالات الشبكة المحلية" في صفحة الإعدادات. تعرض الصفحة بعدها عناوين الهاتف الحالية (تتبع تغيرات Wi-Fi) وتذكرك بأن العميل يجب أن يكون على نفس الشبكة; شبكات الضيوف وعزل نقطة الوصول وجدار حماية الكمبيوتر هي العوائق المعتادة. تُميز طلبات الاقتران القادمة من الشبكة المحلية, ويذكرك إشعار يومي ما دام الخادم متاحا من الشبكة; يمكن إيقاف التذكير. على Android 17 أو أحدث, اسمح بالأجهزة القريبة قبل تفعيل هذا الملحق في مركز ملحقات AutoJs6. يمكنك أيضا إدارة إذن الشبكة المحلية من صفحة إعدادات الملحق. دون الإذن يبقى الملحق معطلا ويتم تخطي التشغيل التلقائي بصمت. يخص الإذن هذا الملحق وهو مستقل عن إذن AutoJs6.

يستخدم المساران نفس الرمز ونفس الاقتران على الهاتف. العملاء الذين لا يملكون نقل HTTP يستخدمون جسر stdio الموصوف في قسم العملاء.

******

### العملاء

******

تنسخ صفحة الإعدادات تكوينا جاهزا مع الرمز الحقيقي لكل عميل أدناه; المقاطع هنا تستخدم `<token>` كعنصر نائب. يتواصل كل عميل عبر Streamable HTTP مع ترويسة Authorization, ويُؤكد أول استدعاء من عميل جديد على الهاتف. تم التحقق: Claude Code و Codex CLI و MCP Inspector و VS Code Copilot Chat و Cursor CLI و Claude Desktop (عبر الجسر); يستخدم بقية العملاء نفس العنوان والترويسة لكن المشرف لم يختبرهم بعد.

Claude Code: نفذ الأمر الموضح في "تكوين العميل" (تنسخه صفحة الإعدادات مع الرمز); بعدها يعرض `claude mcp list` الخادم `autojs6` بحالة Connected.

Cursor: أضف الإدخال أدناه إلى `mcp.json` (تقرأ واجهة سطر الأوامر `cursor-agent` الملف نفسه `~/.cursor/mcp.json`):

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

VS Code (Copilot Chat): نفذ الأمر `MCP: Open User Configuration` وادمج المحتوى أدناه في ملف `mcp.json` الذي يفتحه; المفتاح الأعلى هو `servers` وليس `mcpServers`, لذلك لا يصلح مقطع JSON العام كما هو. يطلب VS Code الرمز عند أول تشغيل للخادم ويحفظه في مخزن الأسرار بدلا من الملف (ينسخه زر "نسخ الرمز" في حوار "VS Code mcp.json" بصفحة الإعدادات):

```json
{
  "inputs": [
    {
      "type": "promptString",
      "id": "autojs6-token",
      "description": "AutoJs6 MCP Server token",
      "password": true
    }
  ],
  "servers": {
    "autojs6": {
      "type": "http",
      "url": "http://127.0.0.1:9637/mcp",
      "headers": {
        "Authorization": "Bearer ${input:autojs6-token}"
      }
    }
  }
}
```

Codex CLI: ضع الرمز في متغير البيئة `AUTOJS6_MCP_TOKEN` (تنسخ صفحة الإعدادات أمر PowerShell لذلك) وأضف الخادم إلى `config.toml`, أو نفذ `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: وضع CLI لا يحتاج إعدادا إضافيا, وواجهة الويب تصل إلى الهاتف عبر وكيل Node الخاص بها. فعل وضع المطور في صفحة الإعدادات فقط عندما تتصل صفحة متصفح بنقطة النهاية مباشرة:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline و Gemini CLI والعملاء المشابهون: استخدم نفس العنوان والترويسة في تكوين MCP الخاص بها; تقدم صفحة الإعدادات مقطع JSON عاما مع `"type": "http"` (المفتاح الأعلى `mcpServers`).

Claude Desktop وغيره من عملاء stdio فقط: ثبت الجسر بالأمر `npm install -g autojs6-mcp-bridge`, وسجل `autojs6-mcp-bridge --serial <serial>` كخادم stdio مع وضع `AUTOJS6_MCP_TOKEN` في كتلة متغيرات البيئة (انظر [README الجسر](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) لمقاطع Claude Desktop و Claude Code). الجسر 0.1.1 (يتطلب Node.js 22 أو أحدث) يعمل مع الإضافة 1.0.1 وما بعدها ويمرر إصدار بروتوكول العميل كما هو ويفتح جلسة جديدة بنفسه عندما يعاد تشغيل المستمع على الهاتف (مع 0.1.0 لـ Node.js 18 أو أحدث كان يلزم حينها إعادة تشغيل الخادم في العميل); تم التحقق منه مع Claude Code 2.1.257 و Claude Desktop 2.9939 عبر stdio. على Windows يكون تشغيل `autojs6-mcp-bridge/dist/cli.js` المثبت عموميا عبر `node` أكثر موثوقية من تشغيله عبر `npx`.

******

### الأسئلة الشائعة

******

- 401 Unauthorized: الرمز مفقود أو مكتوب خطأ أو تم تدويره. انسخ التكوين مجددا من صفحة الإعدادات; بعد تدوير الرمز يحتاج كل عميل إلى القيمة الجديدة.
- يعرض VS Code طلب OAuth أو تسجيل عميل: عند خطأ الرمز يجرب VS Code أولا اكتشاف OAuth والتسجيل الديناميكي للعميل, بينما لا تقبل الإضافة إلا رمزها الثابت. ألغ الطلب, وامسح قيمة الإدخال التي حفظها VS Code, ثم أدخل الرمز مجددا وأعد تشغيل الخادم.
- انتهاء مهلة الاقتران: ينتظر أول استدعاء من عميل جديد نحو دقيقة حتى الضغط على السماح في الهاتف. افتح قفل الهاتف, واقبل مربع الحوار أو إجراء الإشعار, ثم كرر الاستدعاء. الرفض يبدأ فترة تهدئة قصيرة يسأل بعدها الاستدعاء التالي مجددا.
- HOST_UNAVAILABLE: AutoJs6 غير مشغل أو جلسة الإضافة مغلقة. افتح AutoJs6, وأبق مفتاح الدرج مفعلا, وتحقق من حالة الاتصال في صفحة الإعدادات.
- رفض الاتصال (`ECONNREFUSED`): مفتاح الدرج مغلق أو أن `adb forward` على الحاسوب لم يعد موجودا. إعادة تشغيل خادم adb (عند إعادة توصيل الجهاز أو عندما تعيد أداة أخرى تشغيل adb) تمسح كل التحويلات; نفذ `adb -s <serial> forward tcp:9637 tcp:9637` مجددا, أو دع الجسر ينشئ التحويل عبر `--serial` (يزيله عند الخروج).
- إمكانية الوصول معطلة: تحتاج أدوات `ui_*` والتقاط الشاشة إلى خدمة إمكانية الوصول في AutoJs6. استدع `device_ensure_accessibility` أو فعل الخدمة من إعدادات إمكانية الوصول في النظام.
- المنفذ مشغول: يبلغ الدرج عن `port_in_use`. غير المنفذ في صفحة الإعدادات واربط المنفذ الجديد عبر adb.
- تعذر الوصول عبر الشبكة المحلية: فعل الوصول من الشبكة المحلية, واستخدم عنوانا مدرجا في صفحة الإعدادات, وأبق الكمبيوتر والهاتف على نفس الشبكة دون عزل الضيوف, واسمح بالمنفذ في جدار حماية الكمبيوتر. توفير طاقة Wi-Fi في الهاتف يضيف بضع مئات من المللي ثانية لكل استدعاء.
- اختفى الخادم بعد إطفاء الشاشة: الهواتف التي تقيد استخدام البطارية للتطبيق (HyperOS و MIUI يفعلان ذلك افتراضيا للتطبيقات المثبتة يدويا) توقف الخدمة الأمامية بعد نحو دقيقة من إطفاء الشاشة على البطارية. عندها تعرض صفحة الإعدادات تحذيرا مع زر "إعدادات البطارية", فاختر هناك "بدون قيود" لـ MCP Server. كما أن خيار "الإيقاف التلقائي عند الخمول" (معطل افتراضيا) يوقف الخادم بعد الدقائق المختارة بدون طلبات ويترك إشعارا يوضح ذلك.
- ترفض Cursor CLI استدعاءات الأدوات: لا يشغل `cursor-agent` إلا الأدوات الموجودة في قائمة السماح, لذا في وضع الطباعة (`-p`) تُرفض أداة MCP غير المسموح بها محليا; لا يصل الطلب إلى الهاتف ولا يظهر طلب الاقتران. أضف `Mcp(autojs6:<اسم الأداة>)` لكل أداة إلى `permissions.allow` في `~/.cursor/cli-config.json`, مثل `Mcp(autojs6:device_info)`.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder وصفحة الإعدادات محمية بصلاحية التوقيع `org.autojs.permission.PLUGIN`, لذا لا يمكن إلا لـ AutoJs6 الوصول إليها; ومربع حوار الاقتران ومستقبله وصفحة سجل الإصدارات غير مصدرة. الخدمة الأمامية التي تستضيف المستمع هي الوحيدة التي تقبل adb (`android.permission.DUMP`), وهي مفتاح التشغيل / الإيقاف الخاص بالمطور.
- تستخدم صلاحية INTERNET فقط لمستمع HTTP الخاص بالمكون الإضافي; ولا يرسل المكون الإضافي أي طلبات صادرة ولا يجمع أي بيانات. لا يسمح بـ HTTP غير المشفر إلا نحو عناوين الحلقة المحلية عبر تكوين أمان الشبكة.
- يستمع الخادم افتراضيا إلى 127.0.0.1 فقط. يبقى الوصول من الشبكة المحلية معطلا حتى تفعله; وبعد التفعيل يبقى الرمز وتأكيد الاقتران وقائمة Host المسموح بها وحدود المعدل سارية على الشبكة المحلية, ويذكرك إشعار يومي ما دام مفعلا.
- على Android 17 أو أحدث, اسمح بالأجهزة القريبة قبل تفعيل هذا الملحق في مركز ملحقات AutoJs6. يمكنك أيضا إدارة إذن الشبكة المحلية من صفحة إعدادات الملحق. دون الإذن يبقى الملحق معطلا ويتم تخطي التشغيل التلقائي بصمت. يخص الإذن هذا الملحق وهو مستقل عن إذن AutoJs6.
- يأتي رمز الوصول من مصدر عشوائي آمن, ويغلف بمفتاح AES-GCM من Android Keystore ويحفظ في التخزين الخاص بالمكون الإضافي الذي لا ينسخ احتياطيا أبدا; والنسخ الاحتياطي ونقل الجهاز معطلان. تعرض صفحة الإعدادات آخر 4 أحرف فقط, وتمنع مربعات حوار الرمز الكامل لقطات الشاشة, وتوسم النسخ كحساسة في الحافظة.
- لا تحتوي السجلات أبدا على الرمز أو نصوص الطلبات أو محتويات الملفات أو لقطات الشاشة; يسجل المكون الإضافي أسماء الأدوات وأسماء العملاء وبصمات الرمز فقط. تم التحقق من ذلك عبر logcat على جهازين أثناء استدعاءات فعلية للملفات ولقطات الشاشة (docs/dev/p6-security-audit.md).
- تمر استدعاءات الأدوات عبر وسيط قدرات AutoJs6 ولا تتجاوز أبدا ما يسمح به للمضيف نفسه; وتبقى أوامر shell وحذف الملفات والإيماءات معطلة حتى تفعل مجموعاتها, ويحتاج shell بصلاحيات root إضافة إلى ذلك إلى مفتاح خاص وإذن من المضيف.
- يمكن إلغاء الاقترانات واحدا واحدا أو دفعة واحدة من صفحة الإعدادات; ويجب تأكيد العميل الملغى اقترانه على الهاتف مجددا قبل استدعاء الأداة التالي. تدوير الرمز يحتفظ بالاقترانات لكنه يقطع كل عميل ما زال يستخدم الرمز القديم.

احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون الإضافي والتفاوض على التوافق:

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

ينفذ `McpServerPluginService` عقد mcp-server-api للمضيف `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` في العملية `:mcp_server` ويستجيب إلى `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). يستجيب `McpServerPluginInfoService` إلى `org.autojs.plugin.INFO` باستخدام PluginInfo. يسمح `WakeActivity` للمضيف بتنشيط الإضافة.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.4

_2026/10/11_

- `إصلاح` شريط الحالة في صفحتي الإعدادات وسجل الإصدارات لم يكن يأخذ لون السمة في Android 15 وما بعده (كان شريط الأدوات فقط يأخذه)

#### v1.0.3

_2026/10/11_

- `ميزة` مقطع VS Code mcp.json في تكوين العملاء بصفحة الإعدادات: يطلب VS Code الرمز عند أول تشغيل للخادم فلا يُكتب الرمز في الملف (يوفر الحوار زر "نسخ الرمز")
- `ميزة` يمكن لمقتطفات إعدادات العملاء ونافذة الرمز في صفحة الإعدادات عرض رمز QR يحمل النص نفسه الذي ينسخه زر "نسخ" (مع الرمز المميز عندما يتضمنه النص, والنافذة تمنع لقطات الشاشة)
- `ميزة` خيار annotate في screen_capture لرسم مراجع #n الخاصة بآخر ui_dump / ui_find على الصورة (إطارات وتسميات), ويذكر الحقل annotation في النتيجة عدد ما رسم
- `تحسين` دعم استئناف تدفقات الأحداث في نقطة نهاية MCP ذات الحالة: تحمل الأحداث معرفات, ويتلقى عملاء البروتوكول 2025-11-25 أو أحدث أولا حدث تهيئة مع فاصل إعادة الاتصال, ويمكن استئناف تدفق طلب منقطع بطلب GET يحمل Last-Event-ID يعيد إرسال الأحداث الفائتة والنتيجة النهائية (احتفاظ محدود لكل جلسة)
- `تحسين` تعلن device_info و screen_state و script_run و script_run_file مخطط اخراج (outputSchema) يمكن للعملاء التحقق به من نتائجها المنظمة; وتحمل اخطاء هذه الادوات رمز الخطأ والرسالة نصا فقط
- `تحسين` تستخدم أيقونات مركز الملحقات الأحجام والمواضع والصور الفاتحة والداكنة والخلفيات الدائرية المعدلة في Icon Studio مع الاحتفاظ بالمصادر والمعلمات لإعادة إنتاجها
- `تحسين` تستخدم أيقونات معلومات التطبيق في Android صور Icon Studio وخلفياته الفاتحة والداكنة مع الحفاظ على الصور الشفافة في مركز الملحقات وخيارات المشغل الحالية
- `تبعية` إضافة ZXing Core 3.5.4 (`com.google.zxing:core`) لرسم رموز QR في صفحة الإعدادات (المرمز فقط, Apache 2.0)

#### v1.0.2

_2026/09/19_

- `إصلاح` تتبع الأزرار ومسارات ومقابض المفاتيح وحقول الإدخال وخيارات التحديد وأزرار الحوارات لون سمة المضيف مع تحسين التباين في الوضعين الفاتح والداكن
- `إصلاح` تحذيرات قراءة SDK XML v4 مع AGP 9.1 وتشغيل فحص محاذاة مكتبات APK الأصلية خطأ عند تجميع اختبارات JVM, باستخدام إضافات البناء المشتركة 1.8.3
- `تحسين` نقل إذن الشبكة المحلية على Android 17 إلى مسار التفعيل وإعدادات الملحق دون صفحة إذن في المشغل; يبقى الملحق معطلا دون الإذن ويتم تخطي التشغيل التلقائي بصمت
- `تحسين` استهداف Android 17 (SDK 37) مع تحكم مستقل بإذن الشبكة المحلية للملحق وإرشادات استعادة الوصول

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يستهدف هذا القسم المطورين الراغبين في بناء المكون الإضافي من المصدر; ويمكن للمستخدمين العاديين ببساطة تثبيت ملف APK الجاهز من صفحة Releases.

بناء APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وبناء APK اختبارات الأجهزة:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء APK الإصدار:

```powershell
.\gradlew.bat :app:assembleRelease
```

جمع ناتج الإصدار وإلحاق الإصدار وملخص CRC32 باسم الملف:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من تزامن مصادر التوثيق متعدد اللغات مع النواتج المولدة (يفرض ذلك CI أيضا):

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث و Android SDK 37; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

******

### التعريب وتوليد التوثيق

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

ملفات JSON اللغوية في `.readme/` و `.changelog/` هي المصدر الوحيد لملف README وتعليمات مركز المكونات الإضافية وسجل التغييرات. عدل دائما مصادر JSON هذه وأعد تشغيل `py .python/generate_markdown.py`; ولا تحرر يدويا نواتج README و `plugin_instruction.md` وسجل التغييرات المولدة أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من جميع النواتج المولدة.

******

### الترخيص

******

كود المشروع مرخص بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE). المكونات الخارجية وتراخيصها مدرجة في [إشعارات الجهات الخارجية](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- توثيق AutoJs6: https://docs.autojs6.com
- مواصفة Model Context Protocol: https://modelcontextprotocol.io
- إشعارات الجهات الخارجية: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
