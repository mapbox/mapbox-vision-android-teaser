package com.mapbox.vision.teaser.utils

import java.io.File
import java.lang.RuntimeException

object FileUtils {

    fun deleteFolder(folder: File) {
        val subFiles = folder.listFiles()
        if (subFiles.isNotEmpty()) {
            for (subFile in subFiles) {
                if (subFile.isDirectory) {
                    deleteFolder(subFile)
                } else {
                    if (!subFile.delete()) {
                        throw RuntimeException("Error: Can't delete ${subFile.absoluteFile}")
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw RuntimeException("Error: Can't delete ${folder.absoluteFile}")
        }
    }
}