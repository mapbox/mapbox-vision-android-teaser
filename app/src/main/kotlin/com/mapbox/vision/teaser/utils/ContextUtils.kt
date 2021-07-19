package com.mapbox.vision.teaser.utils

import android.content.Context
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.Rounding
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.vision.teaser.R

fun Context.dpToPx(dp: Float) = dp * this.resources.displayMetrics.density

fun Context.pxToDp(px: Float) = px / this.resources.displayMetrics.density

enum class ResourceType(val typeName: kotlin.String) {
    Drawable("drawable"),
    String("string")
}

fun Context.getResId(name: String, type: ResourceType) = resources.getIdentifier(
    name, type.typeName, packageName
)

fun Context.buildNavigationOptions() = NavigationOptions.Builder(applicationContext)
    .distanceFormatter(
        buildMapboxDistanceFormatter()
    )
    .accessToken(
        getString(R.string.mapbox_access_token)
    )
    .build()

fun Context.buildMapboxDistanceFormatter() =
    MapboxDistanceFormatter.Builder(applicationContext)
        .locale(
            inferDeviceLocale(),
        )
        .roundingIncrement(
            Rounding.INCREMENT_FIVE
        )
        .unitType(
            inferDeviceLocale().getUnitTypeForLocale(),
        )
        .build()

