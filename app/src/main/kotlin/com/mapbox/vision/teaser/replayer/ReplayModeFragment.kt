package com.mapbox.vision.teaser.replayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.vision.teaser.OnBackPressedListener
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.utils.FileUtils
import kotlinx.android.synthetic.main.fragment_replay_mode.*
import java.io.File
import java.lang.RuntimeException

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
        initSwipeRefreshLayout()
        initDeleteSessionsButton()
        initDoneEditButton()
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun initRecyclerViewSessions() {
        recycler_sessions.layoutManager = LinearLayoutManager(requireContext())
        val sessionsPath = arguments?.getString(ARG_PARAM_SESSIONS_PATH)
        if (sessionsPath != null) {
            sessionsAdapter = SessionsAdapter(
                    requireContext(),
                    sessionsPath,
                    { onSessionClick(it) },
                    { onCameraClick() },
                    { onActivateMultiSelection() })
            recycler_sessions.adapter = sessionsAdapter
        } else {
            requireActivity().onBackPressed()
        }
    }

    private fun initSelectAllButton() {
        select_all.setOnClickListener {
            sessionsAdapter?.apply {
                selectAll()
                val count = getSelectedCount()
                replay_fragment_title.text = requireContext().resources.getQuantityString(R.plurals.selected_items, count, count)
            }
        }
    }

    private fun initEditButton() {
        edit_sessions_list.setOnClickListener {
            changeMultiSelection(true)
        }
    }

    private fun initSwipeRefreshLayout() {
        swipe_refresh_sessions.setOnRefreshListener {
            sessionsAdapter?.updateSessionsList()
            swipe_refresh_sessions.isRefreshing = false
        }
    }

    private fun initDoneEditButton() {
        done_edit.setOnClickListener {
            changeMultiSelection(false)
        }
    }

    private fun onActivateMultiSelection() {
        changeMultiSelection(true)
        setMultiSelectionTitle()
    }


    private fun initDeleteSessionsButton() {
        delete_sessions.setOnClickListener {
            sessionsAdapter?.let {
                val sessionsPath = arguments?.getString(ARG_PARAM_SESSIONS_PATH)
                if (it.isMultiSelection && sessionsPath != null) {
                    try {
                        for (session in it.selectedItems) {
                            val sessionsFolder = File("$sessionsPath/$session")
                            FileUtils.deleteFolder(sessionsFolder)
                        }
                    } catch (ex: RuntimeException) {
                        Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
                    }
                    it.updateSessionsList()
                    changeMultiSelection(false)
                }
            }
        }
    }

    private fun onSessionClick(fileName: String) {
        if (sessionsAdapter?.isMultiSelection == true) {
            setMultiSelectionTitle()
        } else {
            val listener = requireActivity()
            if (listener is OnClickModeItemListener) {
                listener.onClickSessionItem(fileName)
                requireActivity().onBackPressed()
            }
        }
    }

    private fun onCameraClick() {
        val listener = requireActivity()
        if (listener is OnClickModeItemListener) {
            listener.onClickCamera()
            requireActivity().onBackPressed()
        }
    }

    private fun setMultiSelectionTitle() {
        sessionsAdapter?.apply {
            val count = getSelectedCount()
            replay_fragment_title.text = requireContext().resources.getQuantityString(R.plurals.selected_items, count, count)
        }
    }

    private fun changeMultiSelection(activate: Boolean) {
        if (activate) {
            sessionsAdapter?.activateMultiSelection()
            activateMultiSelectionVisibilityState(true)
        } else {
            sessionsAdapter?.resetMultiSelection()
            replay_fragment_title.setText(R.string.select_session_source)
            activateMultiSelectionVisibilityState(false)
        }
    }

    private fun activateMultiSelectionVisibilityState(visible: Boolean) {
        val visibilityState = if (visible) VISIBLE else GONE
        edit_sessions_list.visibility = visibilityState
        select_all.visibility = visibilityState
        delete_sessions.visibility = visibilityState
        record_session.visibility = visibilityState
        back_button.visibility = visibilityState
        done_edit.visibility = visibilityState
    }

    override fun onBackPressed(): Boolean {
        if (sessionsAdapter?.isMultiSelection == true) {
            changeMultiSelection(false)
            return false
        }
        return true
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

    interface OnClickModeItemListener {

        fun onClickSessionItem(sessionName: String)

        fun onClickCamera()
    }
}