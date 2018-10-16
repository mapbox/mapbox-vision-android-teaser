package com.mapbox.vision.examples.activity.main

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import com.mapbox.vision.VisionManager
import com.mapbox.vision.corewrapper.update.VisionEventsListener
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.examples.activity.map.MapActivity
import com.mapbox.vision.examples.models.UiSignValueModel
import com.mapbox.vision.examples.utils.classification.SignMapper
import com.mapbox.vision.examples.utils.classification.SignMapperImpl
import com.mapbox.vision.examples.utils.classification.Tracker
import com.mapbox.vision.examples.utils.lines.RoadDescriptionMapper
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.view.VisualizationMode
import com.mapbox.vision.visionevents.events.classification.SignClassification
import com.mapbox.vision.visionevents.events.detection.Detections
import com.mapbox.vision.visionevents.events.position.Position
import com.mapbox.vision.visionevents.events.roaddescription.RoadDescription
import com.mapbox.vision.visionevents.events.segmentation.SegmentationMask
import com.mapbox.vision.visionevents.events.worlddescription.WorldDescription
import kotlinx.android.synthetic.main.activity_main.ar_navigation_button_container
import kotlinx.android.synthetic.main.activity_main.back
import kotlinx.android.synthetic.main.activity_main.core_update_fps
import kotlinx.android.synthetic.main.activity_main.dashboard_container
import kotlinx.android.synthetic.main.activity_main.det_container
import kotlinx.android.synthetic.main.activity_main.detection_fps
import kotlinx.android.synthetic.main.activity_main.distance_container
import kotlinx.android.synthetic.main.activity_main.fps_info_container
import kotlinx.android.synthetic.main.activity_main.lines_detections_container
import kotlinx.android.synthetic.main.activity_main.merge_model_fps
import kotlinx.android.synthetic.main.activity_main.object_mapping_button_container
import kotlinx.android.synthetic.main.activity_main.road_confidence_fps
import kotlinx.android.synthetic.main.activity_main.segm_container
import kotlinx.android.synthetic.main.activity_main.segmentation_fps
import kotlinx.android.synthetic.main.activity_main.sign_detection_container
import kotlinx.android.synthetic.main.activity_main.sign_info_container
import kotlinx.android.synthetic.main.activity_main.vision_view

class MainActivity : AppCompatActivity() {

    private val signMapper: SignMapper = SignMapperImpl(this)

    private var signSize = 0
    private var leftMargin = 0

    private var lineSize = 0

    private var tracker: Tracker<UiSignValueModel> = Tracker(5)
    private var currentMode = DETECTION_MODE

    private var isPermissionsGranted = false

    // Debug flag to show debug overlay
    private var extractFpsInfo = true

