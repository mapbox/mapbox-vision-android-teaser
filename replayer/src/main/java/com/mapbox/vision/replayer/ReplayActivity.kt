package com.mapbox.vision.replayer

import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.vision.VisionReplayManager
import com.mapbox.vision.common.view.BaseTeaserActivity
import com.mapbox.vision.common.view.show
import com.mapbox.vision.performance.ModelType
import com.mapbox.vision.safety.VisionSafetyManager
import com.mapbox.vision.utils.threads.WorkThreadHandler
import com.mapbox.vision.view.VisionView
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Scanner


class ReplayActivity : BaseTeaserActivity(), SessionsFragment.SessionChangeListener {

    override fun initViews(root: View) {
        root.findViewById<ImageView>(R.id.object_mapping).apply {
            setImageDrawable(
                ContextCompat.getDrawable(this@ReplayActivity, R.drawable.ic_section_routing)
            )
        }

        root.findViewById<ImageView>(R.id.ar_navigation).apply {
            setImageDrawable(
                ContextCompat.getDrawable(this@ReplayActivity, R.drawable.ic_select_session)
            )
        }

        root.findViewById<TextView>(R.id.object_mapping_text).apply {
            setText(R.string.start_ar_session)
        }

        root.findViewById<TextView>(R.id.ar_navigation_text).apply {
            setText(R.string.choose_session)
        }

        root.findViewById<LinearLayout>(R.id.object_mapping_button_container).apply {
            setOnClickListener { startArSession() }
        }

        root.findViewById<LinearLayout>(R.id.ar_navigation_button_container).apply {
            setOnClickListener { showSessionsList() }
        }

        root.findViewById<TextView>(R.id.title_replayer).apply {
            setText(R.string.app_title)
            show()
        }
    }

    override fun getFrameStatistics() = VisionReplayManager.getFrameStatistics()

    ///////

    private val modelType: ModelType = ModelType.OLD
    private val handler = WorkThreadHandler().also {
        it.start()
    }

    private val fileWriter = FileWriter("${Environment.getExternalStorageDirectory()}/vision-temperatures-${getCurrentTimestamp()}-$modelType-${Build.BOARD}.csv")

    private fun getCurrentTimestamp(): String = SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault()).format(Date())

    private val printThermal = object : Runnable {
        val start: Long by lazy {
            System.currentTimeMillis()
        }

        val files = File("sys/class/thermal/").listFiles { _, fileName ->
            fileName.contains("thermal_zone")
        }.sorted().also {
            fileWriter.append("seconds passed, ")
            it.forEach { file ->
                fileWriter.append("${readSystemFile("${file.absolutePath}/type")}, ")
            }
            fileWriter.appendln()
        }

        override fun run() {
            if (VisionReplayManager.getProgress() > 115000L) {
                restartVisionManager()
            } else {
                handler.postDelayed(this, 5000L)
            }

            fileWriter.append("${(System.currentTimeMillis() - start) / 1000}, ")
            files.forEach {
                val temp = readSystemFile("${it.absolutePath}/temp").toFloatOrNull() ?: 0f
                fileWriter.append(String.format(Locale.US, "%.1f, ", temp / 1000))
            }
            fileWriter.appendln()
            fileWriter.flush()
        }
    }

    override fun initVisionManager(visionView: VisionView): Boolean {
        if (sessionPath.isEmpty() || !File(sessionPath).exists() || File(sessionPath).list().isEmpty()) {
            showSessionsList()
            return false
        }

        VisionReplayManager.create(
            sessionPath,
            modelType = modelType
        )
        VisionReplayManager.visionEventsListener = visionEventsListener
        VisionReplayManager.start()
        VisionReplayManager.setModelPerformanceConfig(appModelPerformanceConfig)
        visionView.setVisionManager(VisionReplayManager)

        VisionSafetyManager.create(VisionReplayManager)
        VisionSafetyManager.visionSafetyListener = visionSafetyListener

        handler.post {
            printThermal.run()
        }

        return true
    }

    @Throws(Exception::class)
    private fun readSystemFile(systemFile: String): String = try {
        val process = ProcessBuilder("/system/bin/cat", systemFile).start()
        readFully(process.inputStream)
    } catch (e: Exception) {
        throw Exception(e)
    }

    private fun readFully(inputStream: InputStream): String {
        val sb = StringBuilder()
        val sc = Scanner(inputStream)
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine())
        }
        return sb.toString()
    }

    ///////

    override fun destroyVisionManager() {
        handler.removeAllTasks()

        VisionSafetyManager.destroy()
        VisionReplayManager.stop()
        VisionReplayManager.destroy()
    }

    private var sessionPath: String = "$BASE_SESSION_PATH/default/"
        set(value) {
            field = "$BASE_SESSION_PATH/$value/"
        }

    private fun startArSession() {
        if (sessionPath.isEmpty()) {
            Toast.makeText(this, "Select a session first.", Toast.LENGTH_SHORT).show()
        } else {
            ArReplayNavigationActivity.start(this, sessionPath)
        }
    }

    private fun showSessionsList() {
        SessionsFragment().show(supportFragmentManager, "sessions_fragment")
    }

    override fun onSessionChanged(dirName: String) {
        sessionPath = dirName
        restartVisionManager()
    }
}
