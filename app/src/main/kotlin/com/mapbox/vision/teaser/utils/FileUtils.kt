package com.mapbox.vision.teaser.utils

import java.io.File

object FileUtils {

    fun isDirectoryContainsFiles(path: String) = File(path).listFiles()?.isNotEmpty() ?: false
}
