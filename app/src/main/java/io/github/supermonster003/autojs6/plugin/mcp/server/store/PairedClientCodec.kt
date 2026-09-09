package io.github.supermonster003.autojs6.plugin.mcp.server.store

import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * JSON form of the paired-client list (roadmap P2.2), hand-written so the module needs no
 * serialization compiler plugin. Decoding is tolerant: entries that miss a field are skipped
 * instead of failing the whole list.
 */
object PairedClientCodec {

    const val FORMAT = 1

    fun encode(clients: List<PairedClient>): String = buildJsonObject {
        put("format", FORMAT)
        put(
            "clients",
            JsonArray(
                clients.map { client ->
                    buildJsonObject {
                        put("fingerprint", client.fingerprint)
                        put("name", client.name)
                        if (client.version != null) put("version", client.version) else put("version", JsonNull)
                        put("addressClass", client.addressClass.id)
                        put("firstPairedAt", client.firstPairedAt)
                        put("lastSeenAt", client.lastSeenAt)
                    }
                },
            ),
        )
    }.toString()

    fun decode(json: String?): List<PairedClient> {
        if (json.isNullOrBlank()) return emptyList()
        val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return emptyList()
        val entries = root["clients"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return emptyList()
        return entries.mapNotNull { element ->
            runCatching {
                val entry = element.jsonObject
                val fingerprint = entry["fingerprint"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@runCatching null
                val name = entry["name"]?.jsonPrimitive?.content ?: return@runCatching null
                val version = entry["version"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
                val addressClass = AddressClass.fromId(entry["addressClass"]?.jsonPrimitive?.content) ?: return@runCatching null
                val firstPairedAt = entry["firstPairedAt"]?.jsonPrimitive?.longOrNull ?: return@runCatching null
                val lastSeenAt = entry["lastSeenAt"]?.jsonPrimitive?.longOrNull ?: firstPairedAt
                PairedClient(fingerprint, name, version, addressClass, firstPairedAt, lastSeenAt)
            }.getOrNull()
        }.distinctBy { it.fingerprint }
    }
}
