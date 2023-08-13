package com.github.oezeb.cypher_connect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.widget.ImageButton
import android.widget.TextView
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService.State
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    companion object {
        var stateListener: ((state: State, profileName: String?) -> Unit)? = null
    }

    private val connection = ShadowsocksConnection(true)
    private var state = State.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button =  findViewById<ImageButton>(R.id.connect_button).apply {
            setOnClickListener { toggle() }
        }
        val stateView = findViewById<TextView>(R.id.state).apply { text = state.name }
        val textView = findViewById<TextView>(R.id.text)

        stateListener = {state, profileName ->
            this.state = state

            button.isEnabled = !(state == State.Connecting || state == State.Stopping)
            if (profileName != null) textView.text = profileName
            stateView.text = state.name
        }

        try {
            connection.connect(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        DataStore.publicStore.registerChangeListener(this)
        thread { syncProfiles() }
    }

    private fun toggle() {
        if (state.canStop) Core.stopService()
        else {
            changeState(State.Connecting)
            thread {
                val profiles = ProfileManager.getActiveProfiles() ?: emptyList()
                val delayArray = testProfiles(profiles)
                val minIndex = delayArray.indexOf(delayArray.min())
                if (minIndex != -1) {
                    switchProfile(profiles[minIndex].id)
                    Core.startService()
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

    private fun switchProfile(id: Long) {
        Core.switchProfile(id)
        if (state == State.Connected) {
            Core.reloadService()
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
