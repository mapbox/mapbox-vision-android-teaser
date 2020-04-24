package com.mapbox.vision.teaser.replayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.replayer.SessionsAdapter.ItemType.CAMERA
import com.mapbox.vision.teaser.replayer.SessionsAdapter.ItemType.SESSION_FILE
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionsAdapter(context: Context,
                      basePath: String,
                      private val clickListener: (String) -> Unit) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {



    private var items: MutableList<Item> = mutableListOf()
    private var cameraString = ""

    init {
        cameraString = context.getString(R.string.camera_text)
        items.add(Item(CAMERA, cameraString, null))
        File(basePath).listFiles().forEach {
            val dateString = dateFormatter.format(it.lastModified())
            items.add(Item(SESSION_FILE, it.name, dateString))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_row_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = items[position]
        holder.textSessionName.text = item.itemName
        if (item.itemType == SESSION_FILE) {
            holder.textSessionDate.text = item.fileDate
        } else {
            holder.textSessionDate.text = ""
        }
        holder.itemView.setOnClickListener {
            clickListener.invoke(item.itemName)
        }
    }

    override fun getItemCount() = items.size

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSessionName: TextView = view.findViewById(R.id.session_view_session_name)
        val textSessionDate: TextView = view.findViewById(R.id.session_view_session_date)
    }

    companion object {

        @SuppressLint("ConstantLocale")
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    }


    enum class ItemType {
        CAMERA,
        SESSION_FILE
    }

    data class Item (val itemType: ItemType, val itemName:String, val fileDate: String?)
}
