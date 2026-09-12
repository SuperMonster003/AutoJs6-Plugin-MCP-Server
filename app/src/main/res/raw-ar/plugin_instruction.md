يحول MCP Server جهاز Android الذي يعمل عليه AutoJs6 إلى خادم [Model Context Protocol](https://modelcontextprotocol.io). يتصل وكلاء الذكاء الاصطناعي على الحاسوب, مثل Claude Code أو Cursor أو MCP Inspector, بالهاتف عبر USB أو Wi-Fi ويستخدمون الأدوات لتشغيل البرامج النصية, وقراءة السجلات, وفحص شجرة عقد إمكانية الوصول, والنقر والكتابة, والتقاط لقطات الشاشة, والتعامل مع الملفات والتطبيقات.

معاينة تطوير حتى P3.3: تم تنفيذ نقطة MCP مع المصادقة والاقتران وأدوات النصوص البرمجية والواجهة ولقطات الشاشة. تعيد screen_capture صور JPEG أو PNG أو WebP, وتعرض screen_state الأبعاد والاتجاه. مفتاح القائمة وصفحة الإعدادات مخططان في P4. التقدم وأدلة اختبار الأجهزة في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) على جهاز يحتوي على AutoJs6 بالبنية 5279 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `MCP Server`, ثم فعله. تجتاز حزم الإصدار الرسمية التحقق من التوقيع تلقائيا.
3. لهذه المعاينة استخدم تحكم adb وجلسة اختبار المضيف الموضحين في ملاحظات التطوير; مفتاح القائمة وإعدادات الملحق مخططان في P4.
4. على الحاسوب, نفذ `adb forward tcp:9637 tcp:9637` ووجه عميل MCP إلى `http://127.0.0.1:9637/mcp` مع استخدام الرمز كبيانات اعتماد bearer.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
