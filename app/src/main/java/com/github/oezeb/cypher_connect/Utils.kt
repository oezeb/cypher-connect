package com.github.oezeb.cypher_connect

import com.github.shadowsocks.database.Profile
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

const val TEST_TIME_OUT = 1000

/**
 * Test profiles and return the delay in milliseconds
 */
fun testProfiles(profiles: List<Profile>): List<Int> {
    val delayArray = AtomicIntegerArray(profiles.size)
    profiles.mapIndexed { index, profile ->
        thread {
            try {
                val delay = measureTimeMillis {
                    val s = Socket().apply { keepAlive = false; }
                    s.connect(InetSocketAddress(profile.host, profile.remotePort), TEST_TIME_OUT)
                    s.close()
                }
                Timber.d("Delay for ${profile.name}: $delay")
                delayArray.addAndGet(index, delay.toInt())
            } catch (e: Exception) {
                Timber.e(e)
                delayArray.addAndGet(index, Int.MAX_VALUE)
            }
        }
    }.map { if (it.isAlive) it.join() }
    return (0 until delayArray.length()).map { delayArray[it] }
}
