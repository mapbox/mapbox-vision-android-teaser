package com.mapbox.vision.common

import android.app.Application
import android.graphics.Camera
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import com.mapbox.vision.VisionManager
import com.mapbox.vision.mobile.core.models.CameraParameters
import com.mapbox.vision.mobile.core.models.frame.ImageFormat
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.video.videosource.VideoSource
import com.mapbox.vision.video.videosource.VideoSourceListener
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera

/**
 * VideoSource implementation that connects to USB camera and feeds frames to VisionManager.
 */
class UsbVideoSource(
    private val context: Application
) : VideoSource {

    companion object {
        private val CAMERA_FRAME_SIZE = ImageSize(
            imageWidth = 1280,
            imageHeight = 720
        )
    }

    private val backgroundHandlerThread = HandlerThread("VideoDecode").apply { start() }
    private var backgroundHandler = Handler(backgroundHandlerThread.looper)

    /**
     * Vision SDK will attach listener to get frames and camera parameters from the USB camera.
     */
    private var usbVideoSourceListener: VideoSourceListener? = null

    /**
     * VisionManager will attach [videoSourceListener] after [VisionManager.create] is called.
     * Here we open USB camera connection, and proceed connection via [onDeviceConnectListener] callbacks.
     *
     * NOTE : method is called from the same thread, that [VisionManager.create] is called.
     */
    override fun attach(videoSourceListener: VideoSourceListener) {
        if (!backgroundHandlerThread.isAlive) {
            backgroundHandlerThread.start()
        }
        backgroundHandler.post {
            // Init and register USBMonitor.
            synchronized(this@UsbVideoSource) {
                this@UsbVideoSource.usbVideoSourceListener = videoSourceListener

                usbMonitor = USBMonitor(context, onDeviceConnectListener)
                usbMonitor?.register()
            }
        }
    }

    /**
     * VisionManager will detach listener after [VisionManager.destroy] is called.
     * Here we close USB camera connection.
     *
     * NOTE : method is called from the same thread, that [VisionManager.destroy] is called.
     */
    override fun detach() {
        backgroundHandler.post {
            synchronized(this@UsbVideoSource) {
                usbMonitor?.unregister()
                uvcCamera?.stopPreview()
                usbMonitor?.destroy()
                releaseCamera()
                usbVideoSourceListener = null
            }
        }

        backgroundHandlerThread.quitSafely()
    }

    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null

    private val onDeviceConnectListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                synchronized(this@UsbVideoSource) {
                    usbMonitor?.requestPermission(device!!)
                }
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                backgroundHandler.post {
                    synchronized(this@UsbVideoSource) {
                        releaseCamera()
                        initializeCamera(ctrlBlock!!)
                        usbVideoSourceListener?.onNewCameraParameters(CameraParameters(
                            width = CAMERA_FRAME_SIZE.imageWidth,
                            height = CAMERA_FRAME_SIZE.imageHeight,
                            focalInPixelsX = 900f,
                            focalInPixelsY = 800f
                        ))
                    }
                }
            }

            override fun onDetach(device: UsbDevice?) {}

            override fun onCancel(device: UsbDevice?) {}

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                backgroundHandler.post {
                    synchronized(this@UsbVideoSource) {
                        releaseCamera()
                    }
                }
            }
        }

    private fun releaseCamera() {
        uvcCamera?.close()
        uvcCamera?.destroy()
        uvcCamera = null
    }

    private fun initializeCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        uvcCamera = UVCCamera().also { camera ->
            camera.open(ctrlBlock)
            camera.setPreviewSize(
                CAMERA_FRAME_SIZE.imageWidth,
                CAMERA_FRAME_SIZE.imageHeight,
                UVCCamera.FRAME_FORMAT_YUYV
            )

            val surfaceTexture = SurfaceTexture(createExternalGlTexture())
            surfaceTexture.setDefaultBufferSize(
                CAMERA_FRAME_SIZE.imageWidth,
                CAMERA_FRAME_SIZE.imageHeight
            )
            // Start preview to external GL texture
            // NOTE : this is necessary for callback passed to [UVCCamera.setFrameCallback]
            // to be triggered afterwards
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()

            // Set callback that will feed frames from the USB camera to Vision SDK
            camera.setFrameCallback(
                { frame ->
                    usbVideoSourceListener?.onNewFrame(
                        VideoSourceListener.FrameHolder.ByteBufferHolder(frame),
                        ImageFormat.RGBA,
                        CAMERA_FRAME_SIZE
                    )
                },
                UVCCamera.PIXEL_FORMAT_RGBX
            )
        }
    }

    /**
     * Create external OpenGL texture for [uvcCamera].
     */
    private fun createExternalGlTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val texId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return texId
    }

}
