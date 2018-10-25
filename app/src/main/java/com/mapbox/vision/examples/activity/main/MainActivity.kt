package com.mapbox.vision.examples.activity.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils
import com.mapbox.vision.VisionManager
import com.mapbox.vision.corewrapper.update.VisionEventsListener
import com.mapbox.vision.examples.R
import com.mapbox.vision.examples.utils.SoundsPlayer
import com.mapbox.vision.examples.activity.ar.ArMapActivity
import com.mapbox.vision.examples.activity.map.MapActivity
import com.mapbox.vision.examples.models.UiSignValueModel
import com.mapbox.vision.examples.utils.classification.SignMapper
import com.mapbox.vision.examples.utils.classification.SignMapperImpl
import com.mapbox.vision.examples.utils.classification.Tracker
import com.mapbox.vision.examples.utils.hide
import com.mapbox.vision.examples.utils.lines.RoadDescriptionMapper
import com.mapbox.vision.examples.utils.show
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceConfig
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.view.VisualizationMode
import com.mapbox.vision.visionevents.CalibrationProgress
import com.mapbox.vision.visionevents.events.classification.SignClassification
import com.mapbox.vision.visionevents.events.detection.Collision
import com.mapbox.vision.visionevents.events.detection.Detections
import com.mapbox.vision.visionevents.events.position.Position
import com.mapbox.vision.visionevents.events.roaddescription.RoadDescription
import com.mapbox.vision.visionevents.events.segmentation.SegmentationMask
import com.mapbox.vision.visionevents.events.worlddescription.WorldDescription
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1

        private const val SEGMENTATION_MODE = 0
        private const val CLASSIFICATION_MODE = 1
        private const val DETECTION_MODE = 2
        private const val DISTANCE_TO_CAR_MODE = 3
        private const val LINE_DETECTION_MODE = 4
    }

    private val signMapper: SignMapper = SignMapperImpl(this)

    private var signSize = 0
    private var leftMargin = 0

    private var lineSize = 0

    private var tracker: Tracker<UiSignValueModel> = Tracker(5)
    private var currentMode = DETECTION_MODE

    private var isPermissionsGranted = false
    private lateinit var soundsPlayer: SoundsPlayer

    private val visionEventsListener = object : VisionEventsListener {

        @SuppressLint("SetTextI18n")
        private fun extractFpsInfo() {
            val frameStatistics = VisionManager.getFrameStatistics()
            segmentation_fps.text = "S: ${frameStatistics.segmentationFPS}"
            detection_fps.text = "D: ${frameStatistics.detectionFPS}"
            road_confidence_fps.text = "RC: ${frameStatistics.roadConfidenceFPS}"
            merge_model_fps.text = "MM: ${frameStatistics.segmentationDetectionFPS}"
            core_update_fps.text = "CU: ${frameStatistics.coreUpdateFPS}"
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

        private val distanceFormatter by lazy {
            LocaleUtils().let { localeUtils ->
                val language = localeUtils.inferDeviceLanguage(this@MainActivity)
                val unitType = localeUtils.getUnitTypeForDeviceLocale(this@MainActivity)
                val roundingIncrement = NavigationConstants.ROUNDING_INCREMENT_FIVE
                DistanceFormatter(this@MainActivity, language, unitType, roundingIncrement)
            }
        }

        private var currentCollisionState: Collision.CollisionState = Collision.CollisionState.NOT_TRIGGERED
        override fun worldDescriptionUpdated(worldDescription: WorldDescription) {
            if (currentMode == DISTANCE_TO_CAR_MODE) {
                extractFpsInfo()
                val car = worldDescription.objects.firstOrNull()
                worldDescription.collisions.firstOrNull().let { collision ->

                    if (collision == null) {
                        soundsPlayer.stop()
                        currentCollisionState = Collision.CollisionState.NOT_TRIGGERED
                        distance_to_car_label.hide()
                        distance_to_car_image.hide()
                    } else {
                        distance_to_car_label.show()
                        distance_to_car_image.show()
                        if (currentCollisionState != collision.state) {
                            soundsPlayer.stop()
                            when (collision.state) {
                                Collision.CollisionState.WARNING -> {
                                    soundsPlayer.playWarning()
                                }
                                Collision.CollisionState.CRITICAL -> {
                                    soundsPlayer.playCritical()
                                }
                                else -> {
                                }
                            }
                            currentCollisionState = collision.state
                        }
                        distance_to_car_label.text = distanceFormatter.formatDistance(collision.car.distance)
                        distance_to_car_image.drawCollision(collision)
                    }
                }
            }
        }

        override fun estimatedPositionUpdated(position: Position) {}

        override fun calibrationProgressUpdated(calibrationProgress: CalibrationProgress) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!SupportedSnapdragonBoards.isBoardSupported(SystemInfoUtils.getSnpeSupportedBoard()) ) {
            val text = Html.fromHtml("The device is not supported, you need <b>Snapdragon-powered</b> device with <b>OpenCL</b> support, more details at <b>https://www.mapbox.com/android-docs/vision/overview/</b>")
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        VisionManager.create()
        VisionManager.setVisionEventListener(visionEventsListener)

        soundsPlayer = SoundsPlayer(this)

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
        distance_to_car.hide()
        object_mapping_button_container.setOnClickListener {
            startActivity(MapActivity.createIntent(this))
        }
        ar_navigation_button_container.setOnClickListener {
            startActivity(Intent(this, ArMapActivity::class.java))
        }
        fps_info_container.show()
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
        soundsPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        VisionManager.destroy()
    }

    private fun onBackClick() {
        dashboard_container.show()
        back.hide()
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

    private fun hideSignsContainer() {
        sign_info_container.removeAllViews()
        sign_info_container.hide()
    }

    private fun hideLineDetectionContainer() {
        lines_detections_container.removeAllViews()
        lines_detections_container.hide()
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
        sign_info_container.show()
        dashboard_container.hide()
        distance_to_car.hide()
        back.show()

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
        dashboard_container.hide()
        distance_to_car.hide()
        back.show()
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
        dashboard_container.hide()
        distance_to_car.hide()
        back.show()
    }

    private fun setDistanceToCarMode() {
        VisionManager.setModelPerformanceConfig(
                ModelPerformanceConfig.Separate(
                        detectionPerformance = ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.HIGH),
                        segmentationPerformance = ModelPerformance.Off
                )
        )

        vision_view.visualizationMode = VisualizationMode.CLEAR
        currentMode = DISTANCE_TO_CAR_MODE

        hideLineDetectionContainer()
        hideSignsContainer()
        dashboard_container.hide()
        distance_to_car.show()
        back.show()
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
}
