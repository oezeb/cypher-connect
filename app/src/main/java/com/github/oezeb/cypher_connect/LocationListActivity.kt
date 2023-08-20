package com.github.oezeb.cypher_connect

import android.os.Handler
import android.os.Looper
import com.github.oezeb.cypher_connect.design.Location
import com.github.oezeb.cypher_connect.design.LocationListDesign
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class LocationListActivity: LocationListDesign() {
    private val locationManager = LocationManager(this)
    private val handler = Handler(Looper.getMainLooper())

    override fun getLocations(): List<Location> {
        val locations = locationManager.getLocations()
        thread {
            for (location in locations) {
                if (location.code != null) {
                    val (delay, _) = locationManager.testLocation(location.code)
                    val speed = (-(delay.toDouble() / TEST_TIME_OUT) + 1) * 4
                    handler.post { updateSpeed(location.code!!, speed.roundToInt()) }
                }
            }
            handler.post { setProgressBarVisible(false) }
        }
        return listOf(Location(null, "Best Location")) + locations
    }
}