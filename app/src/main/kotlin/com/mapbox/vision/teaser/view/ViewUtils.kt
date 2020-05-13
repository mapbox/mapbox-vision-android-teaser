package com.mapbox.vision.teaser.view

import android.view.View
import android.view.View.*

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
    if (visibility == GONE) show() else hide()
}

fun View.toggleVisibleInvisible() {
    if (visibility == INVISIBLE) show() else invisible()
}
