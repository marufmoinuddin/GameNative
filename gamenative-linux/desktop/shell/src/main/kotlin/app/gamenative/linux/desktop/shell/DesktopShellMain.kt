package app.gamenative.linux.desktop.shell

import app.gamenative.linux.runtime.RuntimeBackend
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
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
    private val diagnosticsArea = JTextArea(8, 80)
    private val profileModel = DefaultListModel<String>()
    private val profileList = JList(profileModel)

    private val idField = JTextField("default", 20)
    private val nameField = JTextField("Default", 20)
    private val wineField = JTextField("wine", 20)
    private val backendBox = JComboBox(RuntimeBackend.entries.toTypedArray())

    private val sessionArea = JTextArea(5, 80)
    private val usernameField = JTextField("", 20)
    private val passwordField = JTextField("", 20)

    private val libraryArea = JTextArea(10, 80)
    private val gameDetailArea = JTextArea(7, 80)
    private val detailAppIdField = JTextField("620", 8)
    private val appIdField = JTextField("620", 8)
    private val queueArea = JTextArea(6, 80)
    private val taskArea = JTextArea(8, 80)

    private val sessionMonitorArea = JTextArea(8, 80)
    private val settingsArea = JTextArea(6, 80)
    private val downloadRootField = JTextField("~/.local/share/gamenative/games", 40)
    private val pollingField = JTextField("10", 6)

    init {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(980, 680)
        frame.setLocationRelativeTo(null)

        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.border = BorderFactory.createEmptyBorder(14, 14, 14, 14)

        val tabs = JTabbedPane()

        val refreshButton = JButton("Refresh Diagnostics + Profiles")
        refreshButton.addActionListener { refreshState() }

        diagnosticsArea.isEditable = false
        val diagnosticsPane = JScrollPane(diagnosticsArea)
        diagnosticsPane.border = BorderFactory.createTitledBorder("First-Run Diagnostics")
        val diagnosticsPanel = JPanel()
        diagnosticsPanel.layout = BoxLayout(diagnosticsPanel, BoxLayout.Y_AXIS)
        diagnosticsPanel.add(refreshButton)
        diagnosticsPanel.add(diagnosticsPane)
        tabs.addTab("Diagnostics", JScrollPane(diagnosticsPanel))

        val profilePane = JScrollPane(profileList)
        profilePane.border = BorderFactory.createTitledBorder("Runtime Profiles")

        val editor = JPanel()
        editor.layout = BoxLayout(editor, BoxLayout.Y_AXIS)
        editor.border = BorderFactory.createTitledBorder("Profile Editor")
        editor.add(JLabel("Profile ID"))
        editor.add(idField)
        editor.add(JLabel("Name"))
        editor.add(nameField)
        editor.add(JLabel("Wine Binary"))
        editor.add(wineField)
        editor.add(JLabel("Backend"))
        editor.add(backendBox)

        val saveButton = JButton("Save Profile")
        saveButton.addActionListener { saveProfile() }
        editor.add(saveButton)

        val profilePanel = JPanel()
        profilePanel.layout = BoxLayout(profilePanel, BoxLayout.Y_AXIS)
        profilePanel.add(profilePane)
        profilePanel.add(editor)
        tabs.addTab("Profiles", JScrollPane(profilePanel))

        val accountPanel = JPanel()
        accountPanel.layout = BoxLayout(accountPanel, BoxLayout.Y_AXIS)
        sessionArea.isEditable = false
        accountPanel.border = BorderFactory.createTitledBorder("Steam Account Sign-In")
        accountPanel.add(JLabel("Username"))
        accountPanel.add(usernameField)
        accountPanel.add(JLabel("Password"))
        accountPanel.add(passwordField)
        val loginButton = JButton("Login")
        loginButton.addActionListener { login() }
        val logoutButton = JButton("Logout")
        logoutButton.addActionListener { logout() }
        accountPanel.add(loginButton)
        accountPanel.add(logoutButton)
        accountPanel.add(JScrollPane(sessionArea))
        tabs.addTab("Account", JScrollPane(accountPanel))

        val libraryPanel = JPanel()
        libraryPanel.layout = BoxLayout(libraryPanel, BoxLayout.Y_AXIS)
        libraryArea.isEditable = false
        val refreshLibraryButton = JButton("Refresh Owned Apps")
        refreshLibraryButton.addActionListener { refreshLibrary() }
        val openDetailsButton = JButton("Open Game Details")
        openDetailsButton.addActionListener { openGameDetails() }
        libraryPanel.add(refreshLibraryButton)
        libraryPanel.add(JLabel("Game Detail App ID"))
        libraryPanel.add(detailAppIdField)
        libraryPanel.add(openDetailsButton)
        libraryPanel.add(JScrollPane(libraryArea))
        tabs.addTab("Library", JScrollPane(libraryPanel))

        val gameDetailPanel = JPanel()
        gameDetailPanel.layout = BoxLayout(gameDetailPanel, BoxLayout.Y_AXIS)
        gameDetailArea.isEditable = false
        gameDetailPanel.add(JLabel("Game Detail"))
        gameDetailPanel.add(JScrollPane(gameDetailArea))
        tabs.addTab("Game Detail", JScrollPane(gameDetailPanel))

        val downloadsPanel = JPanel()
        downloadsPanel.layout = BoxLayout(downloadsPanel, BoxLayout.Y_AXIS)
        queueArea.isEditable = false
        downloadsPanel.add(JLabel("App ID"))
        downloadsPanel.add(appIdField)
        val enqueueButton = JButton("Enqueue")
        enqueueButton.addActionListener { mutateQueue("enqueue") }
        val pauseButton = JButton("Pause")
        pauseButton.addActionListener { mutateQueue("pause") }
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener { mutateQueue("cancel") }
        downloadsPanel.add(enqueueButton)
        downloadsPanel.add(pauseButton)
        downloadsPanel.add(cancelButton)
        downloadsPanel.add(JScrollPane(queueArea))
        tabs.addTab("Downloads", JScrollPane(downloadsPanel))

        val tasksPanel = JPanel()
        tasksPanel.layout = BoxLayout(tasksPanel, BoxLayout.Y_AXIS)
        taskArea.isEditable = false
        val refreshTasksButton = JButton("Refresh Tasks")
        refreshTasksButton.addActionListener { refreshState() }
        tasksPanel.add(refreshTasksButton)
        tasksPanel.add(JScrollPane(taskArea))
        tabs.addTab("Background Tasks", JScrollPane(tasksPanel))

        val sessionMonitorPanel = JPanel()
        sessionMonitorPanel.layout = BoxLayout(sessionMonitorPanel, BoxLayout.Y_AXIS)
        sessionMonitorArea.isEditable = false
        sessionMonitorPanel.add(JLabel("Latest Runtime Session Timeline"))
        sessionMonitorPanel.add(JScrollPane(sessionMonitorArea))
        tabs.addTab("Session Monitor", JScrollPane(sessionMonitorPanel))

        val settingsPanel = JPanel()
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        settingsArea.isEditable = false
        settingsPanel.add(JLabel("Settings / Runtime Paths"))
        settingsPanel.add(JLabel("Download Root"))
        settingsPanel.add(downloadRootField)
        settingsPanel.add(JLabel("Diagnostics Polling Seconds"))
        settingsPanel.add(pollingField)
        val saveSettingsButton = JButton("Save Settings")
        saveSettingsButton.addActionListener { saveSettings() }
        settingsPanel.add(saveSettingsButton)
        settingsPanel.add(JScrollPane(settingsArea))
        tabs.addTab("Settings", JScrollPane(settingsPanel))

        root.add(tabs)

        frame.contentPane = JScrollPane(root)
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
