package org.example.actions

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.example.adapters.ClusterNode
import org.example.proto.AppendRequest
import org.example.state.NodeState
import org.example.state.State

class HeartbeatAction(val state: State, val cluster: List<ClusterNode>) {
    private val log = state.log

    suspend fun send() {
        if (state.current == NodeState.LEADER) {
            println("HEARTBEAT_SEND AT " + System.currentTimeMillis() + " " + cluster.size)
            cluster.map {
                val prevIndex = it.nextIndex - 1
                val prevTerm = if (prevIndex != -1) log[prevIndex]?.term ?: throw RuntimeException("WAT") else -1

                val entries = log.starting(prevIndex + 1)
                val request = AppendRequest.newBuilder()
                    .setTerm(state.term).setLeaderId(state.id).setLeaderCommit(state.log.commitIndex)
                    .setPrevLogIndex(prevIndex).setPrevLogTerm(prevTerm)
                    .addAllEntries(entries)
                    .build()
                GlobalScope.async {
                    val response =  it.appendEntries(request)
                    if (response == null) null else it to response
                }
            }.mapNotNull {
                withTimeoutOrNull(1500) {
                    it.await()
                }
            }
                .forEach { (node, response) ->
                    when {
                        response.success -> {
                            node.nextIndex = log.lastIndex() + 1
                            node.matchIndex = node.nextIndex - 1
                        }
                        !response.success -> {
                            println("Heartbeat response: ${response.success}-${response.term}")
                            node.decreaseIndex()
                        }
                    }
                }
        }
    }
}
