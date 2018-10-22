package com.mapbox.vision.examples

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.mapbox.vision.VisionManager
import com.mapbox.vision.visionevents.events.worlddescription.ObjectDescription

class DistanceToCarView : View {

    companion object {
        private const val RECT_HEIGHT = 100f
    }

    private val path = Path()
    private var widthRatio = 1f
    private var heightRatio = 1f

    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private fun getShader(y: Float) = LinearGradient(
            0f, y, 0f, y + RECT_HEIGHT,
            resources.getColor(R.color.minty_green, null),
            resources.getColor(android.R.color.transparent, null),
            Shader.TileMode.MIRROR
    )

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val frameSize = VisionManager.getFrameSize()
            widthRatio = width.toFloat() / frameSize.width
            heightRatio = height.toFloat() / frameSize.height
        }
    }

    fun drawDetectedDistanceToCar(car: ObjectDescription) {
        path.reset()
        val left = car.detection.boundingBox.left * widthRatio
        val top = car.detection.boundingBox.bottom * heightRatio
        val bottom = car.detection.boundingBox.bottom * heightRatio + RECT_HEIGHT
        val right = car.detection.boundingBox.right * widthRatio + RECT_HEIGHT
        if (car.detection.boundingBox.left != 0 && car.detection.boundingBox.right != 0) {
            path.moveTo(
                    left + RECT_HEIGHT,
                    top
            )
            path.lineTo(
                    left,
                    bottom
            )
            path.lineTo(
                    right,
                    bottom
            )
            path.lineTo(
                    right - RECT_HEIGHT,
                    top
            )
            path.close()
            paint.shader = getShader(car.detection.boundingBox.top.toFloat())
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)

        super.onDraw(canvas)
    }
}
