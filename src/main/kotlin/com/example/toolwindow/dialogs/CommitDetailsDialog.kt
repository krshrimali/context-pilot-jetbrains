package com.example.toolwindow.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.example.toolwindow.CommitInfo
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.border.EmptyBorder

class CommitDetailsDialog(
    project: Project,
    private val commitInfo: CommitInfo
) : DialogWrapper(project) {

    init {
        title = "Commit Details"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10)).apply {
            border = JBUI.Borders.empty(10)
            preferredSize = Dimension(600, 400)
        }

        // Header panel with commit hash and author
        val headerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 0, 10, 0)
            
            // Hash and copy button
            val hashPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            val hashLabel = JBLabel(commitInfo.hash).apply {
                font = UIUtil.getLabelFont().deriveFont(Font.BOLD)
                icon = AllIcons.Vcs.CommitNode
            }
            val copyButton = JButton(AllIcons.Actions.Copy).apply {
                toolTipText = "Copy commit hash"
                addActionListener {
                    // Copy hash to clipboard using IntelliJ's API
                    CopyPasteManager.getInstance().setContents(StringSelection(commitInfo.hash))
                }
            }
            hashPanel.add(hashLabel)
            hashPanel.add(copyButton)
            
            // Author and date
            val authorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            val authorLabel = JBLabel(commitInfo.author).apply {
                icon = AllIcons.Vcs.Author
            }
            val dateLabel = JBLabel(commitInfo.date).apply {
                icon = AllIcons.Vcs.History
            }
            authorPanel.add(authorLabel)
            authorPanel.add(dateLabel)
            
            add(hashPanel, BorderLayout.NORTH)
            add(authorPanel, BorderLayout.CENTER)
        }

        // Message panel
        val messagePanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            
            val messageLabel = JBLabel("Commit Message:").apply {
                font = UIUtil.getLabelFont().deriveFont(Font.BOLD)
            }
            
            val messageArea = JBTextArea(commitInfo.message).apply {
                isEditable = false
                wrapStyleWord = true
                lineWrap = true
                background = UIUtil.getPanelBackground()
                border = JBUI.Borders.empty(5)
                font = UIUtil.getLabelFont()
            }
            
            add(messageLabel, BorderLayout.NORTH)
            add(JBScrollPane(messageArea), BorderLayout.CENTER)
        }

        // Add all components
        panel.add(headerPanel, BorderLayout.NORTH)
        panel.add(messagePanel, BorderLayout.CENTER)

        return panel
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)
} 