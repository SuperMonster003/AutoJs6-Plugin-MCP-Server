يحول MCP Server جهاز Android الذي يعمل عليه AutoJs6 إلى خادم [Model Context Protocol](https://modelcontextprotocol.io). يتصل وكلاء الذكاء الاصطناعي على الحاسوب, مثل Claude Code أو Cursor أو MCP Inspector, بالهاتف عبر USB أو Wi-Fi ويستخدمون الأدوات لتشغيل البرامج النصية, وقراءة السجلات, وفحص شجرة عقد إمكانية الوصول, والنقر والكتابة, والتقاط لقطات الشاشة, والتعامل مع الملفات والتطبيقات.

المشروع في مرحلة الهيكل: يسجل هذا الإصدار المكون الإضافي في مركز المكونات الإضافية في AutoJs6 ويجهز البنية التحتية للبناء والتوثيق والاختبار. نقطة نهاية MCP وأدواتها غير متاحة بعد. يتم تتبع التقدم بندا بندا في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. شغل خادم MCP من درج AutoJs6 أو من صفحة إعدادات المكون الإضافي; يعرض الهاتف عنوان نقطة النهاية ورمز الاقتران.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
