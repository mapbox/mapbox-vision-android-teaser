package com.mapbox.vision.teaser.replayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.vision.teaser.OnBackPressedListener
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.utils.FileUtils
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_replay_mode.*

class ReplayModeFragment : Fragment(), OnBackPressedListener {

    companion object {

        val TAG: String = ReplayModeFragment::class.java.simpleName
        private const val ARG_PARAM_SESSIONS_PATH = "ARG_PARAM_SESSIONS_PATH"

        fun newInstance(path: String) = ReplayModeFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM_SESSIONS_PATH, path)
            }
        }
    }

    private lateinit var sessionsAdapter: SessionsAdapter
    private lateinit var sessionsPath: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_replay_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val path = getSessionPathFromArguments()
        if (path != null) {
            sessionsPath = path
            initBackButton()
            initRecyclerViewSessions()
            initSelectAllButton()
            initEditButton()
            initSwipeRefreshLayout()
            initDeleteSessionsButton()
            initDoneEditButton()
            initRecordingButton()
            activateEmptyStateIfRequired()
        } else {
            requireActivity().onBackPressed()
        }
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun initRecyclerViewSessions() {
        recycler_sessions.layoutManager = LinearLayoutManager(requireContext())
        sessionsAdapter = SessionsAdapter(
                context = requireContext(),
                basePath = sessionsPath,
                clickSessionListener = { onSessionClick(it) },
                clickCameraListener = { onCameraClick() },
                onActivateMultiSelectionListener = { onActivateMultiSelection() }
        )
        recycler_sessions.adapter = sessionsAdapter
    }

    private fun initSelectAllButton() {
        select_all.setOnClickListener {
            sessionsAdapter.apply {
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
            sessionsAdapter.updateSessionsList(true)
            swipe_refresh_sessions.isRefreshing = false
            activateEmptyStateIfRequired()
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
            sessionsAdapter.let {
                if (it.isMultiSelection) {
                    for (session in it.selectedItems) {
                        val sessionsFolder = File("$sessionsPath/$session")
                        if (!sessionsFolder.deleteRecursively()) {
                            Toast.makeText(requireContext(), "Error: Can't delete ${sessionsFolder.absoluteFile}", Toast.LENGTH_LONG).show()
                        }
                    }
                    it.updateSessionsList(false)
                    changeMultiSelection(false)
                    activateEmptyStateIfRequired()
                }
            }
        }
    }

    private fun initRecordingButton() {
        record_session.setOnClickListener {
            val listener = requireActivity()
            if (listener is OnSelectModeItemListener) {
                listener.onRecordingSelected()
            }
        }
    }

    private fun onSessionClick(fileName: String) {
        if (sessionsAdapter.isMultiSelection) {
            setMultiSelectionTitle()
        } else {
            val listener = requireActivity()
            if (listener is OnSelectModeItemListener) {
                requireActivity().onBackPressed()
                listener.onSessionSelected(fileName)
            }
        }
    }

    private fun onCameraClick() {
        val listener = requireActivity()
        if (listener is OnSelectModeItemListener) {
            requireActivity().onBackPressed()
            listener.onCameraSelected()
        }
    }

    private fun setMultiSelectionTitle() {
        val count = sessionsAdapter.getSelectedCount()
        replay_fragment_title.text = requireContext().resources.getQuantityString(R.plurals.selected_items, count, count)
    }

    private fun changeMultiSelection(activate: Boolean) {
        if (activate) {
            sessionsAdapter.activateMultiSelection()
        } else {
            sessionsAdapter.resetMultiSelection()
            replay_fragment_title.setText(R.string.select_session_source)
        }
        swipe_refresh_sessions.isEnabled = !activate
        activateMultiSelectionVisibilityState(visible = activate)
    }

    private fun activateMultiSelectionVisibilityState(visible: Boolean) {
        val multiSelectionVisible = if (visible) VISIBLE else GONE
        val multiSelectionGone = if (visible) GONE else VISIBLE
        edit_sessions_list.visibility = multiSelectionGone
        select_all.visibility = multiSelectionVisible
        delete_sessions.visibility = multiSelectionVisible
        record_session.visibility = multiSelectionGone
        back_button.visibility = multiSelectionGone
        done_edit.visibility = multiSelectionVisible
    }

    private fun setEmptyState(empty: Boolean) {
        val emptyStateVisible = if (empty) VISIBLE else GONE
        val emptyStateGone = if (empty) GONE else VISIBLE
        no_sessions_title_text.visibility = emptyStateVisible
        no_sessions_description_text.visibility = emptyStateVisible
        recycler_sessions.visibility = emptyStateGone
        replay_fragment_title.visibility = emptyStateGone
        edit_sessions_list.visibility = emptyStateGone
    }

    private fun activateEmptyStateIfRequired() {
        val emptyState = !FileUtils.isDirectoryContainsFiles(sessionsPath)
        setEmptyState(emptyState)
    }

    override fun onBackPressed(): Boolean {
        if (sessionsAdapter.isMultiSelection) {
            changeMultiSelection(false)
            return true
        }
        return false
    }

    private fun getSessionPathFromArguments() = arguments?.getString(ARG_PARAM_SESSIONS_PATH)

    interface OnSelectModeItemListener {

        fun onSessionSelected(sessionName: String)

        fun onCameraSelected()

        fun onRecordingSelected()
    }
}
