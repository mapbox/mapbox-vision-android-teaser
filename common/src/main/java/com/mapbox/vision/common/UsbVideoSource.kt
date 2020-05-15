package com.mapbox.vision.common

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.WorkerThread
import com.mapbox.vision.VisionManager
import com.mapbox.vision.gl.EglCore
import com.mapbox.vision.gl.GLDrawFrameOES
import com.mapbox.vision.gl.OffscreenSurface
import com.mapbox.vision.gl.RenderBuffer
import com.mapbox.vision.mobile.core.models.CameraParameters
import com.mapbox.vision.mobile.core.models.frame.ImageFormat
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.video.videosource.VideoSource
import com.mapbox.vision.video.videosource.VideoSourceListener
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VideoSource implementation that connects to USB camera and feeds frames to VisionManager.
 */
class UsbVideoSource(
    private val context: Application
) : VideoSource {

    companion object {
        private val FULL_HD = ImageSize(
            imageWidth = 1280,
            imageHeight = 720
        )
        private val DEFAULT = ImageSize(
            imageWidth = 640,
            imageHeight = 480
        )
        private val CAMERA_FRAME_SIZE = FULL_HD

        private val MIN_FPS = 60
        private val MAX_FPS = 60

        // MJPEG allows to configure BRIO with 1280*720 / 60FPS
        private val FRAME_FORMAT = UVCCamera.FRAME_FORMAT_MJPEG
    }

    private fun UsbDevice?.isUvcCamera(): Boolean {
        return this != null && deviceClass == 239 && deviceSubclass == 2
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
                println("Register USB monitor")
                usbMonitor!!.register()
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
                usbMonitor!!.unregister()
                uvcCamera?.stopPreview()
                usbMonitor!!.destroy()
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
                println("OnAttach ${device?.manufacturerName}")
                synchronized(this@UsbVideoSource) {
                    if (device.isUvcCamera()) {
                        usbMonitor!!.requestPermission(device)
                        println("Device is UVC camera!")
                    } else {
                        println("Device is NOT UVC camera!")
                    }
                }
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                backgroundHandler.post {
                    println("OnConnect $device")
                    synchronized(this@UsbVideoSource) {
                        if (device.isUvcCamera()) {
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
            }

            override fun onDetach(device: UsbDevice?) {
                println("OnDetach ${device?.manufacturerName}")
            }

            override fun onCancel(device: UsbDevice?) {
                println("OnCancel ${device?.manufacturerName}")
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                backgroundHandler.post {
                    println("OnDisconnect ${device?.manufacturerName}")
                    synchronized(this@UsbVideoSource) {
                        if (device.isUvcCamera()) {
                            releaseCamera()
                        }
                    }
                }
            }
        }

    private fun releaseCamera() {
        uvcCamera?.close()
        uvcCamera?.destroy()
        uvcCamera = null

        surface?.release()
        surface = null

        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        surfaceTexture = null

        externalFrameDrawer?.release()
        externalFrameDrawer = null

        offscreenRenderBuffer?.release()
        offscreenRenderBuffer = null

        offscreenSurface?.release()
        offscreenSurface = null

        eglCore?.release()
        eglCore = null
    }

    var frames = 0
    var time = 0L

    private var eglCore: EglCore? = null
    private var offscreenSurface: OffscreenSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var externalFrameDrawer: GLDrawFrameOES? = null
    private var offscreenRenderBuffer: RenderBuffer? = null

    private var videoFrameBufferData = ByteBuffer
        .allocateDirect(CAMERA_FRAME_SIZE.imageWidth * CAMERA_FRAME_SIZE.imageHeight * 4)
        .order(ByteOrder.nativeOrder())

    private val matrixMVP = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    @WorkerThread
    private fun initializeCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        uvcCamera = UVCCamera().also { camera ->
            eglCore = EglCore()
            offscreenSurface =
                OffscreenSurface(
                    eglCore = eglCore!!,
                    width = CAMERA_FRAME_SIZE.imageWidth,
                    height = CAMERA_FRAME_SIZE.imageHeight
                )
            offscreenSurface!!.makeCurrent()
            val textureId = createExternalGlTexture()
            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture!!.setDefaultBufferSize(
                CAMERA_FRAME_SIZE.imageWidth,
                CAMERA_FRAME_SIZE.imageHeight
            )
            externalFrameDrawer = GLDrawFrameOES()
            offscreenRenderBuffer = RenderBuffer(
                width = CAMERA_FRAME_SIZE.imageWidth,
                height = CAMERA_FRAME_SIZE.imageHeight
            )

            time = System.currentTimeMillis()

            surfaceTexture!!.setOnFrameAvailableListener {
                surfaceTexture!!.updateTexImage()
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offscreenRenderBuffer!!.frameBufferId)
                externalFrameDrawer!!.draw(textureId, matrixMVP)
                videoFrameBufferData.rewind()
                GLES20.glReadPixels(
                    0, 0, CAMERA_FRAME_SIZE.imageWidth, CAMERA_FRAME_SIZE.imageHeight,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, videoFrameBufferData
                )
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

                frames++
                val delta = (System.currentTimeMillis() - time) / 1000
                if (frames % 30 == 0) {
                    println("Camera FPS ${frames.toFloat() / delta} (frames $frames, time $delta)")
                }

                usbVideoSourceListener?.onNewFrame(
                    VideoSourceListener.FrameHolder.ByteBufferHolder(videoFrameBufferData),
                    ImageFormat.RGBA,
                    CAMERA_FRAME_SIZE
                )
            }
            surface = Surface(surfaceTexture)

            println("Open camera")

            try {
                camera.open(ctrlBlock)
            } catch (e : Exception) {
                println("Open camera failed! ")
                e.printStackTrace()
                return
            }

            camera.setPreviewSize(
                CAMERA_FRAME_SIZE.imageWidth,
                CAMERA_FRAME_SIZE.imageHeight,
                MIN_FPS,
                MAX_FPS,
                FRAME_FORMAT,
                1f
            )

            // Start preview to external GL texture
            // NOTE : this is necessary for callback passed to [UVCCamera.setFrameCallback]
            // to be triggered afterwards
            camera.setPreviewDisplay(surface)
            camera.startPreview()
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
