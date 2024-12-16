package org.example

import kotlinx.coroutines.*
import org.example.actions.CommitAction
import org.example.actions.HeartbeatAction
import org.example.actions.VotingAction
import org.example.adapters.ClusterNode
import org.example.adapters.RaftHandler
import org.example.clock.TermClock
import org.example.log.Command
import org.example.proto.AppendRequest
import org.example.proto.AppendResponse
import org.example.proto.VoteRequest
import org.example.proto.VoteResponse
import org.example.state.NodeState
import org.example.state.State
import java.util.*
import kotlin.concurrent.fixedRateTimer


class RaftController(
    val config: RaftConfiguration,
    val cluster: MutableList<ClusterNode> = mutableListOf()
) : RaftHandler {

    val state = State(id = config.id)

    private val clock = TermClock(config.timerInterval)
    private val heartbeat = HeartbeatAction(state, cluster)
    private val voting = VotingAction(state, cluster)
    private val commit = CommitAction(state, cluster)
    private val clockSubscription = clock.channel.openSubscription()
    private lateinit var termSubscriber: Job
    private lateinit var heartbeatTimer: Timer
    private lateinit var stateSubscriber: Job


    private val stateLogger = fixedRateTimer("logger", initialDelay = 1000, period = 1000) {
        println("⛳️ $state")
        if (state.current == NodeState.LEADER) {
            cluster.forEach {
                println("⚙️ Node: ${it.nodeId} - Next: ${it.nextIndex} Match: ${it.matchIndex}")
            }
        }
    }


    private fun prepareStateSubscriber() {
        stateSubscriber = GlobalScope.launch {
            for ((prev, current) in state.updates.openSubscription()) {
                when {
                    current == NodeState.LEADER -> {
                        cluster.forEach { it.reinitializeIndex(state.log.lastIndex() + 1) }
                        clock.freeze()
                    }

                    prev == NodeState.LEADER && current != NodeState.LEADER -> {
                        clock.start()
                    }
                }
            }
        }
    }

    private fun prepareHeartbeatTimer() {
        heartbeatTimer =
            fixedRateTimer("heartbeat", initialDelay = config.heartbeatInterval, period = config.heartbeatInterval) {
                println("SEND " + System.currentTimeMillis())
                runBlocking {
                    kotlin.runCatching {
                        withTimeoutOrNull(2000) {
                            heartbeat.send()
                            commit.perform()
                        }
                    }.onFailure {
                        println("Failure: $it")
                    }
                }
            }
    }

    private fun prepareTermIncrementSubscriber() {
        termSubscriber = GlobalScope.launch {
            for (term in clockSubscription) {
                println("Starting term increment")
                state.nextTerm(term)
                val result = voting.askVotes()
                if (result) {
                    state.promoteToLeader()
                } else {
                    println("---> 🤬 Can't promote to leader <---")
                }
            }

        }
    }

    private suspend fun actualizeTerm(receivedTerm: Long) {
        if (clock.term < receivedTerm) {
            clock.update(receivedTerm)
        }
    }

    fun start() {
        runBlocking {
            clock.start()
            prepareTermIncrementSubscriber()
            prepareHeartbeatTimer()
            prepareStateSubscriber()
        }
    }

    fun stop() {
        runBlocking {
            clock.freeze()
            termSubscriber.cancelAndJoin()
            heartbeatTimer.cancel()
            stateSubscriber.cancelAndJoin()
        }
    }

    override suspend fun requestVote(request: VoteRequest): VoteResponse {
        actualizeTerm(request.term)
        val vote = state.requestVote(request)
        if (vote.voteGranted) {
            clock.reset()
        }
        println("🗽Vote request: ${request.candidateId} - term  ${request.term} - result: ${vote.voteGranted}")
        return vote
    }

    override suspend fun appendEntries(request: AppendRequest): AppendResponse {
        actualizeTerm(request.term)
        val result = state.appendEntries(request)
        if (result.success) {
            clock.reset()
        }
        return result
    }

    suspend fun applyCommand(command: Command): Boolean {
        val index = state.applyCommand(command)

        while (index > state.log.commitIndex) {
            delay(0)
        }
        return true
    }
}
