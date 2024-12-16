package org.example.grpc

import io.grpc.ManagedChannelBuilder
import org.example.adapters.ClusterNode
import org.example.proto.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class GrpcClusterNode(val host: String, val port: Int) : ClusterNode {
    @Volatile
    override var nextIndex: Int = 0

    @Volatile
    override var matchIndex: Int = -1

    override val nodeId: String = "$host:$port"

    private val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    private val stub = ClusterNodeGrpc.newBlockingStub(channel)

    override suspend fun requestVote(request: VoteRequest): VoteResponse? {
        return suspendCoroutine {
            it.resume(kotlin.runCatching { stub.requestVote(request) }.getOrNull())
        }
    }

    override suspend fun appendEntries(request: AppendRequest): AppendResponse? {
        return suspendCoroutine {
            it.resume(kotlin.runCatching { stub.appendEntries(request) }.getOrNull())
        }
    }

    override fun reinitializeIndex(index: Int) {
        println("Index for node $host:$port reinitialized")
        super.reinitializeIndex(index)
    }
}
