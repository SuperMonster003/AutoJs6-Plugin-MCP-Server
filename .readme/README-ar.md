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

المشروع في مرحلة الهيكل: يسجل هذا الإصدار المكون الإضافي في مركز المكونات الإضافية في AutoJs6 ويجهز البنية التحتية للبناء والتوثيق والاختبار. نقطة نهاية MCP وأدواتها غير متاحة بعد. يتم تتبع التقدم بندا بندا في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### الميزات المخطط لها

******

تقدم خارطة الطريق القدرات التالية على مراحل:

- تنفيذ البرامج النصية: تشغيل JavaScript من نص أو من ملف داخل AutoJs6, وعرض المحركات وإيقافها, وقراءة مخرجات وحدة التحكم الأخيرة.
- واجهة إمكانية الوصول: تفريغ شجرة العقد بتنسيق نصي مضغوط, والعثور على العقد بصيغة محددات AutoJs6, والنقر, والضغط المطول, والتمرير, وتعيين النص, والضغط على المفاتيح العامة مثل رجوع والشاشة الرئيسية.
- لقطات الشاشة: التقاط الشاشة بصيغة PNG أو JPEG مع حد أقصى للحجم يناسب النماذج متعددة الوسائط.
- الملفات والتطبيقات والجهاز: قراءة الملفات وكتابتها ضمن دليل عمل AutoJs6, وتشغيل التطبيقات, والاستعلام عن النافذة الأمامية, والإبلاغ عن معلومات الجهاز.
- مسارات الاتصال: USB عبر `adb forward`, والشبكة المحلية بتفعيل صريح, وجسر stdio على جانب الحاسوب, ونفق عام اختياري مع OAuth 2.1.
- الأمان: رمز bearer قابل للتدوير, وتأكيد الاقتران على الهاتف عند أول استخدام, ومفاتيح تبديل للأدوات حسب المجموعة; ولا يستمع الخادم افتراضيا إلا على واجهة الاسترجاع المحلي.

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. شغل خادم MCP من درج AutoJs6 أو من صفحة إعدادات المكون الإضافي; يعرض الهاتف عنوان نقطة النهاية ورمز الاقتران.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.

> تصف الخطوتان 3 و 4 سير العمل المخطط له وتصبحان متاحتين بعد اكتمال مراحل خارطة الطريق المقابلة. يدعم المكون الإضافي Android 7.0 (API 24) أو أحدث.

******

### إعداد العميل

******

يسجل Claude Code الخادم بأمر واحد; وتستخدم العملاء الأخرى نفس عنوان URL والترويسة في إعدادات MCP الخاصة بها:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

استبدل الرمز بالقيمة المعروضة على الهاتف. لا يعمل الأمر إلا بعد أن يصبح بالإمكان تشغيل الخادم (انظر `الحالة`).

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

تستجيب `McpServerPluginService` للإجراء `org.autojs.plugin.MCP_SERVER` (الفئة `mcp-server`) وتعمل في العملية `:mcp_server`. يعرف المضيف عقد AIDL `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` في وحدته `mcp-server-api` ويصل مع المرحلة P1 من خارطة الطريق; وحتى ذلك الحين لا تكشف الخدمة سوى واصف العقد. تستجيب `McpServerPluginInfoService` لـ `org.autojs.plugin.INFO` بكائن `PluginInfo` القياسي, وتتيح `WakeActivity` للمضيف إيقاظ عملية المكون الإضافي على الأجهزة التي تبقي التطبيقات المثبتة حديثا متوقفة.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/09/07_

- `تلميح` معاينة تطويرية: يسجل المكون الإضافي نفسه في مركز المكونات الإضافية في AutoJs6, لكن نقطة نهاية MCP وأدواتها غير متاحة بعد
- `ميزة` هوية المكون الإضافي `mcp-server` مع خدمة INFO و Wake Activity وهيكل خدمة `org.autojs.plugin.MCP_SERVER` لاكتشاف المضيف
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بـ 10 لغات
- `ميزة` نقطة نهاية Streamable HTTP على `http://127.0.0.1:9637/mcp` مع الأداة `device_ping`, تستضيفها خدمة أمامية يمكن تشغيلها وإيقافها عبر adb أو من المضيف (معاينة تطويرية)
- `تبعية` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) على محرك Ktor 3.5.1 CIO

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
