package app.gamenative.linux.desktop.shell

import app.gamenative.linux.desktop.shell.ui.DesktopShellSwingTheme
import app.gamenative.linux.desktop.shell.ui.DesktopShellThemeTokens
import app.gamenative.linux.desktop.shell.ui.DesktopShellUiPrimitives
import app.gamenative.linux.runtime.RuntimeBackend
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        DesktopShellWindow().show()
    }
}

private class DesktopShellWindow {
    private val controller = DesktopShellController()

    private val frame = JFrame("GameNative Linux Desktop Shell")
    private val diagnosticsArea = DesktopShellUiPrimitives.outputArea(8, 80)
    private val profileModel = DefaultListModel<String>()
    private val profileList = JList(profileModel)

    private val idField = DesktopShellUiPrimitives.inputField("default", 20)
    private val nameField = DesktopShellUiPrimitives.inputField("Default", 20)
    private val wineField = DesktopShellUiPrimitives.inputField("wine", 20)
    private val backendBox = JComboBox(RuntimeBackend.entries.toTypedArray())

    private val sessionArea = DesktopShellUiPrimitives.outputArea(5, 80)
    private val usernameField = DesktopShellUiPrimitives.inputField("", 20)
    private val passwordField = DesktopShellUiPrimitives.inputField("", 20)

    private val libraryArea = DesktopShellUiPrimitives.outputArea(10, 80)
    private val gameDetailArea = DesktopShellUiPrimitives.outputArea(7, 80)
    private val detailAppIdField = DesktopShellUiPrimitives.inputField("620", 8)
    private val appIdField = DesktopShellUiPrimitives.inputField("620", 8)
    private val queueArea = DesktopShellUiPrimitives.outputArea(6, 80)
    private val taskArea = DesktopShellUiPrimitives.outputArea(8, 80)

    private val sessionMonitorArea = DesktopShellUiPrimitives.outputArea(8, 80)
    private val settingsArea = DesktopShellUiPrimitives.outputArea(6, 80)
    private val downloadRootField = DesktopShellUiPrimitives.inputField("~/.local/share/gamenative/games", 40)
    private val pollingField = DesktopShellUiPrimitives.inputField("10", 6)

