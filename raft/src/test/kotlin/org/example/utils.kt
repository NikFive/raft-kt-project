package org.example

import org.example.proto.AppendRequest
import org.example.proto.AppendResponse
import org.example.proto.VoteRequest
import org.example.proto.VoteResponse
import org.example.adapters.ClusterNode
import org.example.state.State
import kotlinx.coroutines.runBlocking


fun test(body: suspend () -> Unit) {
    runBlocking {
        body()
    }
}

class TestRaftNode(val state: State) : ClusterNode {
    override val nodeId: String = "TestNode-${state.id}"

    override var nextIndex: Int = state.log.lastIndex() + 1
    override var matchIndex: Int = -1

    override suspend fun requestVote(request: VoteRequest): VoteResponse = state.requestVote(request)
    override suspend fun appendEntries(request: AppendRequest): AppendResponse = state.appendEntries(request)

}


class LocalRaftNode(val controller: RaftController) : ClusterNode {

    override var nextIndex: Int = controller.state.log.lastIndex() + 1
    override var matchIndex: Int = -1
    override val nodeId: String = "LocalTestNode-${controller.state.id}"

    private var isolated = true

    fun isolate() {
        if (!isolated) {
            isolated = true
            controller.stop()
        }
    }

    fun activate() {
        if (isolated) {
            isolated = false
            controller.start()
        }

    }

    override suspend fun requestVote(request: VoteRequest): VoteResponse? =
            if (isolated) null else controller.requestVote(request)

    override suspend fun appendEntries(request: AppendRequest): AppendResponse? =
            if (isolated) null else controller.appendEntries(request)

    override fun toString(): String {
        return "LocalRaftNode(controller=$controller, nextIndex=$nextIndex, matchIndex=$matchIndex, nodeId='$nodeId')"
    }

}

fun State.testNode(): TestRaftNode {
    return TestRaftNode(this)
}