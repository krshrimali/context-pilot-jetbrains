package com.example.actions

import com.example.ContextPilotService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader

class IndexWorkspaceAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = ContextPilotService.getInstance(project)

        if (!service.checkContextPilotVersion()) {
            Messages.showErrorDialog(
                project,
                "ContextPilot binary not found or version incompatible. Please check installation.",
                "ContextPilot Error"
            )
            return
        }

        val binaryPath = service.getContextPilotPath()
        if (binaryPath == null) {
            Messages.showErrorDialog(
                project,
                "ContextPilot binary path not found. This is unexpected as version check passed.",
                "ContextPilot Error"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Indexing Workspace", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                val workspacePath = project.basePath ?: return
                val command = "$binaryPath $workspacePath -t index"

                try {
                    val process = ProcessBuilder(command.split(" "))
                        .directory(project.basePath?.let { java.io.File(it) })
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
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                                project,
                                "Successfully indexed workspace ($filesIndexed files)",
                                "ContextPilot"
                            )
                        }
                    } else {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "Failed to index workspace (exit code: $exitCode)",
                                "ContextPilot Error"
                            )
                        }
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Error indexing workspace: ${ex.message}",
                            "ContextPilot Error"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
} 