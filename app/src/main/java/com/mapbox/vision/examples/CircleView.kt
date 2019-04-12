package com.mapbox.vision.examples

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import com.mapbox.vision.examples.utils.dpToPx

class CircleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val strokePx = context.dpToPx(2f)
    private val circleOffsetPx
        get() = strokePx

    private val paint = Paint(ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

    var circleColor = Color.parseColor("#00000000") // transparent
        set(value) {
            if (value == field) return
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(getBitmap(), 0f, 0f, null)
    }

    private fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvasBitmap = Canvas(bitmap)
        paint.color = circleColor

        canvasBitmap.drawCircle(
            width.toFloat() / 2,
            height.toFloat() / 2,
            (width.toFloat() - circleOffsetPx) / 2,
            paint
        )

        return bitmap
    }
}