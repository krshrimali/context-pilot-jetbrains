package com.example.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.example.toolwindow.components.DiffPanel
import com.example.toolwindow.components.ContextFilesPanel
import com.example.toolwindow.components.CommitsPanel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ContextPilotToolWindowContent(private val project: Project) : JPanel() {
    private val loadingPanel: JBLoadingPanel
    private val tabbedPane: JBTabbedPane
    private var messageBusConnection: MessageBusConnection? = null
    private val contextAnalyzer = ContextAnalyzer(project)
    private val diffPanel = DiffPanel(project)
    private val contextFilesPanel = ContextFilesPanel(project)
    private val commitsPanel = CommitsPanel(project)
    
    init {
        layout = BorderLayout()
        
        // Create loading panel
        loadingPanel = JBLoadingPanel(BorderLayout(), project).apply {
            setLoadingText("Analyzing context...")
        }
        
        // Create tabbed pane with sections
        tabbedPane = JBTabbedPane().apply {
            add("Diffs", diffPanel)
            add("Context Files", contextFilesPanel)
            add("Related Commits", commitsPanel)
        }
        
        // Add components
        loadingPanel.add(tabbedPane, BorderLayout.CENTER)
        add(loadingPanel, BorderLayout.CENTER)
        
        // Setup file change listener
        setupFileChangeListener()
    }
    
    private fun setupFileChangeListener() {
        messageBusConnection = project.messageBus.connect()
        messageBusConnection?.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    updateContent(file)
                }
                
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    event.newFile?.let { updateContent(it) }
                }
            }
        )
    }
    
    private fun updateContent(file: VirtualFile) {
        SwingUtilities.invokeLater {
            loadingPanel.startLoading()
            
            contextAnalyzer.analyze(file) { result ->
                diffPanel.setDiffs(result.diffs)
                contextFilesPanel.setContextFiles(result.contextFiles)
                commitsPanel.setCommits(result.commits)
                loadingPanel.stopLoading()
            }
        }
    }
    
    fun dispose() {
        messageBusConnection?.disconnect()
        contextAnalyzer.dispose()
    }
} 