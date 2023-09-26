package com.github.oezeb.cypher_connect

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.github.oezeb.cypher_connect.design.LocationListDesign
import com.github.shadowsocks.database.ProfileManager
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class LocationListActivity: LocationListDesign() {
    companion object {
        const val MAX_CONCURRENT_TEST = 10
    }

    private fun updateSpeed() {
        setProgressBarVisible(true)
        val profiles = ProfileManager.getActiveProfiles() ?: emptyList()

        val handler = Handler(Looper.getMainLooper())
        thread {
            val groups = profiles.chunked(MAX_CONCURRENT_TEST)
            for (group in groups) {
                val delayArray = testProfiles(group)
                val speedArray = delayArray.map { (-(it.toDouble() / TEST_TIME_OUT) + 1) * 4 }
                for (i in group.indices) {
                    handler.post { updateSpeed(group[i].id, speedArray[i].roundToInt()) }
                }
            }

            handler.post { setProgressBarVisible(false) }
        }
    }

    override fun getProfiles(): List<Pair<Long, String>> =
        ProfileManager.getActiveProfiles()?.map { Pair(it.id, it.name ?:"") } ?: emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateSpeed()
    }
}