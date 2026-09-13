يحول MCP Server جهاز Android الذي يعمل عليه AutoJs6 إلى خادم [Model Context Protocol](https://modelcontextprotocol.io). يتصل وكلاء الذكاء الاصطناعي على الحاسوب, مثل Claude Code أو Cursor أو MCP Inspector, بالهاتف عبر USB أو Wi-Fi ويستخدمون الأدوات لتشغيل البرامج النصية, وقراءة السجلات, وفحص شجرة عقد إمكانية الوصول, والنقر والكتابة, والتقاط لقطات الشاشة, والتعامل مع الملفات والتطبيقات.

معاينة P4: 37 أداة, منها 33 مفعلة افتراضيا, مع مفتاح في قائمة AutoJs6 وصفحة إعدادات للإضافة. تتطلب نسخة AutoJs6 المتوافقة مع P4. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. شغل MCP Server من قائمة AutoJs6. اضغط مطولا على عنوانه لفتح الإعدادات, أو استخدم إعداداته في مركز الإضافات. انسخ إعداد عميل الكمبيوتر.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.
5. أكد طلب الاقتران الأول على الهاتف. أوقف الخادم من القائمة أو الإعدادات أو الإشعار عند الانتهاء.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
