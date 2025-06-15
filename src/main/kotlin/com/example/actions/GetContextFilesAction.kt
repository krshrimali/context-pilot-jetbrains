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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.util.PathUtil
import org.json.JSONArray
import java.io.File
import java.nio.file.Paths
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon
import javax.swing.JList

data class RelatedFile(
    val path: String,
    val occurrences: Int,
    val isDirectory: Boolean = false
) {
    val name: String get() = File(path).name
    val parentPath: String get() = File(path).parent ?: ""
    
    // Add toString() for search functionality
    override fun toString(): String = "$name $parentPath"
}

class GetContextFilesAction : AnAction() {
    private val logger = Logger.getInstance(GetContextFilesAction::class.java)

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

        val command = "$binaryPath $absoluteWorkspacePath $absoluteFilePath -t query -s $startLine -e $endLine"
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
                val relatedFiles = output.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { line ->
                        val parts = line.split(" - ")
                        val filePath = parts[0].trim()
                        val occurrences = parts.getOrNull(1)?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        RelatedFile(filePath, occurrences)
                    }
                    .sortedByDescending { it.occurrences }

                if (relatedFiles.isEmpty()) {
                    Messages.showInfoMessage(project, "No related files found", "ContextPilot")
                    return
                }

                // Create a list component
                val list = JBList(relatedFiles)

                // Create a custom renderer for the list items
                list.cellRenderer = object : SimpleListCellRenderer<RelatedFile>() {
                    override fun customize(
                        list: JList<out RelatedFile>,
                        value: RelatedFile?,
                        index: Int,
                        selected: Boolean,
                        hasFocus: Boolean
                    ) {
                        if (value == null) return

                        // Set icon based on file type
                        icon = when {
                            value.isDirectory -> AllIcons.Nodes.Folder
                            value.name.endsWith(".kt") -> AllIcons.FileTypes.Text // Use generic text icon for Kotlin
                            value.name.endsWith(".java") -> AllIcons.FileTypes.Java
                            value.name.endsWith(".xml") -> AllIcons.FileTypes.Xml
                            value.name.endsWith(".gradle") -> AllIcons.FileTypes.Text // Use generic text icon for Gradle
                            else -> AllIcons.FileTypes.Any_type
                        }

                        // Create a formatted label with filename and path
                        val label = JBLabel().apply {
                            text = "<html><body>" +
                                    "<b>${value.name}</b> " +
                                    "<font color='#666666'>${value.parentPath}</font> " +
                                    "<font color='#999999'>(${value.occurrences} matches)</font>" +
                                    "</body></html>"
                            border = JBUI.Borders.empty(2)
                        }
                        
                        text = label.text
                    }
                }

                // Create and show the popup
                PopupChooserBuilder(list)
                    .setTitle("Related Files (${relatedFiles.size})")
                    .setItemChoosenCallback {
                        val selectedFile = list.selectedValue
                        if (selectedFile != null) {
                            val fullPath = Paths.get(workspacePath, selectedFile.path).normalize().toString()
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath)
                            if (virtualFile != null) {
                                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                            }
                        }
                    }
                    .createPopup()
                    .showInBestPositionFor(editor)

            } else {
                logger.error("Command failed with exit code $exitCode: $output")
                Messages.showErrorDialog(
                    project,
                    "Failed to get related files (exit code: $exitCode)\nOutput: $output",
                    "ContextPilot Error"
                )
            }
        } catch (ex: Exception) {
            logger.error("Error getting related files", ex)
            Messages.showErrorDialog(
                project,
                "Error getting related files: ${ex.message}",
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