package com.example.toolwindow.components

import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.example.toolwindow.ContextFileInfo
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.util.IconLoader
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class ContextFilesPanel(private val project: Project) : JPanel() {
    private val listModel = DefaultListModel<ContextFileInfo>()
    private val list = JBList(listModel).apply {
        cellRenderer = ContextFileCellRenderer(project)
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    init {
        layout = BorderLayout()
        background = UIUtil.getListBackground()
        border = JBUI.Borders.empty(10)

        // Add list with scroll
        add(JBScrollPane(list), BorderLayout.CENTER)

        // Add list selection listener
        list.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedFile = list.selectedValue
                if (selectedFile != null) {
                    FileEditorManager.getInstance(project).openFile(selectedFile.file, true)
                }
            }
        }
    }

    fun setContextFiles(files: List<ContextFileInfo>) {
        listModel.clear()
        files.forEach { listModel.addElement(it) }
    }
}

private class ContextFileCellRenderer(private val project: Project) : ListCellRenderer<ContextFileInfo> {
    private val panel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(8, 8, 8, 8)
    }
    private val filePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
    private val fileIcon = JLabel()
    private val fileName = JLabel()
    private val relevanceLabel = JLabel().apply {
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }
    private val reasonLabel = JLabel().apply {
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }

    init {
        filePanel.add(fileIcon)
        filePanel.add(fileName)
        filePanel.isOpaque = false
    }

    override fun getListCellRendererComponent(
        list: JList<out ContextFileInfo>,
        value: ContextFileInfo,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        panel.background = if (isSelected) UIUtil.getListSelectionBackground(true)
            else UIUtil.getListBackground()
        
        val textColor = if (isSelected) UIUtil.getListSelectionForeground(true)
            else UIUtil.getListForeground()
        
        // Set file icon and name
        fileIcon.icon = value.file.fileType.icon
            ?: AllIcons.FileTypes.Any_type
        fileName.text = value.file.name
        fileName.foreground = textColor

        // Set relevance score with progress bar
        val relevancePercent = (value.relevanceScore * 100).toInt()
        relevanceLabel.text = "Relevance: $relevancePercent%"
        relevanceLabel.foreground = textColor

        // Set reason with icon
        reasonLabel.icon = AllIcons.General.Information
        reasonLabel.text = value.reason
        reasonLabel.foreground = if (isSelected) textColor
            else UIUtil.getContextHelpForeground()

        // Layout components
        panel.removeAll()
        panel.add(filePanel, BorderLayout.NORTH)
        panel.add(relevanceLabel, BorderLayout.CENTER)
        panel.add(reasonLabel, BorderLayout.SOUTH)

        return panel
    }
} 