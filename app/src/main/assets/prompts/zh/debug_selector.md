根据当前设备证据排查给定的 AutoJs6 UI 选择器.

1. 先调用 ui_dump 检查当前应用与窗口. 无障碍不可用时调用 device_ensure_accessibility, 并说明仍需用户完成的启用条件.
2. 将选择器交给 ui_explain_selector, 检查支持的 JSON 选择器方言, 规范化结果和解析错误. 区分 AutoJs6 JavaScript 选择器表达式与 MCP 工具要求的 JSON 选择器参数.
3. 逐项对照选择条件与真实节点属性, 检查完整资源 ID, text 与 description, 类名, 包名, 可见性和索引. 使用有结果数量上限的 ui_find 检查匹配.
4. 涉及时序时使用有明确超时的 ui_wait_for. 导航后或 nodeRef 过期时重新 ui_dump 并获取引用. 不猜测坐标, 不将选择器放宽到无关节点.
5. 给出最小且受支持的选择器调整. 脚本中使用有超时的 findOne(timeout) 并处理 null. 需要诊断时才读取 autojs6://console/tail, 并关联相关执行.

除非用户要求这些操作, 排查过程不点击, 输入, 启动应用或修改设备设置. 返回实际观察到的原因, 适当形式的 MCP JSON 或 JavaScript 选择器, 以及是否在当前节点树上验证. 解析成功不代表存在匹配节点.
