package com.github.oezeb.cypher_connect

import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

const val profilesURL = "https://gitee.com/oezeb/cypher-connect/raw/master/proxies.txt"

fun syncProfiles() {
    val text = URL(profilesURL).readText()

    val fetched = Profile.findAllUrls(text)
    val locale = ProfileManager.getAllProfiles() ?: emptyList()

    // Two profiles are equal if they have the same host and remote port
    val fetchedSet = fetched.map { it.host to it.remotePort }.toSet()
    val localeSet = locale.map { it.host to it.remotePort }.toSet()

    val toAdd = fetchedSet - localeSet
    val toRemove = localeSet - fetchedSet

    toAdd.forEach { (host, port) ->
        val profile = fetched.find { it.host == host && it.remotePort == port }
        if (profile != null) {
            ProfileManager.createProfile(profile)
        }
    }

    toRemove.forEach { (host, port) ->
        val profile = locale.find { it.host == host && it.remotePort == port }
        if (profile != null) {
            ProfileManager.delProfile(profile.id)
        }
    }

    // remove duplicates
    val set = mutableSetOf<Pair<String, Int>>()
    ProfileManager.getAllProfiles()?.forEach {
        if (set.contains(it.host to it.remotePort)) {
            ProfileManager.delProfile(it.id)
        } else {
            set.add(it.host to it.remotePort)
        }
    }
}

/**
 * Test profiles and return the delay in milliseconds
 */
fun testProfiles(profiles: List<Profile>): List<Long> {
    val delayArray = AtomicLongArray(profiles.size)
    profiles.mapIndexed { index, profile ->
        thread {
            try {
                val delay = measureTimeMillis {
                    val s = Socket().apply { keepAlive = false; }
                    s.connect(InetSocketAddress(profile.host, profile.remotePort), 5000)
                    s.close()
                }
                Timber.d("Delay for ${profile.name}: $delay")
                delayArray.addAndGet(index, delay)
            } catch (e: Exception) {
                Timber.e(e)
                delayArray.addAndGet(index, Long.MAX_VALUE)
            }
        }
    }.map { it.join() }
    return (0 until delayArray.length()).map { delayArray[it] }
}
