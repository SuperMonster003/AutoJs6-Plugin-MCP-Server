package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The activity marker behind the bearer check (roadmap P6): refused requests never count, an
 * authenticated `POST` counts until its response completed, a `GET` counts when it arrives.
 */
class IdleStopInterceptorTest {

    private var now = 1_000L
    private val idleMonitor = IdleStopMonitor { now }
    private val token = BearerTokens.generate()

    @Volatile
    private var inFlightSeenByHandler = -1

    private fun ApplicationTestBuilder.mount() {
        application {
            installRequestGate { GatePolicy.loopback() }
            installBearerAuth { token }
            installActivityMarker(idleMonitor)
            routing {
                post("/mcp") {
                    delay(10)
                    inFlightSeenByHandler = idleMonitor.inFlightCount
                    call.respondText("ok")
                }
                get("/mcp") { call.respondText("stream") }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.post(authorization: String?): HttpResponse = client.post("/mcp") {
        header(HttpHeaders.Host, "localhost")
        authorization?.let { header(HttpHeaders.Authorization, it) }
        setBody("{}")
    }

    @Test
    fun refusedRequestsDoNotCountAsActivity() = testApplication {
        mount()
        now += 30_000L
        assertEquals(HttpStatusCode.Unauthorized, post(null).status)
        assertEquals(HttpStatusCode.Unauthorized, post("Bearer ${BearerTokens.generate()}").status)
        assertEquals(1_000L, idleMonitor.lastActivityAt)
        assertEquals(0, idleMonitor.inFlightCount)
    }

    @Test
    fun anAuthenticatedPostCountsUntilItsResponseCompleted() = testApplication {
        mount()
        now += 30_000L
        assertEquals(HttpStatusCode.OK, post("Bearer $token").status)
        assertEquals("the handler runs with the request in flight", 1, inFlightSeenByHandler)
        assertEquals(0, idleMonitor.inFlightCount)
        assertEquals(31_000L, idleMonitor.lastActivityAt)
    }

    @Test
    fun anAuthenticatedGetCountsWhenItArrives() = testApplication {
        mount()
        now += 45_000L
        val response = client.get("/mcp") {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(46_000L, idleMonitor.lastActivityAt)
        assertEquals(0, idleMonitor.inFlightCount)
    }
}
