package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClientRepository
import java.io.IOException

/**
 * The paired clients (roadmap P2.2) as one JSON document in the plugin's private storage, read
 * on every access and replaced atomically so that the server process and the settings page
 * (another process, P4.2) always see the same list. Token rotation leaves it untouched.
 */
class PairedClientStore(context: Context) : PairedClientRepository {

    private val document = ProcessSharedFile(context, FILE_NAME)

    override fun all(): List<PairedClient> = PairedClientCodec.decode(document.read())

    override fun put(client: PairedClient) {
        modify { clients -> clients.filter { it.fingerprint != client.fingerprint } + client }
    }

    override fun remove(fingerprint: String): Boolean {
        var removed = false
        modify { clients ->
            val next = clients.filter { it.fingerprint != fingerprint }
            removed = next.size != clients.size
            next
        }
        return removed
    }

    fun clear() {
        modify { emptyList() }
    }

    private fun modify(transform: (List<PairedClient>) -> List<PairedClient>) {
        try {
            document.update { current ->
                val next = transform(PairedClientCodec.decode(current))
                if (next.isEmpty()) null else PairedClientCodec.encode(next)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Cannot store the paired clients (${e.javaClass.simpleName})")
        }
    }

    companion object {

        const val FILE_NAME = "paired_clients.json"

        private const val TAG = "PairedClientStore"
    }
}
