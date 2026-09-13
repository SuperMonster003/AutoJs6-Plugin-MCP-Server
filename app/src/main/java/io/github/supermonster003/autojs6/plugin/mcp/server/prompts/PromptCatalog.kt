package io.github.supermonster003.autojs6.plugin.mcp.server.prompts

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.*
import java.util.Locale

/** Prompt bodies are packaged text assets; only declared, bounded string arguments are accepted. */
class PromptCatalog(private val loadText: (String) -> String, private val defaultLanguage: () -> String = { "en" }) {
    fun install(server: Server) {
        all.forEach { prompt -> server.addPrompt(prompt) { request -> get(request.params.name, request.params.arguments.orEmpty()) } }
        server.onConnect {
            server.sessions.values.forEach { session ->
                session.setRequestHandler<GetPromptRequest>(Method.Defined.PromptsGet) { request, _ ->
                    get(request.params.name, request.params.arguments.orEmpty())
                }
            }
        }
    }

    fun get(name: String, arguments: Map<String, String> = emptyMap()): GetPromptResult {
        val prompt = all.firstOrNull { it.name == name } ?: invalid("Unknown prompt")
        val specs = prompt.arguments.orEmpty()
        if (arguments.keys.any { key -> specs.none { it.name == key } }) invalid("Unknown prompt argument")
        specs.forEach { spec ->
            val value = arguments[spec.name]
            if (spec.required == true && value.isNullOrBlank()) invalid("Missing prompt argument: ${spec.name}")
            if (value != null && (value.toByteArray(Charsets.UTF_8).size > MAX_ARGUMENT_BYTES || '\u0000' in value)) invalid("Prompt argument exceeds its boundary")
        }
        val language = arguments[LANGUAGE]?.let {
            if (it !in setOf("en", "zh", "zh-CN", "zh-Hans", "zh-TW", "zh-Hant", "zh-HK")) invalid("language must be en or a supported zh language tag")
            it
        } ?: defaultLanguage()
        val tag = if (language.lowercase(Locale.ROOT).startsWith("zh")) "zh" else "en"
        return GetPromptResult(
            messages = buildList {
                add(PromptMessage(Role.User, TextContent(loadText("prompts/$tag/$name.md"))))
                specs.filter { it.name != LANGUAGE }.forEach { spec ->
                    arguments[spec.name]?.takeIf { it.isNotBlank() }?.let { value ->
                        add(PromptMessage(Role.User, TextContent("${spec.name}:\n$value")))
                    }
                }
            },
            description = prompt.description,
        )
    }

    companion object {
        const val WRITE_SCRIPT = "write_autojs6_script"
        const val AUTOMATE_TASK = "automate_task"
        const val DEBUG_SELECTOR = "debug_selector"
        const val LANGUAGE = "language"
        const val MAX_ARGUMENT_BYTES = 8192
        private val language = PromptArgument(LANGUAGE, "en or zh; defaults to the phone language, with English fallback")
        val all = listOf(
            Prompt(WRITE_SCRIPT, "Write an AutoJs6 script using host samples and bounded selectors", listOf(PromptArgument("goal", "What the script should do"), language)),
            Prompt(AUTOMATE_TASK, "Observe, act, and verify an Android automation task", listOf(PromptArgument("goal", "The requested task", required = true), language)),
            Prompt(DEBUG_SELECTOR, "Explain and diagnose an AutoJs6 UI selector", listOf(PromptArgument("selector", "The selector to diagnose", required = true), language)),
        )
        private fun invalid(message: String): Nothing = throw McpException(RPCError.ErrorCode.INVALID_PARAMS, message)
    }
}
