package com.mapbox.vision.replayer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.vision.common.BaseVisionActivity
import kotlinx.android.synthetic.main.fragment_sessions.*

class SessionsFragment : DialogFragment() {

    private lateinit var sessionListener: ISessionChangeListener

    interface ISessionChangeListener {
        fun onSessionChanged(dirName: String)
    }

    private val clickListener: (String) -> Unit = {
        sessionListener.onSessionChanged(it)
        close()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sessions, container)
        dialog?.setTitle("Choose session")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessions_recycler_view.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = SessionsAdapter(BaseVisionActivity.BASE_SESSION_PATH, clickListener)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionListener = context as ISessionChangeListener
    }

    private fun close() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }
}
