package com.mapbox.vision.teaser.view

import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE

fun View.hide() {
    visibility = GONE
}

fun View.show() {
    visibility = VISIBLE
}

fun View.invisible() {
    visibility = INVISIBLE
}

fun View.toggleVisibleGone() {
    if (visibility == GONE) show() else if (visibility == VISIBLE) hide()
}

fun View.toggleVisibleInvisible() {
    if (visibility == INVISIBLE) show() else if (visibility == VISIBLE) invisible()
}
