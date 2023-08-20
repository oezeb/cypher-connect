package com.github.oezeb.cypher_connect

import android.content.Context
import android.util.Log
import com.github.oezeb.cypher_connect.design.FlagCDN
import com.github.oezeb.cypher_connect.design.Location
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicIntegerArray
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


const val TEST_TIME_OUT = 5000
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
    }.map { it.join() }
    return (0 until delayArray.length()).map { delayArray[it] }
}

fun currentIP(): String? {
    val currentIPProviders = listOf(
        "https://ipinfo.io/ip",
        "https://api.ipify.org",
        "https://icanhazip.com",
        "https://ipecho.net/plain",
        "https://indent.me",
    )

    for (url in currentIPProviders) {
        try { return URL(url).readText() }
        catch (e: Exception) { Log.d("Current IP", "url", e) }
    }
    return null
}


class LocationManager(private val context: Context)  {
    private val flagCDN = FlagCDN(context)

    fun getLocations(): List<Location> {
        val profiles = ProfileManager.getActiveProfiles() ?: emptyList()
        val codeMap = flagCDN.getCodes()
        val codes = mutableSetOf<String>()
        for(profile in profiles) {
            val name = profile.name?.lowercase()?.take(2) ?: ""
            if (!codes.contains(name) && codeMap.contains(name)) codes.add(name)
        }

        return codes.map {  code -> Location(code, codeMap[code] as String) }
    }

    private fun getProfiles(code: String?): List<Profile> {
        val profiles = ProfileManager.getActiveProfiles() ?: emptyList()
        return if (code == null) {
            profiles
        } else {
            profiles.filter { profile -> profile.name?.lowercase() == code.lowercase() }
        }
    }

    /**
     * Return Pair<delay, Profile.id>
     */
    fun testLocation(code: String?): Pair<Int, Long> {
        val profiles = getProfiles(code)
        val delayArray = testProfiles(profiles)
        val delay = delayArray.min()
        val minIndex = delayArray.indexOf(delay)
        return delay to profiles[minIndex].id
    }
}