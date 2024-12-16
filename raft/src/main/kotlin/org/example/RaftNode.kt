package org.example

import io.grpc.ServerBuilder
import org.example.grpc.ClusterNodeService
import org.example.grpc.GrpcClusterNode
import org.example.log.Command
import org.example.log.LogState
import org.example.state.NodeState

class RaftNode {
    val config = EnvConfiguration()
    private val nodes = config.nodes
        .map { (id, host, port) -> GrpcClusterNode(host, port) }

    private val controller = RaftController(config, nodes.toMutableList()).apply { start() }

    private val server = ServerBuilder.forPort(config.port)
        .addService(ClusterNodeService(controller))
        .build().apply { start() }

    fun getState(): LogState {
        return controller.state.log.state()
    }

    fun isLeader() = controller.state.current == NodeState.LEADER

    fun leaderNode() = config.hosts[controller.state.leaderId] ?: throw RuntimeException("Leader not found")

    suspend fun applyCommand(command: Command): Boolean {
        return controller.applyCommand(command)
    }

    fun await() {
        server.awaitTermination()
    }
}
