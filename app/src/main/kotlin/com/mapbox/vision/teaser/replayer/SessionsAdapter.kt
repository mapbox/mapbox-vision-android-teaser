package com.mapbox.vision.teaser.replayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.replayer.SessionsAdapter.AdapterItem.CameraItem
import com.mapbox.vision.teaser.replayer.SessionsAdapter.AdapterItem.SessionItem
import com.mapbox.vision.teaser.view.invisible
import com.mapbox.vision.teaser.view.show
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.HashSet

class SessionsAdapter(
    private val context: Context,
    private val basePath: String,
    private val clickSessionListener: (String) -> Unit,
    private val clickCameraListener: () -> Unit,
    private val onActivateMultiSelectionListener: () -> Unit
) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    }

    private val items: MutableList<AdapterItem> = mutableListOf()
    private var cameraString = ""
    private val backgroundSelectionColor: Int
    private val backgroundTransparentColor: Int
    val selectedItems = HashSet<String>()
    var isMultiSelection = false
        private set

    init {
        cameraString = context.getString(R.string.camera_text)
        backgroundSelectionColor = context.getColor(R.color.white_30_opacity)
        backgroundTransparentColor = context.getColor(R.color.fully_transparent)
        updateSessionsList(false)
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
        holder.textSessionDate.text = if (item is SessionItem) {
            item.fileDate
        } else {
            ""
        }
    }

    private fun bindBackground(holder: SessionViewHolder, item: AdapterItem) {
        if (isMultiSelection) {
            when {
                item is CameraItem -> {
                    holder.itemView.isEnabled = false
                    holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
                    holder.iconChecked.invisible()
                }
                selectedItems.contains(item.name) -> {
                    holder.itemView.setBackgroundColor(backgroundSelectionColor)
                    holder.iconChecked.setImageResource(R.drawable.ic_checked)
                    holder.iconChecked.show()
                }
                else -> {
                    holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
                    holder.iconChecked.setImageResource(R.drawable.ic_not_checked)
                    holder.iconChecked.show()
                }
            }
        } else {
            if (item is CameraItem) {
                holder.itemView.isEnabled = true
            }
            holder.itemView.background = context.getDrawable(R.drawable.session_info_custom_ripple)
            holder.iconChecked.invisible()
        }
    }

    private fun bindViewHolderClickListener(holder: SessionViewHolder, item: AdapterItem) {
        if (!(item is CameraItem) or !isMultiSelection) {
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
            holder.itemView.setBackgroundColor(backgroundTransparentColor)
            holder.iconChecked.setImageResource(R.drawable.ic_not_checked)
            selectedItems.remove(item.name)
        } else {
            holder.itemView.setBackgroundColor(backgroundSelectionColor)
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

    fun updateSessionsList(notifyDataSetChanged: Boolean) {
        items.clear()
        items.add(CameraItem(cameraString))
        val files = File(basePath).listFiles() ?: emptyArray()
        files.sortByDescending { it.lastModified() }
        files.forEach {
            val dateString = dateFormatter.format(it.lastModified())
            items.add(SessionItem(it.name, dateString))
        }
        if (notifyDataSetChanged) {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = items.size

    inner class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSessionName: TextView = view.findViewById(R.id.session_view_session_name)
        val textSessionDate: TextView = view.findViewById(R.id.session_view_session_date)
        val iconChecked: ImageView = view.findViewById(R.id.session_view_checked)
    }

    sealed class AdapterItem(val name: String) {
        class CameraItem(name: String) : AdapterItem(name)
        class SessionItem(name: String, val fileDate: String) : AdapterItem(name)
    }
}
