package com.mapbox.vision.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DimenRes
import com.mabpox.vision.teaser.common.R
import com.mapbox.vision.VisionManager
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.mobile.core.models.frame.PixelCoordinate
import com.mapbox.vision.mobile.core.models.road.Lane
import kotlin.math.max
import kotlin.properties.Delegates


class LaneView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var strokePath = Path()
    private val strokePaint = Paint().apply {
        color = resources.getColor(R.color.blue, null)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var fillPath = Path()
    private val fillPaint = Paint().apply {
        color = resources.getColor(R.color.blue, null)
        style = Paint.Style.FILL
        alpha = 50
    }

    private val transparent = resources.getColor(android.R.color.transparent, null)

    private val alertWidth: Int

    var frameSize: ImageSize by Delegates.observable(ImageSize(1, 1)) { property, oldValue, newValue ->
        if (oldValue != newValue) {
            updateScale()
        }
    }

    private var drawRect = RectF()
    private var drawRectScaleFactor = 1f

    init {
        alertWidth = pixels(R.dimen.alert_width) / 2
    }

    private fun View.pixels(@DimenRes res: Int) = context.resources.getDimensionPixelSize(res)

    private var scaleFactor = 1f
    private var scaledSize = ImageSize(1, 1)

    private fun updateScale() {
        scaleFactor = max(
            width.toFloat() / frameSize.imageWidth,
            height.toFloat() / frameSize.imageHeight
        )
        scaledSize = ImageSize(
            imageWidth = (frameSize.imageWidth * scaleFactor).toInt(),
            imageHeight = (frameSize.imageHeight * scaleFactor).toInt()
        )
    }

    private fun Float.scaleX(): Float = this / frameSize.imageWidth * scaledSize.imageWidth - (scaledSize.imageWidth - width) / 2

    private fun Float.scaleY(): Float = this / frameSize.imageHeight * scaledSize.imageHeight - (scaledSize.imageHeight - height) / 2

    private fun Float.rectifyX(): Float {
        if (drawRect.isEmpty) {
            return this
        }

        // X relative to the zero center
        val relativeX = this - drawRect.centerX()

        // relative X scaled
        val scaledX = relativeX * drawRectScaleFactor

        // scaled X translated back from the zero center to zero left top corner
        val translatedX = scaledX + (width / 2)

        return translatedX
    }

    private fun Float.rectifyY(): Float {
        if (drawRect.isEmpty) {
            return this
        }

        // Y relative to the zero center
        val relativeY = this - drawRect.centerY()

        // relative Y scaled
        val scaledY = relativeY * drawRectScaleFactor

        // scaled Y translated back from the zero center to zero left top corner
        val translatedY = scaledY + (height / 2)

        return translatedY
    }

    private fun PixelCoordinate.scale(): PixelCoordinate = PixelCoordinate(
        this.x.toFloat().scaleX().toInt(),
        this.y.toFloat().scaleY().toInt()
    )

    private fun PixelCoordinate.rectify() = PixelCoordinate(
        this.x.toFloat().rectifyX().toInt(),
        this.y.toFloat().rectifyY().toInt()
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateScale()
    }

    fun setSelectedArea(relativeRect: RectF?) {
        if (relativeRect == null) {
            drawRect = RectF()
            drawRectScaleFactor = 1f
            return
        }

        drawRect = RectF(
            (1 - relativeRect.right) * width,
            (1 - relativeRect.top) * height,
            (1 - relativeRect.left) * width,
            (1 - relativeRect.bottom) * height
        )

        drawRectScaleFactor = max(
            width / drawRect.width(),
            height / drawRect.height()
        )
    }

    fun drawLane(lane: Lane?) {
        strokePath.reset()
        fillPath.reset()
        setBackgroundColor(transparent)

        lane?.let {
            val leftP1 = VisionManager.worldToPixel(lane.leftEdge.curve.p1)?.scale()?.rectify()
                ?: return@let
            val leftP2 = VisionManager.worldToPixel(lane.leftEdge.curve.p2)?.scale()?.rectify()
                ?: return@let
            val leftP3 = VisionManager.worldToPixel(lane.leftEdge.curve.p3)?.scale()?.rectify()
                ?: return@let
            val leftP4 = VisionManager.worldToPixel(lane.leftEdge.curve.p4)?.scale()?.rectify()
                ?: return@let
            val rightP1 = VisionManager.worldToPixel(lane.rightEdge.curve.p1)?.scale()?.rectify()
                ?: return@let
            val rightP2 = VisionManager.worldToPixel(lane.rightEdge.curve.p2)?.scale()?.rectify()
                ?: return@let
            val rightP3 = VisionManager.worldToPixel(lane.rightEdge.curve.p3)?.scale()?.rectify()
                ?: return@let
            val rightP4 = VisionManager.worldToPixel(lane.rightEdge.curve.p4)?.scale()?.rectify()
                ?: return@let

            strokePath.apply {
                moveTo(
                    leftP1.x.toFloat(),
                    leftP1.y.toFloat()
                )
                cubicTo(
                    leftP2.x.toFloat(),
                    leftP2.y.toFloat(),
                    leftP3.x.toFloat(),
                    leftP3.y.toFloat(),
                    leftP4.x.toFloat(),
                    leftP4.y.toFloat()
                )

                moveTo(
                    rightP1.x.toFloat(),
                    rightP1.y.toFloat()
                )
                cubicTo(
                    rightP2.x.toFloat(),
                    rightP2.y.toFloat(),
                    rightP3.x.toFloat(),
                    rightP3.y.toFloat(),
                    rightP4.x.toFloat(),
                    rightP4.y.toFloat()
                )
            }

            fillPath.apply {
                moveTo(
                    leftP1.x.toFloat(),
                    leftP1.y.toFloat()
                )
                cubicTo(
                    leftP2.x.toFloat(),
                    leftP2.y.toFloat(),
                    leftP3.x.toFloat(),
                    leftP3.y.toFloat(),
                    leftP4.x.toFloat(),
                    leftP4.y.toFloat()
                )

                lineTo(
                    rightP4.x.toFloat(),
                    rightP4.y.toFloat()
                )
                cubicTo(
                    rightP3.x.toFloat(),
                    rightP3.y.toFloat(),
                    rightP2.x.toFloat(),
                    rightP2.y.toFloat(),
                    rightP1.x.toFloat(),
                    rightP1.y.toFloat()
                )
                close()
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // flip horizontally for BMW rear-oriented camera
        canvas.scale(-1f, 1f, width.toFloat() / 2, height.toFloat() / 2)
        canvas.drawPath(strokePath, strokePaint)
        canvas.drawPath(fillPath, fillPaint)

        super.onDraw(canvas)
    }
}
