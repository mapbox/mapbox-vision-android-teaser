package com.mapbox.vision.teaser.replayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.vision.teaser.MainActivity
import com.mapbox.vision.teaser.OnBackPressedListener
import com.mapbox.vision.teaser.R
import kotlinx.android.synthetic.main.fragment_replay_mode.*

class ReplayModeFragment : Fragment(), OnBackPressedListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_replay_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackButton()
        initRecyclerViewSessions()
    }

    private fun initBackButton() {
        back.setOnClickListener {
            val parentActivity = requireActivity()
            if (parentActivity is MainActivity) {
                parentActivity.onBackPressed()
            }
        }
    }

    private fun initRecyclerViewSessions() {
        recycler_sessions.layoutManager = LinearLayoutManager(requireContext())
        val sessionsPath = arguments?.getString(ARG_PARAM_SESSIONS_PATH)
        if (sessionsPath != null) {
            recycler_sessions.adapter = SessionsAdapter(requireContext(), sessionsPath) {
                //TODO: Add logic
            }
        } else {
            onBackPressed()
        }
    }

    override fun onBackPressed(): Boolean {
        // TODO: Add multiselection mode reset
        return true;
    }

    companion object{

        val TAG: String = ReplayModeFragment::class.java.simpleName
        private const val ARG_PARAM_SESSIONS_PATH = "ARG_PARAM_SESSIONS_PATH"

        fun newInstance(path: String): ReplayModeFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PARAM_SESSIONS_PATH, path)
            val fragment = ReplayModeFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}