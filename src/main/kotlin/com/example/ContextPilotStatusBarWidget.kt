package com.example

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.WidgetPresentation
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid

class ContextPilotStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {
    private var statusBar: StatusBar? = null

    override fun ID(): String = "ContextPilotWidget"

    override fun getPresentation(): WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getTooltipText(): String = "Click to show ContextPilot actions"

    override fun getSelectedValue(): String = "ContextPilot"

    override fun getIcon(): Icon = AllIcons.Actions.Search

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { event ->
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction("com.example.ContextPilot.MainMenu") as? ActionGroup ?: return@Consumer

        val popup: ListPopup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "ContextPilot Actions",
                group,
                DataContext.EMPTY_CONTEXT,
                ActionSelectionAid.SPEEDSEARCH,
                true,
                null
            )

        val component = event.component
        popup.showUnderneathOf(component)
    }
} 