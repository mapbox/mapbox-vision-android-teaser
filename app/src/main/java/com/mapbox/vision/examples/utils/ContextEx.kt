package com.mapbox.vision.examples.utils

import android.content.Context

fun Context.dpToPx(dp: Float) = dp * this.resources.displayMetrics.density

fun Context.pxToDp(px: Float) = px / this.resources.displayMetrics.density