    init {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(1120, 760)
        frame.setLocationRelativeTo(null)

        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.border = BorderFactory.createEmptyBorder(
            DesktopShellThemeTokens.current.spacing.lg,
            DesktopShellThemeTokens.current.spacing.lg,
            DesktopShellThemeTokens.current.spacing.lg,
            DesktopShellThemeTokens.current.spacing.lg,
        )

        val tabs = JTabbedPane()

        val refreshButton = DesktopShellUiPrimitives.actionButton("Refresh Diagnostics + Profiles") { refreshState() }

        val diagnosticsPane = DesktopShellUiPrimitives.scroll(diagnosticsArea)
        diagnosticsPane.border = DesktopShellSwingTheme.sectionBorder("First-Run Diagnostics")
        val diagnosticsPanel = DesktopShellUiPrimitives.screenPanel()
        diagnosticsPanel.add(refreshButton)
        diagnosticsPanel.add(diagnosticsPane)
        tabs.addTab("Diagnostics", JScrollPane(diagnosticsPanel))

        val profilePane = JScrollPane(profileList)
        profilePane.border = DesktopShellSwingTheme.sectionBorder("Runtime Profiles")

        val editor = DesktopShellUiPrimitives.screenPanel()
        editor.border = DesktopShellSwingTheme.sectionBorder("Profile Editor")
        editor.add(DesktopShellUiPrimitives.fieldLabel("Profile ID"))
        editor.add(idField)
        editor.add(DesktopShellUiPrimitives.fieldLabel("Name"))
        editor.add(nameField)
        editor.add(DesktopShellUiPrimitives.fieldLabel("Wine Binary"))
        editor.add(wineField)
        editor.add(DesktopShellUiPrimitives.fieldLabel("Backend"))
        editor.add(backendBox)

        val saveButton = DesktopShellUiPrimitives.actionButton("Save Profile") { saveProfile() }
        editor.add(saveButton)

        val profilePanel = DesktopShellUiPrimitives.screenPanel()
        profilePanel.add(profilePane)
        profilePanel.add(editor)
        tabs.addTab("Profiles", JScrollPane(profilePanel))

        val accountPanel = DesktopShellUiPrimitives.screenPanel()
        accountPanel.border = DesktopShellSwingTheme.sectionBorder("Steam Account Sign-In")
        accountPanel.add(DesktopShellUiPrimitives.fieldLabel("Username"))
        accountPanel.add(usernameField)
        accountPanel.add(DesktopShellUiPrimitives.fieldLabel("Password"))
        accountPanel.add(passwordField)
        val loginButton = DesktopShellUiPrimitives.actionButton("Login") { login() }
        val logoutButton = DesktopShellUiPrimitives.actionButton("Logout") { logout() }
        accountPanel.add(loginButton)
        accountPanel.add(logoutButton)
        accountPanel.add(DesktopShellUiPrimitives.scroll(sessionArea))
        tabs.addTab("Account", JScrollPane(accountPanel))

        val libraryPanel = DesktopShellUiPrimitives.screenPanel()
        val refreshLibraryButton = DesktopShellUiPrimitives.actionButton("Refresh Owned Apps") { refreshLibrary() }
        val openDetailsButton = DesktopShellUiPrimitives.actionButton("Open Game Details") { openGameDetails() }
        libraryPanel.add(refreshLibraryButton)
        libraryPanel.add(DesktopShellUiPrimitives.fieldLabel("Game Detail App ID"))
        libraryPanel.add(detailAppIdField)
        libraryPanel.add(openDetailsButton)
        libraryPanel.add(DesktopShellUiPrimitives.scroll(libraryArea))
        tabs.addTab("Library", JScrollPane(libraryPanel))

        val gameDetailPanel = DesktopShellUiPrimitives.screenPanel()
        gameDetailPanel.add(DesktopShellUiPrimitives.fieldLabel("Game Detail"))
        gameDetailPanel.add(DesktopShellUiPrimitives.scroll(gameDetailArea))
        tabs.addTab("Game Detail", JScrollPane(gameDetailPanel))

        val downloadsPanel = DesktopShellUiPrimitives.screenPanel()
        downloadsPanel.add(DesktopShellUiPrimitives.fieldLabel("App ID"))
        downloadsPanel.add(appIdField)
        val enqueueButton = DesktopShellUiPrimitives.actionButton("Enqueue") { mutateQueue("enqueue") }
        val pauseButton = DesktopShellUiPrimitives.actionButton("Pause") { mutateQueue("pause") }
        val cancelButton = DesktopShellUiPrimitives.actionButton("Cancel") { mutateQueue("cancel") }
        downloadsPanel.add(enqueueButton)
        downloadsPanel.add(pauseButton)
        downloadsPanel.add(cancelButton)
        downloadsPanel.add(DesktopShellUiPrimitives.scroll(queueArea))
        tabs.addTab("Downloads", JScrollPane(downloadsPanel))

        val tasksPanel = DesktopShellUiPrimitives.screenPanel()
        val refreshTasksButton = DesktopShellUiPrimitives.actionButton("Refresh Tasks") { refreshState() }
        tasksPanel.add(refreshTasksButton)
        tasksPanel.add(DesktopShellUiPrimitives.scroll(taskArea))
        tabs.addTab("Background Tasks", JScrollPane(tasksPanel))

        val sessionMonitorPanel = DesktopShellUiPrimitives.screenPanel()
        sessionMonitorPanel.add(DesktopShellUiPrimitives.fieldLabel("Latest Runtime Session Timeline"))
        sessionMonitorPanel.add(DesktopShellUiPrimitives.scroll(sessionMonitorArea))
        tabs.addTab("Session Monitor", JScrollPane(sessionMonitorPanel))

        val settingsPanel = DesktopShellUiPrimitives.screenPanel()
        settingsPanel.add(DesktopShellUiPrimitives.fieldLabel("Settings / Runtime Paths"))
        settingsPanel.add(DesktopShellUiPrimitives.fieldLabel("Download Root"))
        settingsPanel.add(downloadRootField)
        settingsPanel.add(DesktopShellUiPrimitives.fieldLabel("Diagnostics Polling Seconds"))
        settingsPanel.add(pollingField)
        val saveSettingsButton = DesktopShellUiPrimitives.actionButton("Save Settings") { saveSettings() }
        settingsPanel.add(saveSettingsButton)
        settingsPanel.add(DesktopShellUiPrimitives.scroll(settingsArea))
        tabs.addTab("Settings", JScrollPane(settingsPanel))

        root.add(tabs)

        frame.contentPane = JScrollPane(root)
        DesktopShellSwingTheme.apply(frame)
        refreshState()
    }

    fun show() {
        frame.isVisible = true
    }

