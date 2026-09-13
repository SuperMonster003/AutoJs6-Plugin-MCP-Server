Diagnose the supplied AutoJs6 UI selector from current device evidence.

1. Call ui_dump first and check the active application and window. If accessibility is unavailable, call device_ensure_accessibility and report any remaining activation requirement.
2. Pass the selector to ui_explain_selector. Inspect the supported JSON selector dialect, normalized form, and parser errors. Keep AutoJs6 JavaScript selector expressions distinct from the MCP tool's JSON selector argument.
3. Compare each selector predicate with observed node attributes. Check full resource IDs, text versus description, class name, package, visibility, and index. Use ui_find to inspect matches with a bounded limit.
4. If timing is involved, use ui_wait_for with an explicit timeout. After navigation or a stale nodeRef, obtain a fresh ui_dump and retry using a new reference. Avoid guessing coordinates or weakening the selector until it targets unrelated nodes.
5. Suggest the smallest supported selector change. For script code, use a bounded findOne(timeout) and handle a null result. Read autojs6://console/tail only when diagnostics are needed and correlate the relevant execution.

Diagnosis should not click, type, launch apps, or change device settings unless the user asked for those actions. Return the observed cause, corrected MCP JSON or JavaScript selector as appropriate, and whether it was verified on the current tree. Do not claim a match solely because parsing succeeded.
