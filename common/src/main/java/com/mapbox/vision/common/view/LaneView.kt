package com.mapbox.vision.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DimenRes
import com.mabpox.vision.teaser.common.R
import com.mapbox.vision.VisionReplayManager
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

    private fun PixelCoordinate.scale(): PixelCoordinate = PixelCoordinate(
        this.x.toFloat().scaleX().toInt(),
        this.y.toFloat().scaleY().toInt()
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateScale()
    }

    fun drawLane(lane: Lane?) {
        strokePath.reset()
        fillPath.reset()
        setBackgroundColor(transparent)

        lane?.let {
            val leftP1 = VisionReplayManager.worldToPixel(lane.leftEdge.curve.p1)?.scale() ?: return@let
            val leftP2 = VisionReplayManager.worldToPixel(lane.leftEdge.curve.p2)?.scale() ?: return@let
            val leftP3 = VisionReplayManager.worldToPixel(lane.leftEdge.curve.p3)?.scale() ?: return@let
            val leftP4 = VisionReplayManager.worldToPixel(lane.leftEdge.curve.p4)?.scale() ?: return@let
            val rightP1 = VisionReplayManager.worldToPixel(lane.rightEdge.curve.p1)?.scale() ?: return@let
            val rightP2 = VisionReplayManager.worldToPixel(lane.rightEdge.curve.p2)?.scale() ?: return@let
            val rightP3 = VisionReplayManager.worldToPixel(lane.rightEdge.curve.p3)?.scale() ?: return@let
            val rightP4 = VisionReplayManager.worldToPixel(lane.rightEdge.curve.p4)?.scale() ?: return@let

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
        canvas.drawPath(strokePath, strokePaint)
        canvas.drawPath(fillPath, fillPaint)

        super.onDraw(canvas)
    }
}
