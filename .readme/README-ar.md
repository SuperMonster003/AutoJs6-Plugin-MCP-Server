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

معاينة P4: 37 أداة, منها 33 مفعلة افتراضيا, مع مفتاح في قائمة AutoJs6 وصفحة إعدادات للإضافة. تتطلب نسخة AutoJs6 المتوافقة مع P4. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### الميزات المخطط لها

******

تقدم خارطة الطريق القدرات التالية على مراحل:

- إعدادات الهاتف لحالة الخادم وUSB والمنفذ والشبكة المحلية وعرض الرمز ونسخه وتغييره وإلغاء الاقتران ومجموعات الأدوات وصلاحيات root ووضع المطور وإعدادات Claude Code / Cursor / Codex / HTTP القابلة للنسخ وسجل الإصدارات مع مظهر AutoJs6. تغييرات الشبكة تعيد تشغيل المستمع, وتطبق الرموز والأذونات فورا. نوافذ الأسرار تمنع لقطات الشاشة.
- تنفيذ البرامج النصية: تشغيل JavaScript من نص أو من ملف داخل AutoJs6, وعرض المحركات وإيقافها, وقراءة مخرجات وحدة التحكم الأخيرة.
- واجهة إمكانية الوصول: تفريغ شجرة العقد بتنسيق نصي مضغوط, والعثور على العقد بصيغة محددات AutoJs6, والنقر, والضغط المطول, والتمرير, وتعيين النص, والضغط على المفاتيح العامة مثل رجوع والشاشة الرئيسية.
- مجموعة لقطات الشاشة (P3.3): تعيد screen_capture صور MCP مع القص وscale أو maxWidth وتنسيقات JPEG / PNG / WebP والتحكم في الجودة. الإعداد الافتراضي JPEG بجودة 70 وضلع أطول 1280 px. عند تجاوز base64 حجم 4 MiB تنخفض الجودة أو الأبعاد مع بيان التعديلات في البيانات الوصفية. تعرض screen_state حالة الشاشة والأبعاد والاتجاه والكثافة. يضم الدليل 37 أداة. يتطلب البديل MediaProjection إصدار AutoJs6 مبنيا في 2026-09-13 أو بعده وموافقة على الهاتف تعيد جلسة المضيف استخدامها.
- أدوات مجلد العمل (P3.4): files_list / stat / read / write / mkdir / rename / delete, و editor_open بسطر وعمود يبدأ ترقيمهما من 1, و app_launch / list, و clipboard_get / set, و device_ensure_accessibility, و toast, و shell_exec. القراءة الثنائية تعيد base64 بحد أقصى 1 MiB للبيانات الأصلية. تخضع الكتابة أيضا لميزانية طلب المضيف (عادة 96 KiB بما فيها ترميز JSON). الحذف و Shell معطلان افتراضيا; يتطلب root أيضا allowShellRoot ومنح shell.root من المضيف. يلزم بناء المضيف المطابق لـ P3.4.
- توفر موارد MCP (P3.5) ملفات العمل للقراءة فقط, وتصفح أمثلة المضيف, والتوثيق دون اتصال عند تثبيت مكون AutoJs6 Offline Docs الإضافي, ومعلومات الجهاز, ومخرجات وحدة التحكم الأخيرة, مع احترام الاقتران وإعدادات المجموعات. تتضمن قراءة النص والبيانات الثنائية معلومات الاقتطاع. توفر القوالب write_autojs6_script و automate_task و debug_selector إرشادات بالإنجليزية والصينية, مع استخدام الإنجليزية للغات الهاتف الأخرى.
- مسارات الاتصال: USB عبر `adb forward`, والشبكة المحلية بتفعيل صريح, وجسر stdio على جانب الحاسوب, ونفق عام اختياري مع OAuth 2.1.
- الأمان: رمز bearer قابل للتدوير, وتأكيد الاقتران على الهاتف عند أول استخدام, ومفاتيح تبديل للأدوات حسب المجموعة; ولا يستمع الخادم افتراضيا إلا على واجهة الاسترجاع المحلي.

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. شغل MCP Server من قائمة AutoJs6. اضغط مطولا على عنوانه لفتح الإعدادات, أو استخدم إعداداته في مركز الإضافات. انسخ إعداد عميل الكمبيوتر.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.
5. أكد طلب الاقتران الأول على الهاتف. أوقف الخادم من القائمة أو الإعدادات أو الإشعار عند الانتهاء.

