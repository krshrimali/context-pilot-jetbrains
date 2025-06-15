package com.example.toolwindow.components

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.example.toolwindow.DiffInfo
import com.intellij.openapi.fileTypes.FileTypes
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.border.EmptyBorder

class DiffPanel(private val project: Project) : JPanel() {
    private val listModel = DefaultListModel<DiffInfo>()
    private val list = JBList(listModel).apply {
        cellRenderer = DiffCellRenderer()
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
                val selectedDiff = list.selectedValue
                if (selectedDiff != null) {
                    showDiff(selectedDiff)
                }
            }
        }
    }

    fun setDiffs(diffs: List<DiffInfo>) {
        listModel.clear()
        diffs.forEach { listModel.addElement(it) }
    }

    private fun showDiff(diffInfo: DiffInfo) {
        val change = diffInfo.change
        val contentFactory = DiffContentFactory.getInstance()

        val beforeContent = change.beforeRevision?.let { 
            contentFactory.create(it.content ?: "", it.file?.fileType ?: FileTypes.PLAIN_TEXT)
        } ?: contentFactory.createEmpty()
        
        val afterContent = change.afterRevision?.let {
            contentFactory.create(it.content ?: "", it.file?.fileType ?: FileTypes.PLAIN_TEXT)
        } ?: contentFactory.createEmpty()

        val request = SimpleDiffRequest(
            diffInfo.title,
            beforeContent,
            afterContent,
            change.beforeRevision?.file?.name ?: "Base version",
            change.afterRevision?.file?.name ?: "Current version"
        )

        DiffManager.getInstance().showDiff(project, request)
    }
}

private class DiffCellRenderer : ListCellRenderer<DiffInfo> {
    private val panel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(8, 8, 8, 8)
    }
    private val titleLabel = JLabel().apply {
        font = UIUtil.getLabelFont()
    }

    override fun getListCellRendererComponent(
        list: JList<out DiffInfo>,
        value: DiffInfo,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        panel.background = if (isSelected) UIUtil.getListSelectionBackground(true)
            else UIUtil.getListBackground()
        titleLabel.foreground = if (isSelected) UIUtil.getListSelectionForeground(true)
            else UIUtil.getListForeground()

        titleLabel.text = value.title
        panel.removeAll()
        panel.add(titleLabel, BorderLayout.CENTER)

        return panel
    }
} 