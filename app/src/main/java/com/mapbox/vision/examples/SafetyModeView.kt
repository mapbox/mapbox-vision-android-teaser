package com.mapbox.vision.examples

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.annotation.DimenRes
import android.support.v4.math.MathUtils
import android.util.AttributeSet
import android.view.View
import com.mapbox.vision.VisionManager
import com.mapbox.vision.visionevents.FrameSize
import com.mapbox.vision.visionevents.events.worlddescription.ObjectDescription

class SafetyModeView : View {

    companion object {
        private const val DISTANCE_BASE_RANGE_METERS = 40f
    }

    private enum class Mode {
        DISTANCE,
        WARNING,
        CRITICAL
    }

    private data class WarningShape(
        val center: PointF,
        val radius: Float
    )

    private var mode = Mode.DISTANCE

    private var scaleFactor = 1f
    private var scaledSize = FrameSize(1, 1)

    private val distancePath = Path()
    private var warningShapes: List<WarningShape> = emptyList()

    private val distancePaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val collisionPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val criticalDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_critical)!!
    private val alertDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_alert)!!

    private val alertWidth: Int
    private val alertHeight: Int
    private val criticalAlertWidth: Int
    private val criticalAlertHeight: Int
    private val criticalAlertMargin: Int
    private val criticalMarginHorizontal: Int
    private val criticalMarginVertical: Int

    private val distanceRectHeight: Float
    private val distanceRectBaseWidth: Float
    private val distanceRectBaseWidthMin: Float
    private val distanceRectBaseWidthMax: Float

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        alertWidth = pixels(R.dimen.alert_width) / 2
        alertHeight = pixels(R.dimen.alert_height) / 2
        criticalAlertWidth = pixels(R.dimen.critical_alert_width)
        criticalAlertHeight = pixels(R.dimen.critical_alert_height)
        criticalAlertMargin = pixels(R.dimen.critical_alert_margin)
        criticalMarginHorizontal = pixels(R.dimen.critical_margin_horizontal)
        criticalMarginVertical = pixels(R.dimen.critical_margin_vertical)

        distanceRectHeight = pixels(R.dimen.distance_rect_height).toFloat()
        distanceRectBaseWidth = pixels(R.dimen.distance_rect_base).toFloat()
        distanceRectBaseWidthMin = pixels(R.dimen.distance_rect_base_width_min).toFloat()
        distanceRectBaseWidthMax = pixels(R.dimen.distance_rect_base_width_max).toFloat()
    }

    private fun View.pixels(@DimenRes res: Int) = context.resources.getDimensionPixelSize(res)

    private val transparent = resources.getColor(android.R.color.transparent, null)
    private val dark = resources.getColor(R.color.black_70_transparent, null)

    private fun getDistanceShader(top: Float, bottom: Float) = LinearGradient(
        0f, top, 0f, bottom,
        resources.getColor(R.color.minty_green, null),
        transparent,
        Shader.TileMode.REPEAT
    )

    private fun getWarningShader(centerX: Float, centerY: Float, radius: Float) = RadialGradient(
        centerX, centerY, radius,
        resources.getColor(R.color.neon_red, null),
        transparent,
        Shader.TileMode.REPEAT
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val frameSize = VisionManager.getFrameSize()
        scaleFactor = Math.max(
            width.toFloat() / frameSize.width,
            height.toFloat() / frameSize.height
        )
        scaledSize = frameSize * scaleFactor
    }

    private fun Int.scaleX(): Float = this * scaleFactor - (scaledSize.width - width) / 2

    private fun Int.scaleY(): Float = this * scaleFactor - (scaledSize.height - height) / 2

    fun drawDistanceToCar(car: ObjectDescription) {
        distancePath.reset()
        mode = Mode.DISTANCE
        setBackgroundColor(transparent)

        val widthDelta = MathUtils.clamp(
            (distanceRectBaseWidth / DISTANCE_BASE_RANGE_METERS * car.distance).toFloat(),
            distanceRectBaseWidthMin,
            distanceRectBaseWidthMax
        )

        val left = car.detection.boundingBox.left.scaleX() - widthDelta
        val top = car.detection.boundingBox.bottom.scaleY()
        val right = car.detection.boundingBox.right.scaleX() + widthDelta
        val bottom = car.detection.boundingBox.bottom.scaleY() + distanceRectHeight

        if (car.detection.boundingBox.left != 0 && car.detection.boundingBox.right != 0) {
            distancePath.moveTo(
                left + widthDelta,
                top
            )
            distancePath.lineTo(
                left,
                bottom
            )
            distancePath.lineTo(
                right,
                bottom
            )
            distancePath.lineTo(
                right - widthDelta,
                top
            )
            distancePath.close()
            distancePaint.shader = getDistanceShader(top, bottom)
        }

        invalidate()
    }

    fun drawWarnings(objectDescriptions: List<ObjectDescription>) {
        distancePath.reset()
        mode = Mode.WARNING
        setBackgroundColor(transparent)

        warningShapes = objectDescriptions.map { objectDescription ->
            WarningShape(
                center = PointF(
                    ((objectDescription.detection.boundingBox.left + objectDescription.detection.boundingBox.right) / 2).scaleX(),
                    ((objectDescription.detection.boundingBox.bottom + objectDescription.detection.boundingBox.top) / 2).scaleY()
                ),
                radius = (objectDescription.detection.boundingBox.right - objectDescription.detection.boundingBox.left) * scaleFactor
            )
        }

        invalidate()
    }

    fun drawCritical() {
        distancePath.reset()
        setBackgroundColor(dark)
        mode = Mode.CRITICAL

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        when (mode) {
            SafetyModeView.Mode.DISTANCE -> {
                canvas.drawPath(distancePath, distancePaint)
            }
            SafetyModeView.Mode.WARNING -> {
                for (warning in warningShapes) {
                    collisionPaint.shader = getWarningShader(
                        centerX = warning.center.x,
                        centerY = warning.center.y,
                        radius = warning.radius
                    )
                    canvas.drawCircle(
                        warning.center.x, warning.center.y, warning.radius, collisionPaint
                    )
                    alertDrawable.bounds.set(
                        warning.center.x.toInt() - alertWidth,
                        warning.center.y.toInt() - alertHeight,
                        warning.center.x.toInt() + alertWidth,
                        warning.center.y.toInt() + alertHeight
                    )
                    alertDrawable.draw(canvas)
                }

                criticalDrawable.bounds.set(
                    (width - criticalAlertWidth) / 2,
                    criticalAlertMargin,
                    (width + criticalAlertWidth) / 2,
                    criticalAlertMargin + criticalAlertHeight
                )
                criticalDrawable.draw(canvas)
            }
            SafetyModeView.Mode.CRITICAL -> {
                criticalDrawable.bounds.set(
                    criticalMarginHorizontal,
                    criticalMarginVertical,
                    width - criticalMarginHorizontal,
                    height - criticalMarginVertical
                )
                criticalDrawable.draw(canvas)
            }
        }

        super.onDraw(canvas)
    }
}