> مفتاح MCP Server مع إرشادات التثبيت والتنشيط والتفويض والتوافق, ومزامنة الإيقاف من الإشعار وحفظ الإعدادات عند إعادة الاتصال, وفتح الإعدادات بعد فحص الإذن من القائمة ومركز الإضافات. يستعيد الخادم المفعّل عند فتح AutoJs6 إلا إذا أوقفه المستخدم أثناء غياب المضيف, دون بدء عند إقلاع الجهاز.

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

الشبكة المحلية: فعل "السماح باتصالات الشبكة المحلية" في صفحة الإعدادات. تعرض الصفحة بعدها عناوين الهاتف الحالية (تتبع تغيرات Wi-Fi) وتذكرك بأن العميل يجب أن يكون على نفس الشبكة; شبكات الضيوف وعزل نقطة الوصول وجدار حماية الكمبيوتر هي العوائق المعتادة. تُميز طلبات الاقتران القادمة من الشبكة المحلية, ويذكرك إشعار يومي ما دام الخادم متاحا من الشبكة; يمكن إيقاف التذكير.

يستخدم المساران نفس الرمز ونفس الاقتران على الهاتف. العملاء الذين لا يملكون نقل HTTP يستخدمون جسر stdio الموصوف في قسم العملاء.

******

### العملاء

******

تنسخ صفحة الإعدادات تكوينا جاهزا مع الرمز الحقيقي لكل عميل أدناه; المقاطع هنا تستخدم `<token>` كعنصر نائب. يتواصل كل عميل عبر Streamable HTTP مع ترويسة Authorization, ويُؤكد أول استدعاء من عميل جديد على الهاتف. تم التحقق: Claude Code و Codex CLI و MCP Inspector; يستخدم بقية العملاء نفس العنوان والترويسة لكن المشرف لم يختبرهم بعد.

Claude Code: نفذ الأمر الموضح في "تكوين العميل" (تنسخه صفحة الإعدادات مع الرمز); بعدها يعرض `claude mcp list` الخادم `autojs6` بحالة Connected.

Cursor: أضف الإدخال أدناه إلى `mcp.json`:

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

Cline و VS Code Copilot Chat و Gemini CLI والعملاء المشابهون: استخدم نفس العنوان والترويسة في تكوين MCP الخاص بها; تقدم صفحة الإعدادات مقطع JSON عاما مع `"type": "http"`.

Claude Desktop وغيره من عملاء stdio فقط: ثبت الجسر بالأمر `npm install -g autojs6-mcp-bridge`, وسجل `autojs6-mcp-bridge --serial <serial>` كخادم stdio مع وضع `AUTOJS6_MCP_TOKEN` في كتلة متغيرات البيئة (انظر [README الجسر](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) لمقاطع Claude Desktop و Claude Code). الجسر 0.1.0 يعمل مع الإضافة 1.0.0 ويمرر إصدار بروتوكول العميل كما هو; تم التحقق منه مع Claude Code 2.1.257 عبر stdio.

******

### الأسئلة الشائعة

******

