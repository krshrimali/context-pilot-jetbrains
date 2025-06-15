package com.example

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.ide.DataManager
import java.io.File
import java.nio.file.Paths
import java.awt.event.MouseEvent

@Service(Service.Level.PROJECT)
class ContextPilotService(private val project: Project) {
    private val logger = Logger.getInstance(ContextPilotService::class.java)
    private var lastIndexTime: Long? = null
    private var cachedVersionCheck: Boolean? = null
    private var contextPilotPath: String? = null
    private val MIN_CONTEXTPILOT_VERSION = "0.9.0"

    companion object {
        fun getInstance(project: Project): ContextPilotService = project.service()
    }

    fun parseVersion(versionStr: String): Triple<Int, Int, Int> {
        val match = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(versionStr)
        return if (match != null) {
            Triple(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt()
            )
        } else {
            Triple(0, 0, 0)
        }
    }

    fun isVersionCompatible(installed: String, required: String): Boolean {
        val (imaj, imin, ipat) = parseVersion(installed)
        val (rmaj, rmin, rpat) = parseVersion(required)
        if (imaj != rmaj) return imaj > rmaj
        if (imin != rmin) return imin > rmin
        return ipat >= rpat
    }

    fun getContextPilotPath(): String? = contextPilotPath

    fun checkContextPilotVersion(): Boolean {
        if (cachedVersionCheck != null && contextPilotPath != null) {
            return cachedVersionCheck!!
        }

        // Common installation paths to check
        val possiblePaths = listOf(
            "contextpilot", // Check PATH
            "${System.getProperty("user.home")}/.local/bin/contextpilot",
            "${System.getProperty("user.home")}/.cargo/bin/contextpilot",
            "/usr/local/bin/contextpilot",
            "/usr/bin/contextpilot"
        )

        for (path in possiblePaths) {
            try {
                val process = ProcessBuilder(path, "--version")
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    val match = Regex("contextpilot\\s+(\\d+\\.\\d+\\.\\d+)").find(output)
                    if (match != null) {
                        val version = match.groupValues[1]
                        if (isVersionCompatible(version, MIN_CONTEXTPILOT_VERSION)) {
                            logger.info("Found contextpilot v$version at $path")
                            cachedVersionCheck = true
                            contextPilotPath = path
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                logger.debug("Failed to check version for path: $path", e)
            }
        }

        logger.warn("ContextPilot binary not found or version incompatible")
        cachedVersionCheck = false
        contextPilotPath = null
        return false
    }

    fun getLastIndexTime(): Long? = lastIndexTime

    fun setLastIndexTime(time: Long) {
        lastIndexTime = time
    }
} 