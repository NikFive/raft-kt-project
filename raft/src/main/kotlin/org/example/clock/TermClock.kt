package org.example.clock

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.concurrent.fixedRateTimer

class TermClock(private val interval: Long) {
    val channel = ConflatedBroadcastChannel<Long>()
    var term: Long = 0
        private set

    private val mutex: Mutex = Mutex(false)
    private var stopped = true
    private lateinit var timer: Timer

    private suspend fun updateTerm(term: Long) {
        println("⏳ Term ${this.term} -> $term")
        this.term = term
        channel.send(term)
    }

    suspend fun start() {
        mutex.withLock {
            if (stopped) {
                stopped = false
                schedule()
            }
        }
    }

    private fun schedule() {
        timer = fixedRateTimer(initialDelay = interval, period = interval) {
            runBlocking {
                println("SCHEDULE TIMER BOOM" + System.currentTimeMillis())
                updateTerm(term + 1)
            }
        }
    }

    suspend fun update(newTerm: Long) {
        mutex.withLock {
            this.term = newTerm
        }
    }

    suspend fun reset() {
        mutex.withLock {
            if (stopped) return
            timer.cancel()
            schedule()
        }
    }

    suspend fun freeze() {
        mutex.withLock {
            if (!stopped) {
                stopped = true
                timer.cancel()
            }
        }
    }
}
