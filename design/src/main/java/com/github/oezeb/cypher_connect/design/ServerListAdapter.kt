package com.github.oezeb.cypher_connect.design

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class ServerListAdapter(private val data: List<Server>):
    RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {
    companion object {
        var onClickListener: ((v: View, item: Server) -> Unit)? = null
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: AppCompatButton

        init {
            button = view.findViewById(R.id.server_button)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val button = holder.button
        val item = data[position]

        button.text = item.name
        button.setOnClickListener { onClickListener?.invoke(it, data[position]) }

        val handler = Handler(Looper.getMainLooper())
        thread { handler.post { updateSpeed(button, item.speed) } }
    }

    override fun getItemCount(): Int = data.size
}
