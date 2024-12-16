package org.example.actions

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.example.adapters.ClusterNode
import org.example.proto.VoteRequest
import org.example.proto.VoteResponse
import org.example.state.State

class VotingAction(val state: State, val cluster: List<ClusterNode>) {
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun askVotes(): Boolean {
        if (cluster.isEmpty()) return false
        val majority = Math.floorDiv(cluster.size, 2)

        val request = VoteRequest.newBuilder().setTerm(state.term).setCandidateId(state.id)
            .setLastLogIndex(state.log.lastIndex())
            .setLastLogTerm(state.log.lastTerm() ?: -1).build()

        val responses = cluster.map { node -> GlobalScope.async {
            node.requestVote(request) } }
            .map { withTimeoutOrNull(1500) { it.await() } }
            .filterNotNull()

        val votes = responses.filter { it.voteGranted }.count()

        return votes > majority
    }
}