- 401 Unauthorized: الرمز مفقود أو مكتوب خطأ أو تم تدويره. انسخ التكوين مجددا من صفحة الإعدادات; بعد تدوير الرمز يحتاج كل عميل إلى القيمة الجديدة.
- انتهاء مهلة الاقتران: ينتظر أول استدعاء من عميل جديد نحو دقيقة حتى الضغط على السماح في الهاتف. افتح قفل الهاتف, واقبل مربع الحوار أو إجراء الإشعار, ثم كرر الاستدعاء. الرفض يبدأ فترة تهدئة قصيرة يسأل بعدها الاستدعاء التالي مجددا.
- HOST_UNAVAILABLE: AutoJs6 غير مشغل أو جلسة الإضافة مغلقة. افتح AutoJs6, وأبق مفتاح الدرج مفعلا, وتحقق من حالة الاتصال في صفحة الإعدادات.
- إمكانية الوصول معطلة: تحتاج أدوات `ui_*` والتقاط الشاشة إلى خدمة إمكانية الوصول في AutoJs6. استدع `device_ensure_accessibility` أو فعل الخدمة من إعدادات إمكانية الوصول في النظام.
- المنفذ مشغول: يبلغ الدرج عن `port_in_use`. غير المنفذ في صفحة الإعدادات واربط المنفذ الجديد عبر adb.
- تعذر الوصول عبر الشبكة المحلية: فعل الوصول من الشبكة المحلية, واستخدم عنوانا مدرجا في صفحة الإعدادات, وأبق الكمبيوتر والهاتف على نفس الشبكة دون عزل الضيوف, واسمح بالمنفذ في جدار حماية الكمبيوتر. توفير طاقة Wi-Fi في الهاتف يضيف بضع مئات من المللي ثانية لكل استدعاء.
- اختفى الخادم بعد إطفاء الشاشة: الهواتف التي تقيد استخدام البطارية للتطبيق (HyperOS و MIUI يفعلان ذلك افتراضيا للتطبيقات المثبتة يدويا) توقف الخدمة الأمامية بعد نحو دقيقة من إطفاء الشاشة على البطارية. عندها تعرض صفحة الإعدادات تحذيرا مع زر "إعدادات البطارية", فاختر هناك "بدون قيود" لـ MCP Server. كما أن خيار "الإيقاف التلقائي عند الخمول" (معطل افتراضيا) يوقف الخادم بعد الدقائق المختارة بدون طلبات ويترك إشعارا يوضح ذلك.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder وصفحة الإعدادات محمية بصلاحية التوقيع `org.autojs.permission.PLUGIN`, لذا لا يمكن إلا لـ AutoJs6 الوصول إليها; ومربع حوار الاقتران ومستقبله وصفحة سجل الإصدارات غير مصدرة. الخدمة الأمامية التي تستضيف المستمع هي الوحيدة التي تقبل adb (`android.permission.DUMP`), وهي مفتاح التشغيل / الإيقاف الخاص بالمطور.
- تستخدم صلاحية INTERNET فقط لمستمع HTTP الخاص بالمكون الإضافي; ولا يرسل المكون الإضافي أي طلبات صادرة ولا يجمع أي بيانات. لا يسمح بـ HTTP غير المشفر إلا نحو عناوين الحلقة المحلية عبر تكوين أمان الشبكة.
- يستمع الخادم افتراضيا إلى 127.0.0.1 فقط. يبقى الوصول من الشبكة المحلية معطلا حتى تفعله; وبعد التفعيل يبقى الرمز وتأكيد الاقتران وقائمة Host المسموح بها وحدود المعدل سارية على الشبكة المحلية, ويذكرك إشعار يومي ما دام مفعلا.
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

#### v1.0.1

_2026/09/15_

