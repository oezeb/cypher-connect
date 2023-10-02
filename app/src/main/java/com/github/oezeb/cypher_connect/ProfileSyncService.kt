package com.github.oezeb.cypher_connect

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.oezeb.cypher_connect.design.Http
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import timber.log.Timber
import kotlin.concurrent.thread

class ProfileSyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread {
            for (url in resources.getStringArray(R.array.proxies_url)) {
                try {
                    syncProfiles(url); break
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        return START_STICKY
    }

    companion object {
        fun syncProfiles(url: String) {
            val res = Http.get(url, timeout = 1000)
            val text = if (res.ok) res.text else null ?: throw Exception("Failed to fetch $url")

            val localProfiles = ProfileManager.getAllProfiles() ?: emptyList()
            val localProfilesMap = localProfiles.associateBy { it.host to it.remotePort }
            val remoteProfiles = Profile.findAllUrls(text)
            val remoteProfilesMap = remoteProfiles.associateBy { it.host to it.remotePort }

            val toDelete =
                localProfiles.filter { remoteProfilesMap[it.host to it.remotePort] == null }
            val toCreate =
                remoteProfiles.filter { localProfilesMap[it.host to it.remotePort] == null }
            for (profile in toDelete) {
                ProfileManager.delProfile(profile.id)
            }
            for (profile in toCreate) {
                ProfileManager.createProfile(profile)
            }

            for (profile in remoteProfiles) {
                val localProfile = localProfilesMap[profile.host to profile.remotePort]
                if (localProfile != null) {
                    profile.id = localProfile.id
                    ProfileManager.updateProfile(profile)
                }
            }
        }
    }
}
