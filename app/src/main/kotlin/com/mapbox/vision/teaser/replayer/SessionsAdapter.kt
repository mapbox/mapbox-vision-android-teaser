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
import com.mapbox.vision.teaser.replayer.SessionsAdapter.AdapterItem.CameraItem
import com.mapbox.vision.teaser.replayer.SessionsAdapter.AdapterItem.SessionItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionsAdapter(private val context: Context,
                      private val basePath: String,
                      private val clickSessionListener: (String) -> Unit,
                      private val clickCameraListener: () -> Unit,
                      private val onActivateMultiSelectionListener: () -> Unit) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    private var items: MutableList<AdapterItem> = mutableListOf()
    private var cameraString = ""
    private val backgroundColorSelection: Int
    private val backgroundColorTransparent: Int
    var selectedItems = HashSet<String>()
        private set
    var isMultiSelection = false
        private set

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

    private fun bindTextInfo(holder: SessionViewHolder, item: AdapterItem) {
        holder.textSessionName.text = item.name
        if (item is SessionItem) {
            holder.textSessionDate.text = item.fileDate
        } else {
            holder.textSessionDate.text = ""
        }
    }

    private fun bindBackground(holder: SessionViewHolder, item: AdapterItem) {
        if (isMultiSelection) {
            when {
                item is CameraItem -> {
                    holder.itemView.isEnabled = false
                }
                selectedItems.contains(item.name) -> {
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
            if (item is CameraItem) {
                holder.itemView.isEnabled = true
            } else {
                holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
            }
            holder.iconChecked.visibility = INVISIBLE
        }
    }

    private fun bindViewHolderClickListener(holder: SessionViewHolder, item: AdapterItem) {
        if (!(item is CameraItem && isMultiSelection)) {
            holder.itemView.setOnClickListener {
                if (isMultiSelection) {
                    handleMultiSelectionClick(holder, item)
                }
                if (item is CameraItem) {
                    clickCameraListener.invoke()
                } else {
                    clickSessionListener.invoke(item.name)
                }
            }
        }
    }

    private fun handleMultiSelectionClick(holder: SessionViewHolder, item: AdapterItem) {
        if (selectedItems.contains(item.name)) {
            holder.itemView.setBackgroundColor(backgroundColorTransparent)
            holder.iconChecked.setImageResource(R.drawable.ic_not_checked)
            selectedItems.remove(item.name)
        } else {
            holder.itemView.setBackgroundColor(backgroundColorSelection)
            holder.iconChecked.setImageResource(R.drawable.ic_checked)
            selectedItems.add(item.name)
        }
    }

    private fun bindViewHolderLongClick(holder: SessionViewHolder, item: AdapterItem) {
        if (item !is CameraItem) {
            holder.itemView.setOnLongClickListener {
                if (isMultiSelection) {
                   handleMultiSelectionClick(holder, item)
                } else {
                    selectedItems.add(item.name)
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
            if (it is SessionItem) {
                selectedItems.add(it.name)
            }
        }
        notifyDataSetChanged()
    }

    fun updateSessionsList() {
        items.clear()
        items.add(CameraItem(cameraString))
        File(basePath).listFiles().forEach {
            val dateString = dateFormatter.format(it.lastModified())
            items.add(SessionItem(it.name, dateString))
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

    sealed class AdapterItem(val name: String) {
        class CameraItem(name: String): AdapterItem(name)
        class SessionItem(name: String, val fileDate: String): AdapterItem(name)
    }
}
