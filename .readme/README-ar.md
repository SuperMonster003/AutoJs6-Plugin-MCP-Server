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

معاينة التطوير P3.4: تتوفر 37 أداة, منها 33 مفعلة افتراضيا. تشمل الملفات وتحديد موضع المحرر واستعلام التطبيقات والحافظة والتفعيل التلقائي لإمكانية الوصول و Shell مع حدود للمخرجات. مفتاح القائمة وصفحة الإعدادات مقرران في P4. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### الميزات المخطط لها

******

تقدم خارطة الطريق القدرات التالية على مراحل:

- تنفيذ البرامج النصية: تشغيل JavaScript من نص أو من ملف داخل AutoJs6, وعرض المحركات وإيقافها, وقراءة مخرجات وحدة التحكم الأخيرة.
- واجهة إمكانية الوصول: تفريغ شجرة العقد بتنسيق نصي مضغوط, والعثور على العقد بصيغة محددات AutoJs6, والنقر, والضغط المطول, والتمرير, وتعيين النص, والضغط على المفاتيح العامة مثل رجوع والشاشة الرئيسية.
- مجموعة لقطات الشاشة (P3.3): تعيد screen_capture صور MCP مع القص وscale أو maxWidth وتنسيقات JPEG / PNG / WebP والتحكم في الجودة. الإعداد الافتراضي JPEG بجودة 70 وضلع أطول 1280 px. عند تجاوز base64 حجم 4 MiB تنخفض الجودة أو الأبعاد مع بيان التعديلات في البيانات الوصفية. تعرض screen_state حالة الشاشة والأبعاد والاتجاه والكثافة. يضم الدليل 37 أداة. يتطلب البديل MediaProjection إصدار AutoJs6 مبنيا في 2026-09-13 أو بعده وموافقة على الهاتف تعيد جلسة المضيف استخدامها.
- أدوات مجلد العمل (P3.4): files_list / stat / read / write / mkdir / rename / delete, و editor_open بسطر وعمود يبدأ ترقيمهما من 1, و app_launch / list, و clipboard_get / set, و device_ensure_accessibility, و toast, و shell_exec. القراءة الثنائية تعيد base64 بحد أقصى 1 MiB للبيانات الأصلية. تخضع الكتابة أيضا لميزانية طلب المضيف (عادة 96 KiB بما فيها ترميز JSON). الحذف و Shell معطلان افتراضيا; يتطلب root أيضا allowShellRoot ومنح shell.root من المضيف. يلزم بناء المضيف المطابق لـ P3.4.
- مسارات الاتصال: USB عبر `adb forward`, والشبكة المحلية بتفعيل صريح, وجسر stdio على جانب الحاسوب, ونفق عام اختياري مع OAuth 2.1.
- الأمان: رمز bearer قابل للتدوير, وتأكيد الاقتران على الهاتف عند أول استخدام, ومفاتيح تبديل للأدوات حسب المجموعة; ولا يستمع الخادم افتراضيا إلا على واجهة الاسترجاع المحلي.

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. لهذه المعاينة استخدم تحكم adb وجلسة اختبار المضيف الموضحين في ملاحظات التطوير; مفتاح القائمة وإعدادات الملحق مخططان في P4.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.

> لهذه المعاينة استخدم تحكم adb وجلسة اختبار المضيف الموضحين في ملاحظات التطوير; مفتاح القائمة وإعدادات الملحق مخططان في P4.

******

### إعداد العميل

******

يسجل Claude Code الخادم بأمر واحد; وتستخدم العملاء الأخرى نفس عنوان URL والترويسة في إعدادات MCP الخاصة بها:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

