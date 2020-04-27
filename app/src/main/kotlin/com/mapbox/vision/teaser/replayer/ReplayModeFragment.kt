package com.mapbox.vision.teaser.replayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.vision.teaser.MainActivity
import com.mapbox.vision.teaser.OnBackPressedListener
import com.mapbox.vision.teaser.R
import kotlinx.android.synthetic.main.fragment_replay_mode.*
import kotlinx.android.synthetic.main.fragment_replay_mode.back

class ReplayModeFragment : Fragment(), OnBackPressedListener {

    private var sessionsAdapter: SessionsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_replay_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackButton()
        initRecyclerViewSessions()
        initSelectAllButton()
        initEditButton()
        initSwipeRefreshLayout();
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
            sessionsAdapter = SessionsAdapter(requireContext(), sessionsPath, { onSourceItemClick(it) }, { onActivateMultiSelection() })
            recycler_sessions.adapter = sessionsAdapter
        } else {
            onBackPressed()
        }
    }

    private fun initSelectAllButton() {
        select_all.setOnClickListener {
            sessionsAdapter?.selectAll()
        }
    }

    private fun initEditButton() {
        edit_sessions_list.setOnClickListener {
            changeMultiSelection(true)
        }
    }

    private fun onActivateMultiSelection() {
        changeMultiSelection(true)
        setMultiSelectionTitle()
    }

    private fun initSwipeRefreshLayout() {
        swipe_refresh_sessions.setOnRefreshListener {
            sessionsAdapter?.updateSessionsList()
            swipe_refresh_sessions.isRefreshing = false
        }
    }

    private fun onSourceItemClick(fileName: String) {
        if (sessionsAdapter?.isMultiSelection == true) {
            setMultiSelectionTitle()
        }
    }

    private fun setMultiSelectionTitle() {
        sessionsAdapter?.apply {
            val count = getSelectedCount()
            replay_fragment_title.text = requireContext().resources.getQuantityString(R.plurals.selected_items, count, count);

        }
    }

    private fun changeMultiSelection(activate: Boolean) {
        if (activate) {
            sessionsAdapter?.activateMultiSelection()
            edit_sessions_list.visibility = GONE
            select_all.visibility = VISIBLE
            delete_sessions.visibility = VISIBLE
        } else {
            sessionsAdapter?.resetMultiSelection()
            edit_sessions_list.visibility = VISIBLE
            replay_fragment_title.setText(R.string.select_session_source)
            select_all.visibility = GONE
            delete_sessions.visibility = GONE
        }
    }

    override fun onBackPressed(): Boolean {
        if (sessionsAdapter?.isMultiSelection == true) {
            changeMultiSelection(false)
            return false
        }
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