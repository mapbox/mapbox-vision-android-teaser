package com.mapbox.vision.examples.activity.vision

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.vision.view.VisionView
import kotlinx.android.synthetic.main.activity_vision_sample.*
import com.mapbox.vision.VisionManager
import com.mapbox.vision.examples.R
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.AuthorizationStatus
import com.mapbox.vision.mobile.core.models.Camera
import com.mapbox.vision.mobile.core.models.Country
import com.mapbox.vision.mobile.core.models.FrameSegmentation
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications
import com.mapbox.vision.mobile.core.models.detection.FrameDetections
import com.mapbox.vision.mobile.core.models.frame.ImageFormat
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.mobile.core.models.position.VehicleState
import com.mapbox.vision.mobile.core.models.road.RoadDescription
import com.mapbox.vision.mobile.core.models.world.WorldDescription
import com.mapbox.vision.video.videosource.VideoSource
import com.mapbox.vision.video.videosource.VideoSourceListener
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit


class VisionSampleActivity : AppCompatActivity() {

    // replace with your own path to video file
    // private val PATH_TO_VIDEO_FILE = Environment.getExternalStorageDirectory().path + "/Telemetry/2019-08-27_09-48-52+0800/video0.mp4"
    private val PATH_TO_VIDEO_FILE = "path_to_your_video_file"

    private var videoSourceListener: VideoSourceListener? = null
    private val handlerThread = HandlerThread("VideoDecode")
    private lateinit var visionView: VisionView
    private var visionManagerWasInit = false

    private var mIsGlView = true

    // VideoSource that will play the file.
    private val customVideoSource = object : VideoSource {
        override fun attach(videoSourceListener: VideoSourceListener) {
            this@VisionSampleActivity.videoSourceListener = videoSourceListener
            handlerThread.start()
            Handler(handlerThread.looper).post { startFileVideoSource() }
        }

        override fun detach() {
            videoSourceListener = null
            handlerThread.quitSafely()
        }
    }

    // VisionEventsListener handles events from Vision SDK on background thread.
    private val visionEventsListener = object : VisionEventsListener {

        override fun onAuthorizationStatusUpdated(authorizationStatus: AuthorizationStatus) {}

        override fun onFrameSegmentationUpdated(frameSegmentation: FrameSegmentation) {
            visionView.setSegmentation(frameSegmentation)
        }

        override fun onFrameDetectionsUpdated(frameDetections: FrameDetections) {
            visionView.setDetections(frameDetections)
        }

        override fun onFrameSignClassificationsUpdated(frameSignClassifications: FrameSignClassifications) {}

        override fun onRoadDescriptionUpdated(roadDescription: RoadDescription) {}

        override fun onWorldDescriptionUpdated(worldDescription: WorldDescription) {}

        override fun onVehicleStateUpdated(vehicleState: VehicleState) {}

        override fun onCameraUpdated(camera: Camera) {}

        override fun onCountryUpdated(country: Country) {}

        override fun onUpdateCompleted() {}
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mIsGlView = savedInstanceState.getBoolean("is_gl_view")
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vision_sample)
        if (mIsGlView) {
            visionView = vision_gl_view
            btn_switch_view.text = "Switch to Bitmap view"
            vision_gl_view.visibility = VISIBLE
            vision_bitmap_view.visibility = GONE
        } else {
            visionView = vision_bitmap_view
            btn_switch_view.text = "Switch to GL view"
            vision_gl_view.visibility = GONE
            vision_bitmap_view.visibility = VISIBLE
        }
        btn_switch_view.setOnClickListener { recreate() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_gl_view", !mIsGlView)
    }

    override fun onStart() {
        super.onStart()
        startVisionManager()
    }

    override fun onStop() {
        super.onStop()
        stopVisionManager()
    }

    private fun startVisionManager() {
        if (!visionManagerWasInit) {
            VisionManager.create(customVideoSource)
            VisionManager.start()
            VisionManager.visionEventsListener = visionEventsListener
            VisionManager.setVideoSourceListener(visionView)
            visionManagerWasInit = true
        }
    }

    private fun stopVisionManager() {
        if (visionManagerWasInit) {
            VisionManager.stop()
            VisionManager.destroy()
            visionView.release()
            visionManagerWasInit = false
        }
    }

    /**
     * Decodes video source frame by frame and feeds frames to Vision SDK.
     */
    private fun startFileVideoSource() {
        // Use MediaMetadataRetriever to decode video.
        // It isn't the fastest approach to decode videos and you probably want some other method.
        // if FPS is important (eg. MediaCodec).
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(PATH_TO_VIDEO_FILE)

        // Get video frame size.
        val frameWidth =
            Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        val frameHeight =
            Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        val imageSize = ImageSize(frameWidth, frameHeight)
        // ByteBuffer to hold RGBA bytes.
        val rgbaByteBuffer = ByteBuffer.wrap(ByteArray(frameWidth * frameHeight * 4))

        // Get duration.
        val duration =
            java.lang.Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))

        try {
            // Get frames one by one with 1 second intervals.
            for (seconds in 0 until duration) {
                val bitmap = retriever
                    .getFrameAtTime(
                        TimeUnit.SECONDS.toMicros(seconds),
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    .copy(Bitmap.Config.ARGB_8888, false)

                bitmap.copyPixelsToBuffer(rgbaByteBuffer)

                videoSourceListener!!.onNewFrame(
                    rgbaByteBuffer.array(),
                    ImageFormat.RGBA,
                    imageSize
                )
                rgbaByteBuffer.clear()
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

}
