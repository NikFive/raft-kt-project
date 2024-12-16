package org.example.grpc

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import org.example.adapters.RaftHandler
import org.example.proto.*


class ClusterNodeService(val raft: RaftHandler) : ClusterNodeGrpc.ClusterNodeImplBase() {
    override fun requestVote(request: VoteRequest, responseObserver: StreamObserver<VoteResponse>) {
        val result = runBlocking { raft.requestVote(request) }
        responseObserver.onNext(result)
        responseObserver.onCompleted()
    }

    override fun appendEntries(request: AppendRequest, responseObserver: StreamObserver<AppendResponse>) {
        val result = runBlocking {
            raft.appendEntries(request)
        }
        responseObserver.onNext(result)
        responseObserver.onCompleted()
    }
}
