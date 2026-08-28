package com.example

import android.Manifest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.EpisodicMemoryScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.ContextLogTheme
import com.example.ui.theme.MonoBackground
import com.example.ui.viewmodel.ContextLogViewModel
import com.example.ui.viewmodel.MainAppView

class MainActivity : ComponentActivity() {
  private val viewModel: ContextLogViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ContextLogApp(viewModel = viewModel)
    }
  }
}

@Composable
fun ContextLogApp(viewModel: ContextLogViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Permission Launcher for Audio & Notifications
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { _ -> }

  LaunchedEffect(Unit) {
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val missing = permissions.filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missing.isNotEmpty()) {
      audioPermissionLauncher.launch(missing.toTypedArray())
    }
  }

  ContextLogTheme(themeMode = uiState.themeMode) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(MonoBackground)
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MonoBackground)
          .padding(innerPadding)
      ) {
        AnimatedContent(
          targetState = uiState.activeView,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "view_transition"
        ) { activeView ->
          when (activeView) {
            MainAppView.CAPTURE -> {
              CaptureScreen(
                dictationState = uiState.dictationState,
                liveTranscript = uiState.liveTranscript,
                rawVerifiedTranscript = uiState.rawVerifiedTranscript,
                recordingDurationSeconds = uiState.recordingDurationSeconds,
                micAmplitude = uiState.micAmplitude,
                toastMessage = uiState.toastMessage,
                vaultItemCount = uiState.notes.size + uiState.actionItems.size + uiState.calendarEvents.size,
                episodicItemCount = uiState.episodicMemories.size,
                inlineDictationTarget = uiState.inlineDictationTarget,
                onStartListening = {
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startListeningSession()
                  } else {
                    audioPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                  }
                },
                onStopListening = { viewModel.stopListeningSession() },
                onDiscard = { viewModel.discardDictation() },
                onUpdateVerifiedText = { viewModel.updateVerifiedTranscript(it) },
                onSaveAndProcess = { viewModel.saveAndProcessDictation(it) },
                onNavigateToVault = { viewModel.setActiveView(MainAppView.VAULT) },
                onNavigateToEpisodic = { viewModel.setActiveView(MainAppView.EPISODIC) },
                onStartInlineDictation = { targetId, currentText, onUpdate ->
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startInlineFieldDictation(targetId, currentText, onUpdate)
                  } else {
                    audioPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                  }
                },
                onStopInlineDictation = { viewModel.stopInlineFieldDictation() }
              )
            }

            MainAppView.VAULT -> {
              VaultScreen(
                notes = uiState.notes,
                actionItems = uiState.actionItems,
                calendarEvents = uiState.calendarEvents,
                rollups = uiState.rollups,
                episodicItemCount = uiState.episodicMemories.size,
                currentFilter = uiState.vaultFilter,
                searchQuery = uiState.vaultSearchQuery,
                inlineDictationTarget = uiState.inlineDictationTarget,
                onFilterSelect = { viewModel.setVaultFilter(it) },
                onSearchQueryChange = { viewModel.setVaultSearchQuery(it) },
                onToggleTask = { id, done -> viewModel.toggleTask(id, done) },
                onDeleteNote = { viewModel.deleteNote(it) },
                onDeleteTask = { viewModel.deleteTask(it) },
                onDeleteCalendarEvent = { viewModel.deleteCalendarEvent(it) },
                onGenerateReport = { viewModel.generateExecutiveReportForCurrentBlock() },
                onNavigateToCapture = { viewModel.setActiveView(MainAppView.CAPTURE) },
                onNavigateToEpisodic = { viewModel.setActiveView(MainAppView.EPISODIC) },
                onStartInlineDictation = { targetId, currentText, onUpdate ->
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startInlineFieldDictation(targetId, currentText, onUpdate)
                  } else {
                    audioPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                  }
                },
                onStopInlineDictation = { viewModel.stopInlineFieldDictation() }
              )
            }

            MainAppView.EPISODIC -> {
              EpisodicMemoryScreen(
                memories = uiState.episodicMemories,
                vaultItemCount = uiState.notes.size + uiState.actionItems.size + uiState.calendarEvents.size,
                isIngesting = uiState.isIngestingEpisodic,
                ingestStage = uiState.episodicIngestStage,
                searchQuery = uiState.episodicSearchQuery,
                selectedLifeStageFilter = uiState.selectedLifeStageFilter,
                selectedValenceFilter = uiState.selectedValenceFilter,
                selectedMemory = uiState.selectedEpisodicMemory,
                probingGapQuestion = uiState.probingGapQuestion,
                probeAnswerText = uiState.probeAnswerText,
                isProbingLoading = uiState.isProbingLoading,
                inputText = uiState.episodicInputText,
                imageDesc = uiState.episodicImageDesc,
                inlineDictationTarget = uiState.inlineDictationTarget,
                isGuideSessionActive = uiState.isGuideSessionActive,
                guideMessages = uiState.guideConversationMessages,
                isGuideThinking = uiState.isGuideThinking,
                isGuideSpeaking = uiState.isGuideSpeaking,
                isGuideTtsEnabled = uiState.isGuideTtsEnabled,
                guideInputText = uiState.guideInputText,
                activeExploringMemory = uiState.activeExploringMemory,
                isLiveMultimodalActive = uiState.isLiveMultimodalActive,
                selectedLiveVoice = uiState.selectedLiveVoice,
                liveConnectionStatus = uiState.liveConnectionStatus,
                liveMetrics = uiState.liveMetrics,
                liveMicAmplitude = uiState.liveMicAmplitude,
                liveSpeakerAmplitude = uiState.liveSpeakerAmplitude,
                onToggleLiveMode = { viewModel.toggleMultimodalLiveMode() },
                onSelectLiveVoice = { viewModel.setSelectedLiveVoice(it) },
                onTriggerLiveBargeIn = { viewModel.triggerLiveBargeIn() },
                onStartGuideSession = { viewModel.startMemoryGuideSession(it) },
                onSendGuideMessage = { text, cue -> viewModel.sendMemoryGuideMessage(text, cue) },
                onTriggerSensoryAnchor = { viewModel.triggerSensoryAnchorPrompt(it) },
                onToggleGuideTts = { viewModel.toggleGuideTts() },
                onStopGuideSpeaking = { viewModel.stopGuideSpeaking() },
                onGuideInputChange = { viewModel.setGuideInputText(it) },
                onEndGuideSession = { viewModel.endMemoryGuideSession() },
                onSaveGuideExploration = { viewModel.saveGuideExplorationAsEnrichedMemory() },
                onNavigateToCapture = { viewModel.setActiveView(MainAppView.CAPTURE) },
                onNavigateToVault = { viewModel.setActiveView(MainAppView.VAULT) },
                onSearchQueryChange = { viewModel.setEpisodicSearchQuery(it) },
                onSelectLifeStageFilter = { viewModel.setSelectedLifeStageFilter(it) },
                onSelectValenceFilter = { viewModel.setSelectedValenceFilter(it) },
                onInputTextChange = { viewModel.setEpisodicInputText(it) },
                onImageDescChange = { viewModel.setEpisodicImageDesc(it) },
                onIngestMemory = { text, image -> viewModel.ingestEpisodicMemory(text, image) },
                onSelectMemory = { viewModel.selectEpisodicMemory(it) },
                onStartProbingGap = { viewModel.startProbingGap(it) },
                onProbeAnswerChange = { viewModel.setProbeAnswerText(it) },
                onSubmitProbeAnswer = { memId, question, ans ->
                  viewModel.submitProbeAnswer(memId, question, ans)
                },
                onDeleteMemory = { viewModel.deleteEpisodicMemory(it) },
                onStartInlineDictation = { targetId, currentText, onUpdate ->
                  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startInlineFieldDictation(targetId, currentText, onUpdate)
                  } else {
                    audioPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                  }
                },
                onStopInlineDictation = { viewModel.stopInlineFieldDictation() }
              )
            }
          }
        }
      }
    }
  }
}
