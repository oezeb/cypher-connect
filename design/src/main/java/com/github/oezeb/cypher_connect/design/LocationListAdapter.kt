package com.github.oezeb.cypher_connect.design

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class LocationListAdapter(private val data: List<Location>):
    RecyclerView.Adapter<LocationListAdapter.ViewHolder>() {
    companion object {
        var onClickListener: ((v: View, item: Location) -> Unit)? = null
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: AppCompatButton
        val serverList: RecyclerView
        val expandButton: ImageButton
        val serverListContainer: LinearLayout

        init {
            button = view.findViewById(R.id.location_button)
            serverList = view.findViewById(R.id.server_list)
            serverListContainer = view.findViewById(R.id.server_list_container)
            expandButton = view.findViewById(R.id.expand_button)

            serverList.layoutManager = LinearLayoutManager(view.context)
        }
    }

    fun updateSpeed(id: Long, speed: Int?) {
        for ((position, location) in data.withIndex()) {
            for (server in location.servers) {
                if (server.id == id) {
                    server.speed = speed
                    if (location.speed == null || (speed != null && speed > location.speed!!)) {
                        location.speed = speed
                    }
                    notifyItemChanged(position)
                    return
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_list_item, parent, false)
        val addDrawable = parent.context.getDrawable(R.drawable.baseline_add_24)
        val removeDrawable = parent.context.getDrawable(R.drawable.baseline_remove_24)

        var expanded = false
        return ViewHolder(view).apply {
            expandButton.setOnClickListener {
                if (expanded) {
                    serverListContainer.visibility = View.GONE
                    expandButton.setImageDrawable(addDrawable)
                    expanded = false
                } else {
                    serverListContainer.visibility = View.VISIBLE
                    expandButton.setImageDrawable(removeDrawable)
                    expanded = true
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val button = holder.button
        val item = data[position]

        button.text = item.name
        button.setOnClickListener { onClickListener?.invoke(it, data[position]) }
        holder.serverList.adapter = ServerListAdapter(item.servers)
        if (item.servers.isEmpty()) holder.expandButton.visibility = View.GONE

        val handler = Handler(Looper.getMainLooper())
        if (item.code != null) {
            thread {
                val flag = FlagCDN(button.context).getFlag(item.code)
                handler.post { button.setDrawableStart(flag) }
            }
        }
        thread { handler.post { updateSpeed(button, item.speed) } }
    }

    override fun getItemCount(): Int = data.size
}