- `ميزة` موارد التوثيق دون اتصال الاختيارية: عند تثبيت مكون AutoJs6 Offline Docs الإضافي وقيام المضيف بتمريره عبر app.listDocs / app.readDoc, يضيف resources/list المورد autojs6://docs/ (فهرس يحوي عناوين URI الفرعية) وموردا autojs6://docs/{+path} لكل صفحة توثيق, ويضيف resources/templates/list قالب docs; وبدون المكون الإضافي أو على مضيف لا يملك هاتين الطريقتين لا يدرج أي عنصر ويوضح _meta.docsCatalogStatus السبب
- `تحسين` رفع compileSdk إلى 37 (Android 17), يبقى targetSdk عند 36 حتى يتم التحقق من السلوك المعتمد على الهدف
- `تحسين` مطابقة MCP (P6): شغلنا حزمة @modelcontextprotocol/conformance الرسمية 0.1.16 على جهازين ضد مسار /mcp ذي الحالة. ينجح 9 من 32 سيناريو خادم (initialize, ping, tools/list, نتائج الأدوات النصية والخاطئة, resources/list, prompts/list, تدفقات SSE متزامنة, الحماية من DNS rebinding); ويستدعي 18 التجهيزات المرجعية الخاصة بالحزمة (أدوات test_* والمطالبات وموارد test:// التي يجيب عنها هذا الخادم بنتيجة أداة غير معروفة أو -32602 أو isError), ويحتاج 5 إلى قدرات لا يعلنها الخادم (logging, completions, الاشتراك في الموارد). أصبحت ترويسة Origin للحلقة المحلية مقبولة في كل الأوضاع كما تتوقع الحزمة; وتبقى ترويسات CORS وردود preflight مقتصرة على وضع المطور. لا يوجد مسار لنموذج 2026-07-28 عديم الحالة (Roadmap D9). التفاصيل في docs/dev/p6-conformance.md.
- `تحسين` تدقيق الأمان (P6): تم التحقق من بنود قائمة التحقق السبعة (تخزين الرمز, إخفاء السجلات, المكونات المصدرة, نطاق النص غير المشفر, تعطيل الشبكة المحلية افتراضيا, إلغاء الاقتران, تعطيل مجموعات الأدوات افتراضيا) في الشيفرة وعلى جهازين وتسجيلها في docs/dev/p6-security-audit.md, ويوضح قسم الأمان في README هذه الحدود الآن. أصبح HTTP غير المشفر مقصورا على عناوين الحلقة المحلية عبر تكوين أمان الشبكة بدلا من علامة usesCleartextTraffic على مستوى التطبيق; فالمكون الإضافي لا يفتح اتصالات عميل ولا يحتاج المستمع إلى هذه العلامة.
- `تحسين` خط الأساس للأداء (P6): تم قياس ui_dump عند 50 / 200 / 400 عقدة, و screen_capture بثلاثة أحجام, ودورات script_run, وأربعة طلبات متزامنة على محاكي API 24 وهاتف Sony (API 33) وجهاز Xiaomi Pad (API 35), وسجلت النتائج في docs/dev/p6-performance-baseline.md كمرجع بلا عتبات. يستغرق النداء الذي يجيب عنه المكون الإضافي وحده نحو 20 ms على المحاكي والهاتف, ويزداد ui_dump بنحو 0.05 ms لكل عقدة, ويجيب مسار لقطة الشاشة عبر خدمة إمكانية الوصول خلال 100 ms بينما يستغرق مسار MediaProjection على API 24 نحو 1.35 s لكل التقاط, وتكتمل الطلبات الأربعة المتزامنة في 1.0-1.8 من زمن دورة واحدة ضمن حد المضيف البالغ أربعة نداءات متزامنة.

#### v1.0.0

_2026/09/15_

- `تلميح` معاينة P4: 37 أداة, منها 33 مفعلة افتراضيا, مع مفتاح في قائمة AutoJs6 وصفحة إعدادات للإضافة. تتطلب نسخة AutoJs6 المتوافقة مع P4. ROADMAP.md.
- `ميزة` إعدادات الهاتف لحالة الخادم وUSB والمنفذ والشبكة المحلية وعرض الرمز ونسخه وتغييره وإلغاء الاقتران ومجموعات الأدوات وصلاحيات root ووضع المطور وإعدادات Claude Code / Cursor / Codex / HTTP القابلة للنسخ وسجل الإصدارات مع مظهر AutoJs6. تغييرات الشبكة تعيد تشغيل المستمع, وتطبق الرموز والأذونات فورا. نوافذ الأسرار تمنع لقطات الشاشة.
- `ميزة` توفر موارد MCP (P3.5) ملفات العمل للقراءة فقط, وتصفح أمثلة المضيف, ومعلومات الجهاز, ومخرجات وحدة التحكم الأخيرة, مع احترام الاقتران وإعدادات المجموعات. تتضمن قراءة النص والبيانات الثنائية معلومات الاقتطاع. توفر القوالب write_autojs6_script و automate_task و debug_selector إرشادات بالإنجليزية والصينية, مع استخدام الإنجليزية للغات الهاتف الأخرى.
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
- `ميزة` مسار الشبكة المحلية (P5.1): عند تشغيل الوصول من الشبكة المحلية تعرض صفحة الإعدادات عناوين الهاتف الحالية (تتحدث عند تغير Wi-Fi) مع تلميحات عن نفس الشبكة وجدار الحماية; يُميز طلب الاقتران القادم من الشبكة المحلية في مربع الحوار والإشعار; تذكير يومي يفيد بأن الخادم ما زال متاحا من الشبكة المحلية ويمكن إيقافه دون إعادة تشغيل المستمع. يوثق README مساري USB والشبكة المحلية.
- `ميزة` حدود معدل لكل عميل (P6): 20 طلبا في الثانية و 30 استدعاء screen_capture في الدقيقة لكل عميل على الأكثر. الطلب الذي يتجاوز الحد يحصل على HTTP 429 مع ترويسة Retry-After وخطأ JSON-RPC RATE_LIMITED يحمل retryAfterMs; ولقطة الشاشة التي تتجاوز الحد يرد عليها كنتيجة أداة RATE_LIMITED مع retryAfterMs ليتمكن النموذج من الانتظار. يبقى معدل استعلامات إمكانية الوصول في grant المضيف ساريا فوق ذلك.
- `ميزة` الإيقاف التلقائي عند الخمول والبطارية (P6): تقدم صفحة الإعدادات خيار "الإيقاف التلقائي عند الخمول" (معطل افتراضيا; 5 / 15 / 30 / 60 / 120 دقيقة). يتوقف المستمع من تلقاء نفسه بعد المدة المختارة بدون طلبات من العملاء, ويسجل سبب idle_timeout الداخلي, ويترك إشعارا واحدا يختفي تلقائيا, ولا يعد إيقافا من المستخدم; الطلب الجاري (استدعاء أداة قيد التنفيذ) لا يعد خمولا, بينما العميل الذي يبقي تدفقه مفتوحا دون إرسال طلبات لا يبقي الخادم يعمل. عندما يقيد Android استخدام البطارية للتطبيق (HyperOS و MIUI يفعلان ذلك افتراضيا للتطبيقات المثبتة يدويا ويوقفان الخدمة الأمامية بعد نحو دقيقة من إطفاء الشاشة على البطارية), تعرض صفحة الإعدادات تحذيرا مع زر "إعدادات البطارية" ويتلقى AutoJs6 حدث warning. وقت CPU في الخمول وتقدير batterystats لساعة خمول مسجلان في docs/dev/p6-battery-and-residency.md.
- `إصلاح` لم تعد عملية rebuild في IDE تبحث عن APK لاختبارات وحدة JVM. تنشئ مهام التحقق من APK الملفات المطلوبة تلقائيا وتعمل مباشرة بعد clean.
- `إصلاح` اختلاف حجم أيقونة مركز الإضافات بين الوضعين الفاتح والداكن; يستخدم الوضع الليلي أيضا الأيقونة التكيفية, مع ضبط حجم الطبقات للحفاظ على الرسم والهوامش في ic_launcher_round.png وتغيير لون الخلفية فقط
- `إصلاح` كان التنقل بمفتاح Tab في صفحة الإعدادات يتخطى زر الرجوع في شريط الأدوات; الآن تشمل دورة Tab زر الرجوع وجميع عناصر التحكم بما في ذلك Android 7. تتحقق اختبارات الأجهزة من تسميات قارئ الشاشة والتشغيل بلوحة المفاتيح.
- `إصلاح` كانت خريطة arguments في script_run و script_run_file تعلن نوع القيم كمصفوفة JSON Schema يرفضها أو يضعفها بعض عملاء MCP; يستخدم المخطط الآن فروع anyOf أحادية النوع. أضيف إلى README قسما العملاء والأسئلة الشائعة مع مصفوفة العملاء المختبرين.
- `إصلاح` ترفض أجسام الطلبات العدائية قبل أن يتعمق فيها أي محلل: JSON المتداخل لأكثر من 64 مستوى, والجسم الذي يكرر معرف طلب, ومعرف الطلب الذي ما زال قيد المعالجة في نفس الجلسة (كان يترك الطلب الأول بلا رد) تحصل على 400 مع خطأ JSON-RPC; ويحصل تدفق GET بلا جلسة فعالة على 400 / 404 بدلا من تدفق أحداث فارغ. تغطي اختبارات JVM والأجهزة (API 28 / 31 / 33 / 35) الحالات الطويلة جدا والعميقة جدا و UTF-8 غير الصالح والطرق المجهولة وتركيبات الترويسات و base64 الكبير و 64 جلسة متزامنة.
- `إصلاح` مصفوفة دورة الحياة (P6): الطلب الذي يحمل معرف جلسة عملية مستمع سابقة (بعد قتل المستمع أو إعادة تشغيله بسبب تدوير الرمز) يترك لاستجابة 404 من طبقة النقل ليعيد العميل initialize, بدلا من إظهار طلب اقتران زائد باسم User-Agent الطلب لعميل مقترن أصلا; وتزال إشعارات الاقتران التي تركتها عملية مستمع ميتة في لوحة الإشعارات عند بدء المستمع التالي. قتل المضيف, قتل المستمع, قتل الاثنين معا, الإيقاف القسري من إعدادات النظام, تدوير الرمز أثناء جلسة حية, والاقتران المعلق عبر قتل المضيف وقتل المستمع, سجلت حالاتها المتوقعة ومسارات استعادتها في docs/dev/lifecycle-matrix.md وتم التحقق منها على أجهزة حقيقية.
- `تحسين` التحقق أثناء البناء لمنع إدخال تبعيات أصلية غير مقصودة, مع تقرير JSON
- `تبعية` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) على محرك Ktor 3.5.1 CIO
- `تبعية` إضافة Ktor 3.5.1 `ktor-server-test-host` لاختبارات النقل على JVM (نطاق الاختبار فقط)
- `تبعية` أضيف `mcp-server-api.aar` (وحدة AutoJs6 `plugin-api/mcp-server-api`, بناء المضيف 6.8.0 / 5279, MPL 2.0) عقدا لـ Binder بين AutoJs6 والإضافة, مع تثبيت التجزئة في `locks/host-api-aars.lock`
- `تبعية` تحديث حزمتَي common-plugin-api وmcp-server-api من مضيف P4: امتداد إعدادات اختياري v1 وترتيب AIDL دون تغيير وقفل SHA-256 وتوافق SDK 36.

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
