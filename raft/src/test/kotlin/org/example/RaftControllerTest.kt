package org.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.example.proto.VoteRequest
import org.example.state.NodeState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(BlockJUnit4ClassRunner::class)
class RaftControllerTest {
    val consensus = RaftController(StaticConfiguration(id = 5), mutableListOf())


    @Test
    fun controllerIsWorkingConsistently() = test {
        val raft = RaftController(StaticConfiguration(id = 1, heartbeatInterval = 50, timerInterval = 400))
        assertThat(raft.state.term).isEqualTo(0)
        delay(1000)
        assertThat(raft.state.term).isEqualTo(0)
        raft.start()
        delay(1000)
        raft.stop()
        assertThat(raft.state.term).isEqualTo(2)
        delay(1000)
        assertThat(raft.state.term).isEqualTo(2)
    }


    @Test
    fun nodeCanVoteForCandidate() {
        assertThat(consensus.state.current).isEqualTo(NodeState.FOLLOWER)
        val request = VoteRequest.newBuilder().setCandidateId(10).setTerm(consensus.state.term).build()

        val result = runBlocking { consensus.requestVote(request) }
        assertThat(result.voteGranted).isEqualTo(true)
        assertThat(consensus.state.votedFor).isEqualTo(10L)
    }

    @Test
    fun voteForLowerTermIsNotGranted() {
        assertThat(consensus.state.votedFor).isEqualTo(null)
        assertThat(consensus.state.current).isEqualTo(NodeState.FOLLOWER)
        val request = VoteRequest.newBuilder().setCandidateId(10).setTerm(consensus.state.term - 1).build()
        val result = runBlocking { consensus.requestVote(request) }
        assertThat(result.voteGranted).isEqualTo(false)
        assertThat(consensus.state.votedFor).isEqualTo(null)
    }

    @Test
    fun onlyOneVotePerTermIsGranted() {
        assertThat(consensus.state.votedFor).isEqualTo(null)
        assertThat(consensus.state.current).isEqualTo(NodeState.FOLLOWER)

        val request = VoteRequest.newBuilder().setCandidateId(10).setTerm(consensus.state.term).build()

        val result = runBlocking { consensus.requestVote(request) }

        assertThat(result.voteGranted).isEqualTo(true)
        assertThat(consensus.state.votedFor).isEqualTo(10L)

        val requestTwo = VoteRequest.newBuilder().setCandidateId(1).setTerm(consensus.state.term).build()

        val resultTwo = runBlocking { consensus.requestVote(requestTwo) }

        assertThat(resultTwo.voteGranted).isEqualTo(false)
        assertThat(consensus.state.votedFor).isEqualTo(10)
    }

}