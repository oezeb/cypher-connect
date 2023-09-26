package com.github.oezeb.cypher_connect.design

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

abstract class MainDesign: AppCompatActivity() {
    private lateinit var stateView1: TextView // Top
    private lateinit var stateView2: TextView // Center - On Button - All Caps
    private lateinit var stateView3: TextView // Below Button

    private lateinit var durationView: TextView
    private lateinit var connectButton: ImageButton
    private lateinit var currentIp: TextView

    private lateinit var selectLocationText: TextView
    private lateinit var trafficUp: TextView
    private lateinit var trafficDown: TextView
    private lateinit var currentLocationButton: AppCompatButton

    private lateinit var loadingProgressBar: FrameLayout

    private val startLocationListActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    val code = it.getStringExtra("code")
                    val id = it.getLongExtra("id", -1)

                    val name = if (code == null) "Best Location" else {
                        val codeMap = FlagCDN(this).getCodes()
                        codeMap.getOrDefault(code, null) as String?
                    }

                    if (name != null) {
                        setCurrentLocation(Location(code, name))
                        onProfileChanged(id)
                    }
                }
            }
        }

    abstract val launchLocationListActivityIntent: Intent
    abstract fun getCurrentIp(): String
    abstract fun onClickConnectButton(v: View)
    abstract fun onProfileChanged(id: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_design)

        stateView1 = findViewById(R.id.state_text_1)
        stateView2 = findViewById(R.id.state_text_2)
        stateView3 = findViewById(R.id.state_text_3)

        durationView = findViewById(R.id.duration)
        connectButton = findViewById(R.id.connect_button)
        currentIp = findViewById(R.id.current_ip)

        selectLocationText = findViewById(R.id.select_location_text)
        trafficUp = findViewById(R.id.traffic_up)
        trafficDown = findViewById(R.id.traffic_down)
        currentLocationButton = findViewById(R.id.location_button)

        loadingProgressBar = findViewById(R.id.loading)

        connectButton.setOnClickListener {
            onClickConnectButton(it)
        }
        currentLocationButton.setOnClickListener {
            launchLocationListActivity()
        }

        findViewById<ImageButton>(R.id.leading).apply { visibility = View.GONE }
        findViewById<ImageButton>(R.id.expand_button).apply { visibility = View.GONE }

        setCurrentLocation(Location(null, "Best Location"))
        showNotConnectedStatePage()

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this)
        findViewById<AdView>(R.id.bannerAdView).apply { loadAd(AdRequest.Builder().build()) }
    }

    private val handler = Handler(Looper.getMainLooper())

    open fun launchLocationListActivity() {
        startLocationListActivity.launch(launchLocationListActivityIntent)
    }

    private fun setCurrentLocation(location: Location) {
        thread {
            if (location.code != null) {
                val flag = FlagCDN(this).getFlag(location.code)
                Handler(Looper.getMainLooper()).post {
                    currentLocationButton.setDrawableStart(flag)
                }
            } else {
                currentLocationButton.setDrawableStart(getDrawable(R.drawable.flag_placeholder))
            }
        }
        currentLocationButton.text = location.name
        currentLocationButton.setDrawableEnd(R.drawable.baseline_keyboard_arrow_down_24)
    }

    private fun updateCurrentIp() {
        currentIp.text = getString(R.string.current_ip, "...")
        thread {
            val ip = getString(R.string.current_ip, getCurrentIp())
            handler.post { currentIp.text = ip }
        }
    }

    private var startTimeMillis: Long = 0L
    private fun restartDuration() {
        startTimeMillis = System.currentTimeMillis()
        updateDuration()
    }

    fun showLoadingProgressBar()  { loadingProgressBar.visibility = View.VISIBLE }
    fun hideLoadingProgressBar() { loadingProgressBar.visibility = View.GONE }

    fun showConnectingStatePage() {
        hideViews(listOf(durationView, stateView2, currentIp, trafficUp, trafficDown))
        showViews(listOf(stateView1, stateView3, selectLocationText))

        stateView1.text = getString(R.string.not_connected)
        stateView3.text = getString(R.string.connecting)
        stateView3.setDrawableStart(R.drawable.icon_ionic_md_refresh)

        connectButton.setImageDrawable(getDrawable(R.drawable.ic_button_2))
//        connectButton.isEnabled = false

        selectLocationText.alpha = 0.2F
        currentLocationButton.alpha = 0.2F
        currentLocationButton.isEnabled = false
    }

    fun showConnectedStatePage() {
        hideViews(listOf(stateView1, stateView3, selectLocationText))
        showViews(listOf(durationView, stateView2, currentIp, trafficUp, trafficDown))

        stateView2.text = getString(R.string.connected)
        updateCurrentIp()

        connectButton.setImageDrawable(getDrawable(R.drawable.ic_button_3))
        connectButton.isEnabled = true

        currentLocationButton.alpha = 1.0F
        currentLocationButton.isEnabled = true

        updateTraffic(0, 0)
        restartDuration()
    }

    fun showStoppingStatePage() {
        hideViews(listOf(durationView, stateView2, currentIp, trafficUp, trafficDown))
        showViews(listOf(stateView1, stateView3, selectLocationText))

        stateView1.text = getString(R.string.connected)
        stateView3.text = getString(R.string.stopping)
        stateView3.setDrawableStart(null)

        connectButton.setImageDrawable(getDrawable(R.drawable.ic_button))
//        connectButton.isEnabled = false

        selectLocationText.alpha = 0.2F
        currentLocationButton.alpha = 0.2F
        currentLocationButton.isEnabled = false
    }

    fun showNotConnectedStatePage() {
        hideViews(listOf(durationView, stateView2, currentIp, trafficUp, trafficDown))
        showViews(listOf(stateView1, stateView3, selectLocationText))

        stateView1.text = getString(R.string.not_connected)
        stateView3.text = getString(R.string.tap_to_connect)
        stateView3.setDrawableStart(null)

        connectButton.setImageDrawable(getDrawable(R.drawable.ic_button))
        connectButton.isEnabled = true

        selectLocationText.alpha = 1.0F
        currentLocationButton.alpha = 1.0F
        currentLocationButton.isEnabled = true
    }
  
    fun updateTrafficUp(txRate: Long) = updateTraffic(trafficUp, txRate)
    fun updateTrafficDown(rxRate: Long) = updateTraffic(trafficDown, rxRate)
    fun updateTraffic(txRate: Long, rxRate: Long) {
        updateTrafficUp(txRate)
        updateTrafficDown(rxRate)
    }

    /// rate: Bytes per second
    private fun updateTraffic(view: TextView, rate: Long) {
        val (convertedRate, unit) = when {
            rate < 1000 -> rate.toDouble() to "B/s"
            rate < 1000000 -> rate.toDouble() / 1000.0 to "kB/s"
            rate < 1000000000 -> rate.toDouble() / 1000000.0 to "MB/s"
            else -> rate.toDouble() / 1000000000.0 to "GB/s"
        }

         val rateString = if (convertedRate % 1 == 0.0) {
             convertedRate.toInt().toString()
         } else {
             String.format("%.2f", convertedRate)
         } + " $unit"

        handler.post { view.text = rateString }
    }

    private fun updateDuration() {
        if (startTimeMillis == 0L || !durationView.isVisible) return

        val currentTimeMillis = System.currentTimeMillis()
        val elapsedMillis = currentTimeMillis - startTimeMillis

        val hours = elapsedMillis / 3600000
        val minutes = (elapsedMillis % 3600000) / 60000
        val seconds = (elapsedMillis % 60000) / 1000

        val formattedDuration = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        durationView.text = formattedDuration

        // Schedule the next update after 1 second
        handler.postDelayed({ updateDuration() }, 1000)
    }

    private fun hideViews(views: Collection<View>) = views.forEach {
        if (it.isVisible) it.visibility = View.INVISIBLE
    }

    private fun showViews(views: Collection<View>) = views.forEach {
        if (!it.isVisible) it.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove any pending callbacks when the activity is destroyed
        // specially the timer callbacks
        handler.removeCallbacksAndMessages(null)
    }

    fun showAbout(view: View) = startActivity(Intent(this, About::class.java))
    fun back(view: View) {}
    fun onClickLoading(view: View) {}
}