في هذه النسخة التجريبية, اقرأ الرمز عبر تحكم adb في وضع المطور كما هو موضح في ملاحظات التطوير. عرض الرمز في صفحة الإعدادات مخطط له في P4.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بصلاحية التوقيع `org.autojs.permission.PLUGIN`, لذا لا يمكن إلا لـ AutoJs6 الارتباط بها.
- تستخدم صلاحية INTERNET فقط لمستمع HTTP الخاص بالمكون الإضافي; ولا يرسل المكون الإضافي أي طلبات صادرة ولا يجمع أي بيانات.
- تمر استدعاءات الأدوات عبر وسيط قدرات AutoJs6 ولا تتجاوز أبدا ما يسمح به للمضيف نفسه; وتبقى المجموعات الخطرة مثل أوامر shell وحذف الملفات معطلة حتى يفعلها المستخدم.
- النسخ الاحتياطي معطل, وتخزن الرموز فقط في التخزين الخاص بالمكون الإضافي.

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

#### v1.0.0

_2026/09/13_

- `تلميح` معاينة التطوير P3.4: تتوفر 37 أداة, منها 33 مفعلة افتراضيا. تشمل الملفات وتحديد موضع المحرر واستعلام التطبيقات والحافظة والتفعيل التلقائي لإمكانية الوصول و Shell مع حدود للمخرجات. مفتاح القائمة وصفحة الإعدادات مقرران في P4. ROADMAP.md.
- `ميزة` أدوات مجلد العمل (P3.4): files_list / stat / read / write / mkdir / rename / delete, و editor_open بسطر وعمود يبدأ ترقيمهما من 1, و app_launch / list, و clipboard_get / set, و device_ensure_accessibility, و toast, و shell_exec. القراءة الثنائية تعيد base64 بحد أقصى 1 MiB للبيانات الأصلية. تخضع الكتابة أيضا لميزانية طلب المضيف (عادة 96 KiB بما فيها ترميز JSON). الحذف و Shell معطلان افتراضيا; يتطلب root أيضا allowShellRoot ومنح shell.root من المضيف. يلزم بناء المضيف المطابق لـ P3.4.
- `ميزة` هوية المكون الإضافي `mcp-server` مع خدمة INFO و Wake Activity وهيكل خدمة `org.autojs.plugin.MCP_SERVER` لاكتشاف المضيف
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بـ 10 لغات
- `ميزة` نقطة نهاية Streamable HTTP على `http://127.0.0.1:9637/mcp` مع الأداة `device_ping`, تستضيفها خدمة أمامية يمكن تشغيلها وإيقافها عبر adb أو من المضيف (معاينة تطويرية)
- `ميزة` تقوية النقل لنقطة النهاية `/mcp`: يأتي عنوان الربط والمنفذ من مخزن إعدادات الخادم, ويقتصر حجم جسم الطلب على 1 MiB, وتغلق الاتصالات الخاملة بعد 60 ثانية, وينتهي المنفذ المشغول أو الربط المرفوض بحالة `port_in_use` / `bind_failed` مع تلميح بدلا من الانهيار
- `ميزة` حماية من DNS rebinding أمام نقل SDK: يقبل وضع loopback فقط `localhost` / `127.0.0.1` / `[::1]` كقيمة `Host`, ويضيف وضع LAN عناوين IPv4 الحالية للجهاز وأسماء مضيفين إضافية اختيارية ويحدثها عند تغير الشبكة; ترفض أصول المتصفح ما لم يسمح مفتاح "وضع المطور" بأصل loopback الخاص بـ Inspector عبر CORS
- `ميزة` هوية الخادم `autojs6-mcp-server` مع إصدار الإضافة وقدرات tools (`listChanged`) و resources و prompts; يحافظ `tools/list` على ترتيب التسجيل ليتمكن العملاء من تخزينه مؤقتا
- `ميزة` مصادقة برمز Bearer لكل طلب `/mcp`: رمز من 32 بايت ينشأ عند أول تشغيل, ويغلف بمفتاح AES-GCM من Android Keystore ويحفظ في التخزين الخاص بالإضافة الذي لا يدخل في النسخ الاحتياطي; ترفض ترويسة `Authorization` المفقودة أو الخاطئة بعد مقارنة بزمن ثابت بالرد `401` + `WWW-Authenticate: Bearer` وخطأ JSON-RPC `-32001`; لا يصل الرمز إلى السجل أبدا
- `ميزة` اقتران عند أول استخدام أمام النقل: يمكن للعميل غير المقترن تنفيذ `initialize` وسرد tools و resources و prompts, لكن أول `tools/call` أو `resources/read` أو `resources/subscribe` أو `prompts/get` يرد بـ `PAIRING_REQUIRED` (`-32002`) حتى يؤكد الطلب على الهاتف خلال 60 ثانية; الرفض أو انتهاء المهلة يرد بـ `PAIRING_DENIED` (`-32003`) لمدة 30 ثانية; يعرف العميل باسم `clientInfo` (أو `User-Agent`) مع فئة العنوان (loopback / LAN), لذا يحافظ تدوير الرمز على الاقترانات القائمة, ويمكن اقتران ما يصل إلى 32 عميلا
- `ميزة` تأكيد الاقتران على الهاتف عبر قناتين: إشعار عالي الأولوية بإجراءي السماح / الرفض, إضافة إلى مربع حوار ما دامت الشاشة غير مقفلة; تحفظ إعدادات الخادم والرمز والعملاء المقترنون في ملفات تستبدل ذريا يتشاركها عملية الخادم وصفحة الإعدادات دون ذاكرة مؤقتة قديمة
- `ميزة` فهرس أدوات بمفاتيح مجموعات (القرار D6): `device_ping` (محلي في الإضافة) و `device_info` (`device.info` في AutoJs6) و `script_run` (`engines.execScript` في AutoJs6: ينفذ JavaScript وينتظر انتهاءه حتى `timeoutMs` ثم يعيد النتيجة مع أحدث سطور وحدة التحكم, ويرسل إشعارات تقدم أثناء التنفيذ); تعلن كل أداة عن JSON Schema مغلق (`additionalProperties: false`) وتتحقق من وسائطها قبل أن يصل أي شيء إلى AutoJs6; تحفظ مفاتيح المجموعات `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` في `tool_groups.json`, وتختفي المجموعة الموقوفة من `tools/list` ابتداء من الطلب التالي وترد أدواتها بـ `TOOL_DISABLED`
- `ميزة` جسر المضيف: تنفذ خدمة `org.autojs.plugin.MCP_SERVER` واجهة Binder الحقيقية `IMcpServerPlugin` (يبلغ `getInfo` / `getCapabilities` عن إصدار العقد 1 ومجموعات الأدوات وإصدارات بروتوكول MCP وإصدار SDK; ولا يقبل `openServer` إلا AutoJs6 المثبت بالتوقيع نفسه ويعيد `IMcpServerSession` مع `getStatus` / `updateConfig` / `stop` / `close`); تمر استدعاءات الأدوات عبر وسيط قدرات المضيف بمعرفات طلب متزايدة ومهلة لكل استدعاء وحد أقصى 4 استدعاءات متزامنة, وتربط فئات أخطاء المضيف بـ `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; عند توقف AutoJs6 يستمر المستمع في العمل وترد الأدوات المعتمدة على المضيف بـ `HOST_UNAVAILABLE` حتى يعود المضيف للاتصال; تصل الحالة والأحداث (`pairing_requested` و `client_paired` و `tool_call` و `warning`) إلى المضيف عبر رد النداء الخاص به
- `ميزة` يعرض إشعار خدمة المقدمة نقطة النهاية وحالة اتصال AutoJs6 وعدد العملاء المقترنين مع إجراء إيقاف; وعند حظر الإشعارات تظهر رسالة toast بنقطة النهاية; ويطبع `dumpsys activity service` إضافة إلى ذلك جلسة المضيف ومفاتيح المجموعات والأدوات المسجلة
- `ميزة` اكتمال مجموعة السكربتات: `script_run_file` يشغل ملف سكربت على الجهاز, و `script_stop` / `script_stop_all` يوقفان تنفيذا واحدا أو كل تنفيذات AutoJs6, و `script_list` يسرد التنفيذات الجارية, و `console_tail` يعيد أحدث سطور وحدة التحكم مع مؤشر `nextSinceId` ومرشح مستوى; يعيد `script_run` و `script_run_file` الآن `executionId` و `status` (`finished` / `error` / `running`) و `durationMs` والاستثناء مع رقم سطره وأحدث سطور وحدة التحكم, وأثناء الانتظار يرسلان كل 2 ث إشعار تقدم يحمل أحدث سطر في وحدة التحكم
- `ميزة` ردود نقطة نهاية MCP تبث الآن كأحداث مرسلة من الخادم (لا يستخدم وضع استجابة JSON في SDK), وبذلك يصل الإشعار المرتبط بطلب, مثل نبض التقدم لسكربت قيد التشغيل, إلى العميل ضمن استجابة ذلك الطلب
- `ميزة` إضافة مجموعة واجهة المستخدم (roadmap P3.2): يعيد `ui_dump` النافذة النشطة كشجرة عقد مضغوطة مع مراجع `#n` (`format` هو text / json / xml, و `maxNodes` حتى 400, و `maxDepth`, و `visibleOnly`, و `window`), ويستطلع `ui_find` / `ui_wait_for` محددا, ويبلغ `ui_current_window` و `ui_explain_selector` عن النافذة وسبب فشل المحدد, ويعمل `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` على `nodeRef` (يعاد تحديد موقعه ببصمته, و `NODE_REF_STALE` إذا اختفى) أو `selector`, ويضغط `ui_press_key` على back / home / recents / notifications / quick_settings / power_dialog / lock_screen, وتضيف مجموعة `ui_gesture` (معطلة افتراضيا) `ui_swipe` و `ui_gesture` والشكل الإحداثي لأدوات النقر (`TOOL_DISABLED` ما دامت المجموعة معطلة); تزداد لقطة كتالوج الأدوات إلى 20 أداة; تحتاج إيماءات الإحداثيات إلى مضيف AutoJs6 مبني بتاريخ 2026-09-11 أو بعده (يجيب المضيف الأقدم عشوائيا بـ "the system cancelled ...")
- `ميزة` مجموعة لقطات الشاشة (P3.3): تعيد screen_capture صور MCP مع القص وscale أو maxWidth وتنسيقات JPEG / PNG / WebP والتحكم في الجودة. الإعداد الافتراضي JPEG بجودة 70 وضلع أطول 1280 px. عند تجاوز base64 حجم 4 MiB تنخفض الجودة أو الأبعاد مع بيان التعديلات في البيانات الوصفية. تعرض screen_state حالة الشاشة والأبعاد والاتجاه والكثافة. يضم الدليل 22 أداة. يتطلب البديل MediaProjection إصدار AutoJs6 مبنيا في 2026-09-13 أو بعده وموافقة على الهاتف تعيد جلسة المضيف استخدامها.
- `تحسين` التحقق أثناء البناء لمنع إدخال تبعيات أصلية غير مقصودة, مع تقرير JSON
- `تبعية` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) على محرك Ktor 3.5.1 CIO
- `تبعية` إضافة Ktor 3.5.1 `ktor-server-test-host` لاختبارات النقل على JVM (نطاق الاختبار فقط)
- `تبعية` أضيف `mcp-server-api.aar` (وحدة AutoJs6 `plugin-api/mcp-server-api`, بناء المضيف 6.8.0 / 5279, MPL 2.0) عقدا لـ Binder بين AutoJs6 والإضافة, مع تثبيت التجزئة في `locks/host-api-aars.lock`

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

يتطلب البناء JDK 21 أو أحدث و Android SDK 36; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

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
