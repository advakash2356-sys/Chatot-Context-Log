package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.CalendarNotificationScheduler
import com.example.ui.components.BottomNav
import com.example.ui.components.LogEntryModal
import com.example.ui.components.RagSearchModal
import com.example.ui.components.TopHeader
import com.example.ui.screens.AskNeoTab
import com.example.ui.screens.BillingTab
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.FlowStudioTab
import com.example.ui.screens.LogsTab
import com.example.ui.screens.MemoriesTab
import com.example.ui.screens.SettingsTab
import com.example.ui.screens.TasksTab
import com.example.ui.theme.ContextLogTheme
import com.example.ui.theme.PurpleOnPrimary
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.ContextLogViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: ContextLogViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleNotificationIntent(intent)
    setContent {
      ContextLogApp(viewModel = viewModel)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    if (intent == null) return
    val isLogAction = intent.action == CalendarNotificationScheduler.ACTION_OPEN_LOG_ENTRY ||
        intent.getBooleanExtra(CalendarNotificationScheduler.ACTION_OPEN_LOG_ENTRY, false)

    if (isLogAction) {
      val eventTitle = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_EVENT_TITLE) ?: ""
      val matterCode = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_MATTER_CODE) ?: ""
      val autoRecord = intent.getBooleanExtra(CalendarNotificationScheduler.EXTRA_AUTO_START_RECORDING, false)
      viewModel.openLogModal(
        prefillMatterCode = matterCode,
        prefillEventTitle = eventTitle,
        autoStartRecording = autoRecord
      )
    }
  }
}