    private val visionEventsListener = object : VisionEventsListener {
        private fun extractFpsInfo() {
            val frameStatistics = VisionManager.getFrameStatistics()
            setSegmentationFPS(frameStatistics.segmentationFPS)
            setDetectionFPS(frameStatistics.detectionFPS)
            setRoadConfidenceFPS(frameStatistics.roadConfidenceFPS)
            setSegmentationDetectionFPS(frameStatistics.segmentationDetectionFPS)
            setCoreUpdateFPS(frameStatistics.coreUpdateFPS)
        }

        override fun detectionsUpdated(detections: Detections) {
            if (currentMode == DETECTION_MODE) {
                extractFpsInfo()
            }
        }

        override fun segmentationUpdated(segmentationMask: SegmentationMask) {
            if (currentMode == SEGMENTATION_MODE) {
                extractFpsInfo()
            }
        }

        override fun signClassificationUpdated(signClassification: SignClassification) {
            if (currentMode == CLASSIFICATION_MODE) {
                extractFpsInfo()
                with(tracker) {
                    update(UiSignValueModel.getSignValueListBySignClassification(signClassification))
                    drawSigns(getCurrent())
                }
            }
        }

        override fun roadDescriptionUpdated(roadDescription: RoadDescription) {}

        override fun worldDescriptionUpdated(worldDescription: WorldDescription) {}

        override fun estimatedPositionUpdated(position: Position) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        VisionManager.create()
        VisionManager.setVisionEventListener(visionEventsListener)

        if (!allPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE)
            return
        } else {
            onPermissionsGranted()
        }
    }

    private fun onPermissionsGranted() {
        isPermissionsGranted = true

        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
                )
        )

        signSize = resources.getDimension(R.dimen.dp64).toInt()
        lineSize = resources.getDimension(R.dimen.dp40).toInt()
        leftMargin = resources.getDimension(R.dimen.dp8).toInt()

        back.setOnClickListener { onBackClick() }
        segm_container.setOnClickListener { setSegmentationMode() }
        sign_detection_container.setOnClickListener { setSignClassificationMode() }
        det_container.setOnClickListener { setDetectionMode() }
        distance_container.setOnClickListener { setDistanceToCarMode() }
        object_mapping_button_container.setOnClickListener {
            startActivity(MapActivity.createIntent(this))
        }
        ar_navigation_button_container.setOnClickListener {
            startActivity(Intent(this, ArMapActivity::class.java))
        }

        if (extractFpsInfo) {
            showFpsInfoContainer()
        } else {
            hideFpsInfoContainer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted() && requestCode == PERMISSIONS_REQUEST_CODE) {
            onPermissionsGranted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionsGranted) {
            VisionManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPermissionsGranted) {
            VisionManager.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VisionManager.destroy()
    }

    private fun onBackClick() {
        showDashboard()
        hideBackButton()
    }

    private fun drawSigns(signsValueUis: List<UiSignValueModel>) {
        sign_info_container.removeAllViews()
        for (signValue in signsValueUis) {
            val image = ImageView(this)
            val lp = ViewGroup.MarginLayoutParams(signSize, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = leftMargin
            image.layoutParams = lp
            image.paddingLeft

            image.setImageResource(signMapper.getResourceByValue(signValue))

            // Adds the view to the layout
            sign_info_container.addView(image)
        }
    }

    // TODO draw lines
    private fun drawLinesDetection(roadElements: List<RoadDescriptionMapper.RoadElement>) {
        lines_detections_container.removeAllViews()

        for (roadElement in roadElements) {
            val image = ImageView(this)
            val lp = ViewGroup.MarginLayoutParams(lineSize, ViewGroup.LayoutParams.WRAP_CONTENT)
            image.layoutParams = lp
            image.paddingLeft

            image.setImageResource(roadElement.drawableResourceId)

            lines_detections_container.addView(image)
        }
    }

    private fun showDashboard() {
        dashboard_container.visibility = View.VISIBLE
    }

    private fun hideDashboard() {
        dashboard_container.visibility = View.GONE
    }

    private fun hideSignsContainer() {
        sign_info_container.removeAllViews()
        sign_info_container.visibility = View.GONE
    }

    private fun showSignsContainer() {
        sign_info_container.visibility = View.VISIBLE
    }

    private fun showLineDetectionContainer() {
        lines_detections_container.visibility = View.VISIBLE
    }

    private fun hideLineDetectionContainer() {
        lines_detections_container.removeAllViews()
        lines_detections_container.visibility = View.GONE
    }

    private fun hideBackButton() {
        back.visibility = View.GONE
    }

    private fun showBackButton() {
        back.visibility = View.VISIBLE
    }

    private fun setSegmentationOverlay(bitmap: Bitmap) {
        // DO nothing
    }

    private fun setDetectionOverlay(bitmap: Bitmap) {
        // Do nothing
    }

    private fun setSegmentationFPS(fpsRate: Float) {
        segmentation_fps.text = "S: $fpsRate"
    }

    private fun setDetectionFPS(fpsRate: Float) {
        detection_fps.text = "D: $fpsRate"
    }

    private fun setRoadConfidenceFPS(fpsRate: Float) {
        road_confidence_fps.text = "RC: $fpsRate"
    }

    private fun setSegmentationDetectionFPS(fpsRate: Float) {
        merge_model_fps.text = "MM: $fpsRate"
    }

    private fun setCoreUpdateFPS(fpsRate: Float) {
        core_update_fps.text = "CU: $fpsRate"
    }

    private fun showFpsInfoContainer() {
        fps_info_container.visibility = View.VISIBLE
    }

    private fun hideFpsInfoContainer() {
        fps_info_container.visibility = View.GONE
    }

    private fun setSignClassificationMode() {
        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
                )
        )

        vision_view.visualizationMode = VisualizationMode.CLEAR
        currentMode = CLASSIFICATION_MODE

        tracker = Tracker(5)

        hideLineDetectionContainer()
        showSignsContainer()
        hideDashboard()
        showBackButton()

    }

    private fun setDetectionMode() {
        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
                )
        )

        vision_view.visualizationMode = VisualizationMode.DETECTION
        currentMode = DETECTION_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        hideDashboard()
        showBackButton()
    }

    private fun setSegmentationMode() {
        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Merged(
                        performance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH)
                )
        )

        vision_view.visualizationMode = VisualizationMode.SEGMENTATION
        currentMode = SEGMENTATION_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        hideDashboard()
        showBackButton()
    }

    // FIXME
    private fun setDistanceToCarMode() {
        currentMode = DISTANCE_TO_CAR_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        hideDashboard()
        showBackButton()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun getRequiredPermissions(): Array<String> {
        return try {
            val info = packageManager?.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val ps = info!!.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            emptyArray()
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1

        private const val SEGMENTATION_MODE = 0
        private const val CLASSIFICATION_MODE = 1
        private const val DETECTION_MODE = 2
        private const val DISTANCE_TO_CAR_MODE = 3
        private const val LINE_DETECTION_MODE = 4
    }
}
