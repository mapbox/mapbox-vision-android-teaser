package com.mapbox.vision.examples

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.support.annotation.DimenRes
import android.support.v4.math.MathUtils
import android.util.AttributeSet
import android.view.View
import com.mapbox.vision.VisionManager
import com.mapbox.vision.visionevents.events.detection.Collision
import com.mapbox.vision.visionevents.events.worlddescription.ObjectDescription

class DistanceToCarView : View {

    companion object {
        private const val DISTANCE_BASE_RANGE_METERS = 40f
    }

    private enum class Mode {
        DISTANCE,
        WARNING,
        CRITICAL
    }

    private var mode = Mode.DISTANCE

    private val distancePath = Path()
    private var center: PointF = PointF(0f, 0f)
    private var radius: Float = 0f
    private var widthRatio = 1f
    private var heightRatio = 1f

    private val distancePaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val collisionPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val criticalDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_critical)!!
    private val alertDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_alert)!!

    private val alertSize: Int
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
        alertSize = pixels(R.dimen.alert_size)
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
        widthRatio = width.toFloat() / frameSize.width
        heightRatio = height.toFloat() / frameSize.height
    }

    fun drawCollision(collision: Collision) {
        distancePath.reset()
        mode = when (collision.state) {
            Collision.CollisionState.NOT_TRIGGERED -> {
                setBackgroundColor(transparent)
                drawDistanceToCar(collision.car)
                Mode.DISTANCE
            }
            Collision.CollisionState.WARNING -> {
                setBackgroundColor(transparent)
                drawWarning(collision.car)
                Mode.WARNING
            }
            Collision.CollisionState.CRITICAL -> {
                setBackgroundColor(dark)
                Mode.CRITICAL
            }
        }

        invalidate()
    }

    private fun drawDistanceToCar(car: ObjectDescription) {
        distancePath.reset()

        val widthDelta = MathUtils.clamp(
                (distanceRectBaseWidth / DISTANCE_BASE_RANGE_METERS * car.distance).toFloat(),
                distanceRectBaseWidthMin,
                distanceRectBaseWidthMax
        )

        val left = car.detection.boundingBox.left * widthRatio
        val top = car.detection.boundingBox.bottom * heightRatio
        val bottom = car.detection.boundingBox.bottom * heightRatio + distanceRectHeight
        val right = car.detection.boundingBox.right * widthRatio + widthDelta
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
    }


    private fun drawWarning(car: ObjectDescription) {
        center.x = (car.detection.boundingBox.left + car.detection.boundingBox.right) / 2 * widthRatio
        center.y = (car.detection.boundingBox.bottom + car.detection.boundingBox.top) / 2 * heightRatio
        radius = (car.detection.boundingBox.right - car.detection.boundingBox.left) * heightRatio
        collisionPaint.shader = getWarningShader(
                centerX = center.x,
                centerY = center.y,
                radius = radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        when (mode) {
            DistanceToCarView.Mode.DISTANCE -> {
                canvas.drawPath(distancePath, distancePaint)
            }
            DistanceToCarView.Mode.WARNING -> {
                canvas.drawCircle(
                        center.x, center.y, radius, collisionPaint
                )
                alertDrawable.bounds.set(
                        center.x.toInt() - alertSize,
                        center.y.toInt() - alertSize,
                        center.x.toInt() + alertSize,
                        center.y.toInt() + alertSize
                )
                alertDrawable.draw(canvas)
                criticalDrawable.bounds.set(
                        (width - criticalAlertWidth) / 2,
                        criticalAlertMargin,
                        (width + criticalAlertWidth) / 2,
                        criticalAlertMargin + criticalAlertHeight
                )
                criticalDrawable.draw(canvas)
            }
            DistanceToCarView.Mode.CRITICAL -> {
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
