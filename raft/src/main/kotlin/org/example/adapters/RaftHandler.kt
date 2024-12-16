package org.example.adapters

import org.example.proto.AppendRequest
import org.example.proto.AppendResponse
import org.example.proto.VoteRequest
import org.example.proto.VoteResponse

interface RaftHandler {
    suspend fun requestVote(request: VoteRequest): VoteResponse?

    suspend fun appendEntries(request: AppendRequest): AppendResponse?
}