@Composable
fun ContextLogApp(viewModel: ContextLogViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Request runtime notifications permission for Android 13+ (API 33+)
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ -> }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  ContextLogTheme(themeMode = uiState.themeMode) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopHeader(
          onQuickCaptureClick = { viewModel.openLogModal() }
        )
      },
      bottomBar = {
        BottomNav(
          selectedIndex = uiState.activeTab,
          onTabSelected = { viewModel.selectTab(it) }
        )
      },
      floatingActionButton = {
        FloatingActionButton(
          onClick = { viewModel.openLogModal() },
          containerColor = PurplePrimary,
          contentColor = PurpleOnPrimary,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .padding(bottom = 8.dp, end = 8.dp)
            .testTag("add_context_note_fab")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Log Context Note",
            modifier = Modifier.size(28.dp)
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        AnimatedContent(
          targetState = uiState.activeTab,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "tab_transition"
        ) { tabIndex ->
          when (tabIndex) {
            0 -> MemoriesTab(
              notes = uiState.notes,
              searchQuery = uiState.searchQuery,
              onSearchQueryChange = { viewModel.updateSearchQuery(it) },
              availableTags = uiState.availableTags,
              selectedTagFilter = uiState.selectedTagFilter,
              onTagFilterSelect = { viewModel.selectTagFilter(it) },
              onDeleteNote = { viewModel.deleteNote(it) },
              onSpeakText = { viewModel.speakText(it) },
              isSpeaking = uiState.isSpeaking,
              onQuickCaptureClick = { viewModel.openLogModal() },
              pendantConnected = uiState.pendantConnected,
              batterySoC = uiState.batterySoC
            )

            1 -> TasksTab(
              actionItems = uiState.actionItems,
              onToggleTask = { id, done -> viewModel.toggleTask(id, done) },
              onAddTask = { item -> viewModel.addTask(item.title, item.owner, item.isAssignedToYou, item.dueDateFormatted, item.priority, item.externalSyncTarget ?: "ClickUp", item.memoryTitle ?: "") },
              onUpdateTask = { item -> viewModel.updateTask(item) },
              onDeleteTask = { id -> viewModel.deleteTask(id) },
              onUpdateSyncStatus = { id, status, target -> viewModel.updateSyncStatus(id, status, target) }
            )

            2 -> AskNeoTab(
              notes = uiState.notes,
              dossiers = uiState.briefingDossiers,
              ragQuery = uiState.ragQuery,
              onRagQueryChange = { viewModel.setRagQuery(it) },
              ragAnswer = uiState.ragAnswer,
              isGeneratingRag = uiState.isRagLoading,
              onAskRag = { viewModel.askRagQuestion(it) },
              onGenerateBriefing = { viewModel.generateBriefingDossier(it) },
              onDeleteBriefing = { viewModel.deleteBriefingDossier(it) },
              onGeneratePromptExport = { target, task -> viewModel.generatePromptExport(target, task) },
              onSpeakAnswer = { viewModel.speakText(it) },
              isSpeaking = uiState.isSpeaking
            )

            3 -> SettingsTab(
              googleCalendarSyncEnabled = uiState.googleCalendarSyncEnabled,
              onCalendarSyncToggle = { viewModel.toggleGoogleCalendarSync(it) },
              googleCalendarStatus = uiState.googleCalendarStatus,
              autoSyncAndReindexEnabled = uiState.autoSyncAndReindexEnabled,
              onAutoSyncToggle = { viewModel.toggleAutoSyncAndReindex(it) },
              chunkCount = uiState.indexingChunkCount,
              onReindexNow = { viewModel.reindexAllDocuments() },
              currentUser = uiState.currentUser,
              isAuthLoading = uiState.isAuthLoading,
              authErrorMessage = uiState.authErrorMessage,
              onSignInWithGoogle = { viewModel.signInWithGoogle() },
              onSignOut = { viewModel.signOut() },
              themeMode = uiState.themeMode,
              onThemeModeChange = { viewModel.setThemeMode(it) },
              backupFrequency = uiState.backupFrequency,
              onBackupFrequencyChange = { viewModel.setBackupFrequency(it) },
              lastBackupTime = uiState.lastBackupTime,
              lastBackupStatus = uiState.lastBackupStatus,
              lastBackupMetadata = uiState.lastBackupMetadata,
              isBackingUp = uiState.isBackingUp,
              onPerformBackupNow = { viewModel.performBackupNow() },
              onTriggerBackgroundSync = { viewModel.triggerBackgroundWorkerSync() },
              dictionaryItems = uiState.dictionaryItems,
              onAddDictionaryItem = { term, cat, notes -> viewModel.addDictionaryItem(term, cat, notes) },
              onDeleteDictionaryItem = { viewModel.deleteDictionaryItem(it) },
              snippets = uiState.snippets,
              onAddSnippet = { trigger, exp, desc, cat -> viewModel.addSnippet(trigger, exp, desc, cat) },
              onDeleteSnippet = { viewModel.deleteSnippet(it) },
              meetingAlertsEnabled = uiState.meetingAlertsEnabled,
              onMeetingAlertsToggle = { viewModel.toggleMeetingAlerts(it) },
              onTriggerTestAlert = { viewModel.triggerTest5MinNotification() },
              isSpeaking = uiState.isSpeaking,
              onSpeakText = { viewModel.speakText(it) },
              onStopSpeaking = { viewModel.stopSpeaking() },
              speechRate = uiState.speechRate,
              onSetSpeechRate = { viewModel.setSpeechRate(it) },
              fontScale = uiState.fontScale,
              onSetFontScale = { viewModel.setFontScale(it) },
              highContrast = uiState.highContrast,
              onToggleHighContrast = { viewModel.toggleHighContrast(it) },
              dictationLanguage = uiState.dictationLanguage,
              onSelectDictationLanguage = { viewModel.selectDictationLanguage(it) },
              pendantConnected = uiState.pendantConnected,
              onTogglePendantConnection = { viewModel.togglePendantConnection() },
              batterySoC = uiState.batterySoC,
              rssiDbm = uiState.rssiDbm,
              circularBufferSeconds = uiState.circularBufferSeconds,
              voiceStudioEngine = uiState.voiceStudioEngine,
              onVoiceStudioEngineChange = { viewModel.setVoiceStudioEngine(it) },
              selectedASREngine = uiState.selectedASREngine,
              onASREngineChange = { viewModel.setSelectedASREngine(it) },
              voiceprintEnrolled = uiState.voiceprintEnrolled,
              onToggleVoiceprintEnrollment = { viewModel.toggleVoiceprintEnrollment() },
              voiceprintSimilarity = uiState.voiceprintSimilarity
            )
          }
        }

        if (uiState.isLogModalOpen) {
          LogEntryModal(
            onDismiss = { viewModel.closeLogModal() },
            onSubmit = { viewModel.submitLog(it) },
            isLoggingInProgress = uiState.isLoggingInProgress,
            prefillTranscript = uiState.prefillLogText,
            prefillMatterCode = uiState.prefillMatterCode,
            prefillEventTitle = uiState.prefillEventTitle,
            autoStartRecording = uiState.autoStartRecordingInModal,
            googleCalendarSyncEnabled = uiState.googleCalendarSyncEnabled
          )
        }

        if (uiState.isRagModalOpen) {
          RagSearchModal(
            onDismiss = { viewModel.closeRagModal() },
            query = uiState.ragQuery,
            onQueryChange = { viewModel.setRagQuery(it) },
            onSearch = { viewModel.submitRagSearch() },
            answer = uiState.ragAnswer,
            citations = uiState.ragCitations,
            isLoading = uiState.isRagLoading,
            onIngestDocument = { title, content -> viewModel.ingestDocument(title, content) },
            ingestionProgress = uiState.ingestionProgress,
            ingestionStatusText = uiState.ingestionStatusText,
            isIngesting = uiState.isIngesting
          )
        }
      }
    }
  }
}
