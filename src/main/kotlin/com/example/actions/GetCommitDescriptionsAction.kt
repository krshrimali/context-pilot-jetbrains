package com.example.actions

import com.example.ContextPilotService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.application.ApplicationManager
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GetCommitDescriptionsAction : AnAction() {
    private val logger = Logger.getInstance(GetCommitDescriptionsAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
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

        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)?.path ?: return
        val workspacePath = project.basePath ?: return

        // Always use absolute paths
        val absoluteWorkspacePath = File(workspacePath).absolutePath
        val absoluteFilePath = File(currentFile).absolutePath

        // Get current line or selection
        val document = editor.document
        val selectionModel = editor.selectionModel
        val startLine = if (selectionModel.hasSelection()) {
            document.getLineNumber(selectionModel.selectionStart) + 1
        } else {
            editor.caretModel.logicalPosition.line + 1
        }
        val endLine = if (selectionModel.hasSelection()) {
            document.getLineNumber(selectionModel.selectionEnd) + 1
        } else {
            startLine
        }

        val command = "$binaryPath $absoluteWorkspacePath $absoluteFilePath -t desc -s $startLine -e $endLine"
        logger.info("Running command: $command")

        try {
            val process = ProcessBuilder(command.split(" "))
                .directory(File(workspacePath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            logger.info("Command output: $output")
            logger.info("Exit code: $exitCode")

            if (exitCode == 0) {
                try {
                    val jsonArray = JSONArray(output)
                    logger.info("Found ${jsonArray.length()} commits")

                    if (jsonArray.length() == 0) {
                        Messages.showInfoMessage(project, "No commits found for the selected code", "ContextPilot")
                        return
                    }

                    val markdownContent = StringBuilder()
                    markdownContent.append("# Commit History Analysis\n\n")
                    markdownContent.append("This file contains the relevant commit history for the selected code.\n\n")
                    markdownContent.append("File: `$absoluteFilePath`\n")
                    markdownContent.append("Lines: $startLine to $endLine\n\n")
                    markdownContent.append("---\n\n")

                    for (i in 0 until jsonArray.length()) {
                        val commitArray = jsonArray.getJSONArray(i)
                        val title = commitArray.getString(0)
                        val description = commitArray.getString(1)
                        val author = commitArray.getString(2)
                        val date = commitArray.getString(3)
                        val commitUrl = commitArray.getString(4)

                        // Format the date to be more readable
                        val parsedDate = try {
                            val instant = Instant.parse(date)
                            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(instant)
                        } catch (e: Exception) {
                            date // Fallback to original format if parsing fails
                        }

                        markdownContent.append("## ${title}\n\n")
                        markdownContent.append("**Author:** ${author}\n\n")
                        markdownContent.append("**Date:** ${parsedDate}\n\n")
                        if (description.isNotEmpty()) {
                            markdownContent.append("**Description:**\n${description}\n\n")
                        }
                        markdownContent.append("**Commit URL:** [View on GitHub](${commitUrl})\n\n")
                        markdownContent.append("---\n\n")
                    }

                    // Create a virtual file with markdown content
                    ApplicationManager.getApplication().invokeLater {
                        val fileName = "Commit History - ${File(absoluteFilePath).name}.md"
                        val virtualFile = LightVirtualFile(
                            fileName,
                            FileTypeManager.getInstance().getFileTypeByExtension("md"),
                            markdownContent.toString()
                        )
                        
                        // Open the file in the editor
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                } catch (ex: Exception) {
                    logger.error("Error processing commit descriptions", ex)
                    Messages.showErrorDialog(
                        project,
                        "Error processing commit descriptions: ${ex.message}",
                        "ContextPilot Error"
                    )
                }
            } else {
                logger.error("Command failed with exit code $exitCode: $output")
                Messages.showErrorDialog(
                    project,
                    "Failed to get commit information (exit code: $exitCode)\nOutput: $output",
                    "ContextPilot Error"
                )
            }
        } catch (ex: Exception) {
            logger.error("Error getting commit descriptions", ex)
            Messages.showErrorDialog(
                project,
                "Error getting commit descriptions: ${ex.message}",
                "ContextPilot Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = project != null && editor != null
    }
} 