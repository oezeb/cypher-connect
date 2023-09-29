package com.github.oezeb.cypher_connect.design

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

abstract class LocationListDesign : AppCompatActivity() {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
    }

    /**
     * Return list of profiles' (id, name)
     */
    abstract fun getProfiles(): List<Pair<Long, String>>

    private lateinit var progressBar: ProgressBar
    private lateinit var locationListView: RecyclerView

    private fun getLocations(): List<Location> {
        val profiles = getProfiles()
        val codeMap = FlagCDN(this).getCodes()
        // each profile.name first two letters is the country code
        val locations = profiles.groupBy { (_, name) -> if (name.length >= 2) name.substring(0, 2).lowercase() else "" }
            .filter { codeMap.containsKey(it.key) }
            .map { (code, profiles) ->
                val servers = profiles.map { (id, name) -> Server(id, name) }
                Location(code, codeMap[code] as String, servers)
            }
        return listOf(Location(null, "Best Location")) + locations
    }

    private fun onClickItem(code: String?, id: Long) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("code", code)
            putExtra("id", id)
        })
        finish()
    }

    fun updateSpeed(id: Long, speed: Int?) =
        (locationListView.adapter as LocationListAdapter?)?.updateSpeed(id, speed)

    fun setProgressBarVisible(isVisible: Boolean) {
        progressBar.isVisible = isVisible
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.location_list)

        progressBar = findViewById(R.id.progress_bar)
        locationListView = findViewById(R.id.recycle_view)
        locationListView.layoutManager = LinearLayoutManager(this)
        locationListView.adapter = LocationListAdapter(emptyList())

        thread {
            val locations = getLocations()
            val adapter = LocationListAdapter(locations)
            handler.post { locationListView.adapter = adapter }
        }

        ServerListAdapter.onClickListener = { _, item ->
            onClickItem(item.name?.substring(0, 2)?.lowercase(), item.id)
        }

        LocationListAdapter.onClickListener = { _, item ->
            // return the server with the highest speed
            val server = item.servers.maxByOrNull { it.speed ?: 0 }
            onClickItem(item.code, server?.id ?: -1)
        }


        findViewById<LinearLayout>(R.id.actions).apply { visibility = View.GONE }

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this)
        findViewById<AdView>(R.id.bannerAdView).apply { loadAd(AdRequest.Builder().build()) }
    }

    fun back(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
    fun showAbout(view: View) {}
}