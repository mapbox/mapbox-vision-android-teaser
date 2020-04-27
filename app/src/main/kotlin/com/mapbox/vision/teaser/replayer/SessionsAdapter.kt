package com.mapbox.vision.teaser.replayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.replayer.SessionsAdapter.ItemType.CAMERA
import com.mapbox.vision.teaser.replayer.SessionsAdapter.ItemType.SESSION_FILE
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionsAdapter(private val context: Context,
                      private val basePath: String,
                      private val clickListener: (String) -> Unit,
                      private val onActivateMultiSelectionListener: () -> Unit) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    private var items: MutableList<SourceItem> = mutableListOf()
    private var cameraString = ""
    private val backgroundColorSelection: Int
    private val backgroundColorTransparent: Int
    private var selectedItems = HashSet<String>()
    var isMultiSelection = false

    init {
        cameraString = context.getString(R.string.camera_text)
        backgroundColorSelection = context.getColor(R.color.white_30_opacity)
        backgroundColorTransparent = context.getColor(R.color.fully_transparent)
        updateSessionsList()
    }

    fun getSelectedCount() = selectedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_row_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = items[position]
        bindTextInfo(holder, item)
        bindBackground(holder, item)
        bindViewHolderClickListener(holder, item)
        bindViewHolderLongClick(holder, item)
    }

    private fun bindTextInfo(holder: SessionViewHolder, item: SourceItem) {
        holder.textSessionName.text = item.itemName
        if (item.itemType == SESSION_FILE) {
            holder.textSessionDate.text = item.fileDate
        } else {
            holder.textSessionDate.text = ""
        }
    }

    private fun bindBackground(holder: SessionViewHolder, item: SourceItem) {
        if (isMultiSelection) {
            when {
                item.itemType == CAMERA -> {
                    holder.itemView.isEnabled = false
                }
                selectedItems.contains(item.itemName) -> {
                    holder.itemView.setBackgroundColor(backgroundColorSelection)
                    holder.iconChecked.setImageResource(R.drawable.ic_checked)
                    holder.iconChecked.visibility = VISIBLE
                }
                else -> {
                    holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
                    holder.iconChecked.setImageResource(R.drawable.ic_not_checked)
                    holder.iconChecked.visibility = VISIBLE
                }
            }
        } else {
            if (item.itemType == CAMERA) {
                holder.itemView.isEnabled = true
            } else {
                holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
                holder.iconChecked.visibility = INVISIBLE
            }
        }
    }

    private fun bindViewHolderClickListener(holder: SessionViewHolder, item: SourceItem) {
        if (!(item.itemType == CAMERA && isMultiSelection)) {
            holder.itemView.setOnClickListener {
                if (isMultiSelection) {
                    handleMultiSelectionClick(holder, item)
                }
                clickListener.invoke(item.itemName)
            }
        }
    }

    private fun handleMultiSelectionClick(holder: SessionViewHolder, item: SourceItem) {
        if (selectedItems.contains(item.itemName)) {
            holder.itemView.setBackgroundColor(backgroundColorTransparent)
            holder.iconChecked.setImageResource(R.drawable.ic_not_checked)
            selectedItems.remove(item.itemName)
        } else {
            holder.itemView.setBackgroundColor(backgroundColorSelection)
            holder.iconChecked.setImageResource(R.drawable.ic_checked)
            selectedItems.add(item.itemName)
        }
    }

    private fun bindViewHolderLongClick(holder: SessionViewHolder, item: SourceItem) {
        if (item.itemType != CAMERA) {
            holder.itemView.setOnLongClickListener {
                if (isMultiSelection) {
                   handleMultiSelectionClick(holder, item)
                } else {
                    selectedItems.add(item.itemName)
                    onActivateMultiSelectionListener.invoke()
                }
                true
            }
        }
    }

    fun activateMultiSelection() {
        isMultiSelection = true
        notifyDataSetChanged()
    }

    fun resetMultiSelection() {
        isMultiSelection = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        items.forEach {
            selectedItems.add(it.itemName)
        }
        notifyDataSetChanged()
    }

    fun updateSessionsList() {
        items.clear()
        items.add(SourceItem(CAMERA, cameraString, null))
        File(basePath).listFiles().forEach {
            val dateString = dateFormatter.format(it.lastModified())
            items.add(SourceItem(SESSION_FILE, it.name, dateString))
        }
    }

    override fun getItemCount() = items.size

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSessionName: TextView = view.findViewById(R.id.session_view_session_name)
        val textSessionDate: TextView = view.findViewById(R.id.session_view_session_date)
        val iconChecked: ImageView = view.findViewById(R.id.session_view_checked)
    }

    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    }

    enum class ItemType {
        CAMERA,
        SESSION_FILE
    }

    data class SourceItem (val itemType: ItemType, val itemName:String, val fileDate: String?)
}