    private fun refreshState() {
        val state = controller.refreshState()
        diagnosticsArea.text = buildString {
            appendLine("wineAvailable=${state.diagnostics.wineAvailable}")
            appendLine("box64Available=${state.diagnostics.box64Available}")
            appendLine("fexAvailable=${state.diagnostics.fexAvailable}")
            appendLine("vulkanAvailable=${state.diagnostics.vulkanAvailable}")
            appendLine("diagnostics=${state.diagnostics.diagnostics.joinToString("; ")}")
        }

        profileModel.clear()
        state.profiles.forEach {
            profileModel.addElement("${it.id} | ${it.name} | ${it.backend} | ${it.wineBinary}")
        }

        val session = state.session
        sessionArea.text = if (session == null) {
            "No session info"
        } else {
            "state=${session.state}\naccount=${session.accountName ?: ""}\nmessage=${session.message ?: ""}"
        }

        libraryArea.text = state.library.joinToString("\n") { app -> "${app.id} | ${app.name}" }
        gameDetailArea.text = state.selectedGame?.let { game ->
            buildString {
                appendLine("appId=${game.appId}")
                appendLine("name=${game.name}")
                appendLine("releaseState=${game.releaseState}")
                appendLine("isFree=${game.isFree}")
                appendLine("headerUrl=${game.headerUrl}")
            }
        } ?: "Select a game id and click Open Game Details."
        queueArea.text = buildString {
            appendLine("queued=${state.downloads.queuedAppIds.joinToString(",")}")
            appendLine("paused=${state.downloads.pausedAppIds.joinToString(",")}")
        }
        taskArea.text = state.tasks.joinToString("\n") { task ->
            "${task.id} | ${task.type} | payload=${task.payload} | ${task.status} | ${task.updatedAt}"
        }

        sessionMonitorArea.text = state.sessionMonitor.timelineSummary
        settingsArea.text = buildString {
            appendLine("profilesPath=${System.getProperty("user.home")}/.config/gamenative/profiles.json")
            appendLine("settingsPath=${System.getProperty("user.home")}/.config/gamenative/desktop-settings.properties")
            appendLine("taskStorePath=${System.getProperty("user.home")}/.local/state/gamenative/desktop-tasks.properties")
            appendLine("runtimeStatePath=build/runtime-prototype-state")
            appendLine("hardeningScript=./tools/linux_phase6_hardening_check.sh")
        }
        downloadRootField.text = state.settings.downloadRoot
        pollingField.text = state.settings.diagnosticsPollingSeconds.toString()
    }

    private fun saveProfile() {
        val selectedBackend = backendBox.selectedItem as RuntimeBackend
        runCatching {
            controller.saveProfile(
                id = idField.text,
                name = nameField.text,
                wineBinary = wineField.text,
                backend = selectedBackend,
            )
        }.onSuccess {
            refreshState()
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                frame,
                error.message ?: "Unknown error while saving profile",
                "Save Profile Failed",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }

    private fun login() {
        val session = controller.login(usernameField.text, passwordField.text)
        sessionArea.text = "state=${session.state}\naccount=${session.accountName ?: ""}\nmessage=${session.message ?: ""}"
        refreshState()
    }

    private fun logout() {
        controller.logout()
        refreshState()
    }

    private fun refreshLibrary() {
        controller.refreshLibrary()
        refreshState()
    }

    private fun openGameDetails() {
        val appId = detailAppIdField.text.toIntOrNull()
        if (appId == null) {
            JOptionPane.showMessageDialog(frame, "Invalid app id", "Open Game Detail Failed", JOptionPane.ERROR_MESSAGE)
            return
        }
        controller.selectGame(appId)
        refreshState()
    }

    private fun mutateQueue(action: String) {
        val appId = appIdField.text.toIntOrNull()
        if (appId == null) {
            JOptionPane.showMessageDialog(frame, "Invalid app id", "Queue Update Failed", JOptionPane.ERROR_MESSAGE)
            return
        }

        when (action) {
            "enqueue" -> controller.enqueueDownload(appId)
            "pause" -> controller.pauseDownload(appId)
            "cancel" -> controller.cancelDownload(appId)
        }
        refreshState()
    }

    private fun saveSettings() {
        val poll = pollingField.text.toIntOrNull()
        if (poll == null) {
            JOptionPane.showMessageDialog(frame, "Invalid polling seconds", "Save Settings Failed", JOptionPane.ERROR_MESSAGE)
            return
        }

        runCatching {
            controller.saveSettings(
                downloadRoot = downloadRootField.text,
                diagnosticsPollingSeconds = poll,
            )
        }.onSuccess {
            refreshState()
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                frame,
                error.message ?: "Unknown error while saving settings",
                "Save Settings Failed",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }
}
