package com.mapbox.vision.teaser.replayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.vision.teaser.R
import java.io.File

class SessionsAdapter(basePath: String, val clickListener: (String) -> Unit) :
    RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    private var sessions: MutableList<String> = mutableListOf()

    init {
        File(basePath)
            .listFiles()
            .forEach { sessions.add(it.name) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.textView.text = sessions[position]
    }

    override fun getItemCount() = sessions.size

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tv_session_name)

        init {
            textView.setOnClickListener {
                clickListener.invoke(sessions[adapterPosition])
            }
        }
    }
}
