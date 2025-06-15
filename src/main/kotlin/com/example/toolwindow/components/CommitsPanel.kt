package com.example.toolwindow.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.example.toolwindow.CommitInfo
import com.example.toolwindow.dialogs.CommitDetailsDialog
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class CommitsPanel(private val project: Project) : JPanel() {
    private val listModel = DefaultListModel<CommitInfo>()
    private val list = JBList(listModel).apply {
        cellRenderer = CommitCellRenderer()
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
                val selectedCommit = list.selectedValue
                if (selectedCommit != null) {
                    showCommitDetails(selectedCommit)
                }
            }
        }
    }

    private fun showCommitDetails(commit: CommitInfo) {
        CommitDetailsDialog(project, commit).show()
    }

    fun setCommits(commits: List<CommitInfo>) {
        listModel.clear()
        commits.forEach { listModel.addElement(it) }
    }
}

private class CommitCellRenderer : ListCellRenderer<CommitInfo> {
    private val panel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(8, 8, 8, 8)
    }
    private val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
    private val commitIcon = JLabel(AllIcons.Vcs.CommitNode)
    private val hashLabel = JLabel().apply {
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }
    private val messageLabel = JLabel().apply {
        font = UIUtil.getLabelFont()
    }
    private val detailsLabel = JLabel().apply {
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }

    init {
        headerPanel.add(commitIcon)
        headerPanel.add(hashLabel)
        headerPanel.isOpaque = false
    }

    override fun getListCellRendererComponent(
        list: JList<out CommitInfo>,
        value: CommitInfo,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        panel.background = if (isSelected) UIUtil.getListSelectionBackground(true)
            else UIUtil.getListBackground()
        
        val textColor = if (isSelected) UIUtil.getListSelectionForeground(true)
            else UIUtil.getListForeground()
        
        // Set commit hash
        hashLabel.text = value.hash.substring(0, 7)
        hashLabel.foreground = if (isSelected) textColor
            else UIUtil.getContextHelpForeground()

        // Set commit message
        messageLabel.text = value.message.lines().first() // Show first line only
        messageLabel.foreground = textColor

        // Set commit details
        detailsLabel.text = "${value.author} on ${value.date}"
        detailsLabel.foreground = if (isSelected) textColor
            else UIUtil.getContextHelpForeground()

        // Layout components
        panel.removeAll()
        panel.add(headerPanel, BorderLayout.NORTH)
        panel.add(messageLabel, BorderLayout.CENTER)
        panel.add(detailsLabel, BorderLayout.SOUTH)

        return panel
    }
} 