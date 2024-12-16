package org.example


import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.log.Set

object KeyValue {
    val node = RaftNode()
    val serverPortInc = 4000

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, serverPortInc + node.config.port) {
            routing {
                get("/{key}") {
                    val key = call.parameters["key"]!!
                    if (node.isLeader()) {
                        val data = node.getState()[key]?.let { String(it) } ?: ""
                        call.respondText(data, ContentType.Text.Plain)
                    } else {
                        val leader = node.leaderNode()
                        call.response.header("Location", "http://${leader.host}:${leader.port + serverPortInc}/$key")
                        call.respond(HttpStatusCode.TemporaryRedirect, "redirected to master")
                    }
                }
                post("/{key}") {
                    val key = call.parameters["key"]!!
                    if (node.isLeader()) {
                        val data = call.receiveText()
                        val result = node.applyCommand(Set(key, data.toByteArray()))
                        call.respondText("Result: $result")
                    } else {
                        val leader = node.leaderNode()
                        call.response.header("Location", "http://${leader.host}:${leader.port + serverPortInc}/$key")
                        call.respond(HttpStatusCode.TemporaryRedirect, "redirected to master")
                    }
                }
            }
        }.start(true)
        node.await()
    }
}
