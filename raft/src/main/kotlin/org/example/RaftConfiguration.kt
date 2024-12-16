package org.example

data class RaftNodeData(val id: Int, val host: String, val port: Int)

interface RaftConfiguration {
    val port: Int
    val nodes: List<RaftNodeData>
    val hosts: Map<Int, RaftNodeData>
    val id: Int
    val timerInterval: Long
    val heartbeatInterval: Long
}

class StaticConfiguration(
    override val id: Int, override val port: Int = 4000,
    override val timerInterval: Long = (10000..15000).random().toLong(),
    override val heartbeatInterval: Long = 1000
) : RaftConfiguration {
    override val nodes: List<RaftNodeData> = mutableListOf()
    override val hosts: Map<Int, RaftNodeData> = emptyMap()
}

class EnvConfiguration : RaftConfiguration {
    val env = System.getenv()
    override val port = env.getOrDefault("PORT", "4000").toInt()
    override val nodes: List<RaftNodeData> = env.getOrDefault("NODES", "50:localhost:4000")
        .split(",")
        .map {
            val uri = it.split(":")
            RaftNodeData(uri[0].toInt(), uri[1], uri[2].toInt())
        }.toList()

    override val hosts: Map<Int, RaftNodeData> = nodes.groupBy { it.id }.mapValues { (_, v) -> v.first() }

    override val id = env.getOrDefault("ID", (Math.random() * 100).toInt().toString()).toInt()

    override val timerInterval: Long = env.getOrDefault("TIMER", "5000").toLong()

    override val heartbeatInterval: Long = env.getOrDefault("HEARTBEAT_TIMER", "1000").toLong()
}
