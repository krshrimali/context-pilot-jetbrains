package com.example

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

class ContextPilotStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val service = ContextPilotService.getInstance(project)

        if (!service.checkContextPilotVersion()) {
            // Don't show error on startup, just return
            return
        }

        val binaryPath = service.getContextPilotPath() ?: return
        val workspacePath = project.basePath ?: return

        // Always use absolute paths
        val absoluteWorkspacePath = File(workspacePath).absolutePath

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Indexing Workspace", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                val command = "$binaryPath $absoluteWorkspacePath -t index"

                try {
                    val process = ProcessBuilder(command.split(" "))
                        .directory(File(workspacePath))
                        .redirectErrorStream(true)
                        .start()

                    var filesIndexed = 0
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (line?.contains("Indexing file:") == true) {
                                filesIndexed++
                                indicator.fraction = (filesIndexed % 100) / 100.0
                                indicator.text = "Indexed $filesIndexed files..."
                            }
                        }
                    }

                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        service.setLastIndexTime(System.currentTimeMillis() / 1000)
                    }
                } catch (ex: Exception) {
                    // Log error but don't show dialog on startup
                    ex.printStackTrace()
                }
            }
        })
    }
} 