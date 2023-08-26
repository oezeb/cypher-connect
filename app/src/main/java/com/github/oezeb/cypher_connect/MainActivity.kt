package com.github.oezeb.cypher_connect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.view.View
import androidx.preference.PreferenceDataStore
import com.github.oezeb.cypher_connect.design.Location
import com.github.oezeb.cypher_connect.design.MainDesign
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.aidl.TrafficStats
import com.github.shadowsocks.bg.BaseService.State
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import kotlin.concurrent.thread

class MainActivity : MainDesign(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    companion object {
        var stateListener: ((state: State, profileName: String?) -> Unit)? = null
    }

    private val locationManager = LocationManager(this)
    private var currentLocation: Location = Location(null, "Best Location")
    private val connection = ShadowsocksConnection(true)
    private var state = State.Idle
    private val handler = Handler(Looper.getMainLooper())

    override val launchLocationListActivityIntent: Intent
        get() = Intent(this, LocationListActivity::class.java)

    override fun getCurrentIp(): String = currentIP(this) ?: "Error"

    override fun onClickConnectButton(v: View) {
        if (state == State.Connected) {
            if (state.canStop) Core.stopService()
            showStoppingStatePage()
        } else if (state == State.Stopped || state == State.Idle) {
            thread {
                val (_, id) = locationManager.testLocation(currentLocation.code)
                Core.switchProfile(id)
                Core.startService()
            }
            showConnectingStatePage()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        if (state == State.Connected) {
            thread {
                val (_, id) = locationManager.testLocation(currentLocation.code)
                Core.switchProfile(id)
                Core.reloadService()
            }
            showConnectingStatePage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            connection.connect(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        DataStore.publicStore.registerChangeListener(this)
        thread { syncProfiles(this) }

        setCurrentLocation(currentLocation)
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
