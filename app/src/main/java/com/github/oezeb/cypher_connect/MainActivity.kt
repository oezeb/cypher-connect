package com.github.oezeb.cypher_connect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.view.View
import androidx.preference.PreferenceDataStore
import com.github.oezeb.cypher_connect.design.Http
import com.github.oezeb.cypher_connect.design.MainDesign
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.aidl.TrafficStats
import com.github.shadowsocks.bg.BaseService.State
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import timber.log.Timber
import kotlin.concurrent.thread

class MainActivity : MainDesign(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    companion object {
        var stateListener: ((state: State, profileName: String?) -> Unit)? = null
        const val MAX_CONCURRENT_TEST = 10
    }

    private fun getBestProfile(): Profile? {
        val profiles = ProfileManager.getActiveProfiles() ?: emptyList()

        val groups = profiles.chunked(MAX_CONCURRENT_TEST)
        var best = Pair<Profile?, Int>(null, Int.MAX_VALUE)
        for (group in groups) {
            val delayArray = testProfiles(group)
            val minDelayIndex = delayArray.indexOfFirst { it == delayArray.minOrNull() }
            if (best.second > delayArray[minDelayIndex]) {
                best = group[minDelayIndex] to delayArray[minDelayIndex]
            }
        }
        return best.first
    }

    private val connection = ShadowsocksConnection(true)
    private var currentProfileId = -1L
    private var state = State.Idle
    private val syncProfilesThread = thread(false) { syncProfiles() }

    private var bestProfile: Profile? = null
    private var bestProfileThread = thread(false) { bestProfile = getBestProfile() }

    private val handler = Handler(Looper.getMainLooper())

    override val launchLocationListActivityIntent: Intent
        get() = Intent(this, LocationListActivity::class.java)

    override fun getCurrentIp(): String {
        for (url in resources.getStringArray(R.array.current_ip_providers)) {
            try {
                val res = Http.get(url, timeout = 1000)
                if (res.ok) {
                    val text = res.text
                    if (text.isNotEmpty()) return text
                } else {
                    Timber.e("GET $url - code ${res.code} - data: ${res.text}")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return "Error"
    }

    override fun onClickConnectButton(v: View) {
        if (state == State.Connected) {
            if (state.canStop) Core.stopService()
        } else if (state == State.Stopped || state == State.Idle) {
            if (currentProfileId < 0) {
                thread {
                    handler.post { showLoadingProgressBar() }
                    if (bestProfileThread.isAlive) bestProfileThread.join()
                    bestProfile?.let {
                        Core.switchProfile(it.id)
                    }
                    handler.post { hideLoadingProgressBar() }
                }
            }
            Core.startService()
        }
    }

    override fun onProfileChanged(id: Long) {
        if (id < 0) {
            thread {
                handler.post { showLoadingProgressBar() }
                if (bestProfileThread.isAlive) bestProfileThread.join()
                bestProfile?.let {
                    Core.switchProfile(it.id)
                    if (state == State.Connected) Core.reloadService()
                }

                bestProfileThread = thread { bestProfile = getBestProfile() }
                handler.post { hideLoadingProgressBar() }
            }
        } else if (currentProfileId != id) {
            Core.switchProfile(id)
            if (state == State.Connected) Core.reloadService()
        }

        currentProfileId = id
    }

    override fun launchLocationListActivity() {
        showLoadingProgressBar()
        thread {
            if (syncProfilesThread.isAlive) syncProfilesThread.join()
            handler.post {
                hideLoadingProgressBar()
                super.launchLocationListActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncProfilesThread.start()
        thread {
            syncProfilesThread.join()
            bestProfileThread.start()
        }

        try {
            connection.connect(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        DataStore.publicStore.registerChangeListener(this)

        stateListener = {state, _ ->
            handler.post {
                when (state) {
                    State.Connecting -> showConnectingStatePage()
                    State.Connected -> showConnectedStatePage()
                    State.Stopping -> showStoppingStatePage()
                    else -> showNotConnectedStatePage()
                }
            }
        }
    }

    private fun syncProfiles() {
        for (url in resources.getStringArray(R.array.proxies_url)) {
            try {
                val res = Http.get(url, timeout = 1000)
                val text = if (res.ok) res.text else ""
                ProfileManager.getAllProfiles()?.forEach { ProfileManager.delProfile(it.id) }
                Profile.findAllUrls(text).forEach { ProfileManager.createProfile(it) }
                break
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun changeState(state: State, profileName: String? = null) {
        this.state = state
        stateListener?.invoke(state, profileName)
    }

    override fun stateChanged(state: State, profileName: String?, msg: String?) =
        changeState(if (msg == null) state else State.Idle, profileName)

    override fun onServiceConnected(service: IShadowsocksService) = changeState(try {
        State.values()[service.state]
    } catch (_: RemoteException) {
        State.Idle
    })

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
        if (state != State.Stopping) {
            if (profileId != 0L) {
                updateTraffic(stats.txRate, stats.rxRate)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }
}
