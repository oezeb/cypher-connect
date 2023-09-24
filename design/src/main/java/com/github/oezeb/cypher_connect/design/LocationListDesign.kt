package com.github.oezeb.cypher_connect.design

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

abstract class LocationListDesign : AppCompatActivity() {
    companion object {
        private val SPEED_ICONS = listOf(
            R.drawable.cellular_connection,
            R.drawable.cellular_connection_1,
            R.drawable.cellular_connection_2,
            R.drawable.cellular_connection_3,
            R.drawable.cellular_connection_4
        )

        private fun updateSpeed(view: View, speed: Int?) {
            if (speed == null) return

            val index = when {
                speed < 0 -> 0
                speed >= SPEED_ICONS.size -> SPEED_ICONS.size - 1
                else -> speed
            }

            (view as TextView).setDrawableEnd(view.context.getDrawable(SPEED_ICONS[index]))
        }

        private val handler = Handler(Looper.getMainLooper())
    }

    class ListAdapter(val dataSet: List<Location>) :
        RecyclerView.Adapter<ListAdapter.ViewHolder>() {
        companion object {
            var onClickListener: ((v: View, item: Location) -> Unit)? = null
        }
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val button: AppCompatButton

            init {
                button = view as AppCompatButton
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.location_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val button = holder.button
            val item = dataSet[position]

            button.text = item.name
            thread {
                if (item.code != null) {
                    val flag = FlagCDN(button.context).getFlag(item.code)
                    handler.post { button.setDrawableStart(flag) }
                }
            }
            thread {
                handler.post { updateSpeed(button, item.speed) }
            }
            holder.button.setOnClickListener { onClickListener?.invoke(it, dataSet[position]) }
        }

        override fun getItemCount(): Int = dataSet.size
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var listView: RecyclerView

    abstract fun getLocations(): List<Location>

    fun updateSpeed(code: String, speed: Int) {
        val adapter = listView.adapter as ListAdapter? ?: return

        for (index in 0 until adapter.itemCount) {
            if (adapter.dataSet[index].code == code) {
                adapter.dataSet[index].speed = speed
                adapter.notifyItemChanged(index)
            }
        }
    }

    fun setProgressBarVisible(isVisible: Boolean) {
        progressBar.isVisible = isVisible
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.location_list)

        thread {
            val locations = getLocations()
            val adapter = ListAdapter(locations)
            handler.post { listView.adapter = adapter }
        }

        progressBar = findViewById(R.id.progress_bar)
        listView = findViewById(R.id.recycle_view)
        listView.layoutManager = LinearLayoutManager(this)
        listView.adapter = ListAdapter(emptyList())

        ListAdapter.onClickListener = { _, item ->
            val intent = Intent()
            intent.putExtra("location", item)
            setResult(Activity.RESULT_OK, intent)
            finish()
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