package org.example.actions

import org.example.adapters.ClusterNode
import org.example.state.NodeState
import org.example.state.State

class CommitAction(val state: State, val cluster: List<ClusterNode>) {

    private val log = state.log

    fun perform() {
        if (state.current == NodeState.LEADER) {
            val newCommit = ((state.log.commitIndex + 1)..Int.MAX_VALUE)
                .takeWhile { newCommit ->
                    val clusterApprove = matchIndexMatches(newCommit)
                    val logLastTermMatch = log[newCommit]?.term == state.term
                    clusterApprove && logLastTermMatch
                }
                .lastOrNull()
            newCommit?.run {
                println("Doing commit at $newCommit")
                log.commit(this)
            }
        }
    }

    private fun matchIndexMatches(newCommit: Int): Boolean {
        val majority = Math.floorDiv(cluster.size, 2)
        return cluster.filter { it.matchIndex >= newCommit }.count() > majority
    }
}
