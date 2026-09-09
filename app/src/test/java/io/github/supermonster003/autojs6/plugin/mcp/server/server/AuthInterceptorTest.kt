package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInterceptorTest {

    @Volatile
    private var token: String? = BearerTokens.generate()

    private fun ApplicationTestBuilder.mount() {
        application {
            installRequestGate { GatePolicy.loopback() }
            installBearerAuth { token }
            routing { post("/mcp") { call.respondText("ok") } }
        }
    }

    private suspend fun ApplicationTestBuilder.post(authorization: String?): HttpResponse = client.post("/mcp") {
        header(HttpHeaders.Host, "localhost")
        authorization?.let { header(HttpHeaders.Authorization, it) }
        setBody("{}")
    }

    @Test
    fun missingOrWrongTokensAre401WithAChallenge() = testApplication {
        mount()
        val missing = post(null)
        assertEquals(HttpStatusCode.Unauthorized, missing.status)
        assertEquals("Bearer realm=\"${BearerTokens.REALM}\"", missing.headers[HttpHeaders.WWWAuthenticate])
        val error = Json.parseToJsonElement(missing.bodyAsText()).jsonObject["error"]!!.jsonObject
        assertEquals(McpErrors.UNAUTHORIZED, error["code"]!!.jsonPrimitive.int)
        assertTrue(error["message"]!!.jsonPrimitive.content.startsWith(McpErrors.CODE_UNAUTHORIZED))

        val wrong = post("Bearer ${BearerTokens.generate()}")
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
        assertTrue(wrong.bodyAsText().contains("not accepted"))
        assertEquals(HttpStatusCode.Unauthorized, post("Basic abc").status)
        assertEquals(HttpStatusCode.Unauthorized, post("Bearer ${token!!.dropLast(1)}").status)
    }

    @Test
    fun theConfiguredTokenPassesRegardlessOfSchemeCase() = testApplication {
        mount()
        val ok = post("Bearer $token")
        assertEquals(HttpStatusCode.OK, ok.status)
        assertEquals("ok", ok.bodyAsText())
        assertNull(ok.headers[HttpHeaders.WWWAuthenticate])
        assertEquals(HttpStatusCode.OK, post("bearer $token").status)
    }

    @Test
    fun noConfiguredTokenFailsClosed() = testApplication {
        mount()
        token = null
        assertEquals(HttpStatusCode.Unauthorized, post("Bearer ${BearerTokens.generate()}").status)
        token = ""
        assertEquals(HttpStatusCode.Unauthorized, post("Bearer ").status)
    }

    @Test
    fun theRequestGateStillAnswersFirst() = testApplication {
        mount()
        val response = client.post("/mcp") {
            header(HttpHeaders.Host, "evil.example")
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
