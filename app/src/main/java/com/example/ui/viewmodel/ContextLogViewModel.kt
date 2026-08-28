package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiService
import com.example.data.ai.MemoryGuideMessage
import com.example.data.ai.GuideSender
import com.example.data.ai.StructuredMeetingNotes
import com.example.data.ai.VoiceContextType
import com.example.data.ai.VoiceFlowEngine
import com.example.data.ai.VoiceFlowResult
import com.example.data.ai.VoiceTone
import com.example.data.ai.VoiceTransform
import com.example.data.ai.WisprContextType
import com.example.data.ai.WisprFlowEngine
import com.example.data.ai.WisprFlowResult
import com.example.data.ai.WisprTone
import com.example.data.ai.WisprTransform
import com.example.data.ai.GeminiMultimodalLiveClient
import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveMetrics
import com.example.data.ai.ToolCallInfo
import com.example.data.audio.LiveAudioStreamManager
import com.example.data.audio.MediaRecorderManager
import com.example.data.audio.SpeechDictationManager
import com.example.data.audio.TextToSpeechHelper
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthState
import com.example.data.calendar.DeviceCalendarManager
import com.example.data.local.ActionItemEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BriefingDossierEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextNoteEntity
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.DocumentEntity
import com.example.data.local.EntryType
import com.example.data.local.GroundedCitation
import com.example.data.local.MatterEntity
import com.example.data.local.SnippetEntity
import com.example.data.local.TokenUsageEntity
import com.example.data.local.TwoHourRollupEntity
import com.example.data.repository.ContextLogRepository
import com.example.data.sync.BackupFrequency
import com.example.data.sync.BackupMetadata
import com.example.data.sync.CloudBackupManager
import com.example.data.sync.GoogleCalendarSyncWorker
import com.example.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar

enum class DictationState {
  IDLE,
  LISTENING,
  VERIFICATION,
  PROCESSING,
  CONFIRMATION
}

enum class MainAppView {
  CAPTURE,
  VAULT,
  EPISODIC
}

enum class VaultFilter {
  ALL,
  NOTES,
  TASKS,
  CALENDAR,
  REPORTS
}

data class RoutingToast(
  val id: Long = System.currentTimeMillis(),
  val message: String,
  val noteTitle: String = "",
  val tasksCount: Int = 0,
  val eventTitle: String? = null
)

data class ContextLogUiState(
  // Primary 3-view navigation & state machine
  val activeView: MainAppView = MainAppView.CAPTURE,
  val dictationState: DictationState = DictationState.IDLE,
  val liveTranscript: String = "",
  val rawVerifiedTranscript: String = "",
  val recordingDurationSeconds: Int = 0,
  val micAmplitude: Float = 0.1f,
  val dictationWaveformLevels: FloatArray = floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
  val toastMessage: RoutingToast? = null,
  val isProcessingIntelligence: Boolean = false,
  val inlineDictationTarget: String? = null,
  val vaultFilter: VaultFilter = VaultFilter.ALL,
  val vaultSearchQuery: String = "",

  // Episodic Memory Ingestion Engine State
  val episodicMemories: List<com.example.data.local.EpisodicMemoryEntity> = emptyList(),
  val isIngestingEpisodic: Boolean = false,
  val episodicIngestStage: String = "",
  val episodicSearchQuery: String = "",
  val selectedLifeStageFilter: String? = null,
  val selectedValenceFilter: String? = null,
  val selectedEpisodicMemory: com.example.data.local.EpisodicMemoryEntity? = null,
  val probingGapQuestion: String? = null,
  val probeAnswerText: String = "",
  val isProbingLoading: Boolean = false,
  val episodicInputText: String = "",
  val episodicImageDesc: String = "",

  // Interactive Memory Exploration Guide State
  val isGuideSessionActive: Boolean = false,
  val guideConversationMessages: List<MemoryGuideMessage> = emptyList(),
  val isGuideThinking: Boolean = false,
  val isGuideSpeaking: Boolean = false,
  val isGuideTtsEnabled: Boolean = true,
  val guideInputText: String = "",
  val activeExploringMemory: com.example.data.local.EpisodicMemoryEntity? = null,

  // Phase 4: Gemini Multimodal Live API UI State
  val isLiveMultimodalActive: Boolean = false,
  val selectedLiveVoice: String = "Aoede",
  val liveConnectionStatus: LiveConnectionStatus = LiveConnectionStatus.DISCONNECTED,
  val liveMetrics: LiveMetrics = LiveMetrics(),
  val liveStatusMessage: String = "Ready to connect",
  val liveMicAmplitude: Float = 0f,
  val liveSpeakerAmplitude: Float = 0f,
  val isLiveBargeInActive: Boolean = false,
  val isLiveRecordingMic: Boolean = false,
  val isLivePlayingSpeaker: Boolean = false,

  val activeTab: Int = 0,
  val todayHours: Double = 0.0,
  val targetHours: Double = 8.0,
  val isSynced: Boolean = true,
  val notes: List<ContextNoteEntity> = emptyList(),
  val matters: List<MatterEntity> = emptyList(),
  val billingSummaries: List<MatterBillingSummary> = emptyList(),
  val rollups: List<TwoHourRollupEntity> = emptyList(),
  val calendarEvents: List<com.example.data.local.CalendarEventEntity> = emptyList(),
  val tokenMetrics: List<TokenUsageEntity> = emptyList(),
  val totalTokenSpend: Int = 0,
  val totalEstimatedCostUsd: Double = 0.0,
  val documents: List<DocumentEntity> = emptyList(),
  val indexingChunkCount: Int = 0,
  val isIndexing: Boolean = false,
  val searchQuery: String = "",
  val selectedTypeFilter: EntryType? = null,
  val selectedTagFilter: String? = null,
  val availableTags: List<String> = emptyList(),
  val isLogModalOpen: Boolean = false,
  val prefillLogText: String = "",
  val prefillMatterCode: String = "",
  val prefillEventTitle: String = "",
  val autoStartRecordingInModal: Boolean = false,
  val meetingAlertsEnabled: Boolean = true,
  val isRagModalOpen: Boolean = false,
  val ragQuery: String = "",
  val ragAnswer: String? = null,
  val ragCitations: List<GroundedCitation> = emptyList(),
  val isRagLoading: Boolean = false,
  val isLoggingInProgress: Boolean = false,
  val googleCalendarSyncEnabled: Boolean = true,
  val googleCalendarStatus: String = "Connected (calendar.events scope)",
  val autoSyncAndReindexEnabled: Boolean = true,
  val ingestionProgress: Float = 0.0f,
  val ingestionStatusText: String = "",
  val isIngesting: Boolean = false,
  val currentUser: FirebaseUser? = null,
  val isAuthLoading: Boolean = false,
  val authErrorMessage: String? = null,
  val lastCalendarSyncEventId: String? = null,

  // Theme & Cloud Backup State
  val themeMode: ThemeMode = ThemeMode.DARK,
  val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
  val lastBackupTime: Long = 0L,
  val lastBackupStatus: String = "Ready to backup",
  val lastBackupMetadata: BackupMetadata? = null,
  val isBackingUp: Boolean = false,

  // Wispr Flow Voice & Extended Context State
  val wisprInput: String = "",
  val wisprContext: WisprContextType = WisprContextType.GENERAL,
  val wisprTone: WisprTone = WisprTone.AUTO_CLEAN,
  val wisprLanguage: String = "English",
  val wisprResult: WisprFlowResult? = null,
  val isWisprProcessing: Boolean = false,
  val activeTransform: WisprTransform? = null,
  val transformResult: String? = null,
  val isTransformLoading: Boolean = false,
  val isRecordingAudio: Boolean = false,
  val audioRecordingSeconds: Int = 0,
  val micErrorMessage: String? = null,
  val isExtendedContextMode: Boolean = false,
  val dictionaryItems: List<DictionaryItemEntity> = emptyList(),
  val snippets: List<SnippetEntity> = emptyList(),

  // Data Accessibility & Audio Readback State
  val isSpeaking: Boolean = false,
  val speechRate: Float = 1.0f,
  val dictationAnnouncement: String? = null,
  val dictationLanguage: String = "en-US",
  val fontScale: Float = 1.0f,
  val highContrastMode: Boolean = false,

  // Spatial Hardware & VoiceStudio Engine State
  val actionItems: List<ActionItemEntity> = emptyList(),
  val briefingDossiers: List<BriefingDossierEntity> = emptyList(),
  val pendantConnected: Boolean = true,
  val batterySoC: Int = 88,
  val rssiDbm: Int = -62,
  val circularBufferSeconds: Int = 300,
  val voiceStudioEngine: String = "Piper Neural (Local VITS)",
  val selectedASREngine: String = "Whisper Fast (Local GGML)",
  val voiceprintEnrolled: Boolean = true,
  val voiceprintSimilarity: Float = 0.94f
) {
  val highContrast: Boolean get() = highContrastMode
}

data class MatterBillingSummary(
  val matterCode: String,
  val matterName: String,
  val clientName: String,
  val loggedHours: Double,
  val entriesCount: Int,
  val lastActivity: Long
)

class ContextLogViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ContextLogRepository
  val authManager: AuthManager = AuthManager(application)
  val speechDictationManager: SpeechDictationManager = SpeechDictationManager(application)
  val textToSpeechHelper: TextToSpeechHelper = TextToSpeechHelper(application)
  val mediaRecorderManager: MediaRecorderManager = MediaRecorderManager(application)
  val cloudBackupManager: CloudBackupManager = CloudBackupManager(application)
  val liveAudioStreamManager: LiveAudioStreamManager = LiveAudioStreamManager(application)
  val geminiLiveClient: GeminiMultimodalLiveClient = GeminiMultimodalLiveClient()
  private val wisprEngine: WisprFlowEngine = WisprFlowEngine()

  private val appPrefs = application.getSharedPreferences("context_log_app_prefs", Context.MODE_PRIVATE)

  // Two-View Architecture & Dictation State Machine
  private val _activeView = MutableStateFlow(MainAppView.CAPTURE)
  val activeView: StateFlow<MainAppView> = _activeView.asStateFlow()

  private val _dictationState = MutableStateFlow(DictationState.IDLE)
  val dictationState: StateFlow<DictationState> = _dictationState.asStateFlow()

  private val _liveTranscript = MutableStateFlow("")
  val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

  private val _rawVerifiedTranscript = MutableStateFlow("")
  val rawVerifiedTranscript: StateFlow<String> = _rawVerifiedTranscript.asStateFlow()

  private val _recordingDurationSeconds = MutableStateFlow(0)
  val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

  private val _toastMessage = MutableStateFlow<RoutingToast?>(null)
  val toastMessage: StateFlow<RoutingToast?> = _toastMessage.asStateFlow()

  private val _isProcessingIntelligence = MutableStateFlow(false)
  val isProcessingIntelligence: StateFlow<Boolean> = _isProcessingIntelligence.asStateFlow()

  private val _inlineDictationTarget = MutableStateFlow<String?>(null)
  val inlineDictationTarget: StateFlow<String?> = _inlineDictationTarget.asStateFlow()
  private var inlineCallback: ((String) -> Unit)? = null

  private val _vaultFilter = MutableStateFlow(VaultFilter.ALL)
  val vaultFilter: StateFlow<VaultFilter> = _vaultFilter.asStateFlow()

  private val _vaultSearchQuery = MutableStateFlow("")
  val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

  // Episodic Memory Ingestion State
  private val _isIngestingEpisodic = MutableStateFlow(false)
  val isIngestingEpisodic: StateFlow<Boolean> = _isIngestingEpisodic.asStateFlow()

  private val _episodicIngestStage = MutableStateFlow("")
  val episodicIngestStage: StateFlow<String> = _episodicIngestStage.asStateFlow()

  private val _episodicSearchQuery = MutableStateFlow("")
  val episodicSearchQuery: StateFlow<String> = _episodicSearchQuery.asStateFlow()

  private val _selectedLifeStageFilter = MutableStateFlow<String?>(null)
  val selectedLifeStageFilter: StateFlow<String?> = _selectedLifeStageFilter.asStateFlow()

  private val _selectedValenceFilter = MutableStateFlow<String?>(null)
  val selectedValenceFilter: StateFlow<String?> = _selectedValenceFilter.asStateFlow()

  private val _selectedEpisodicMemory = MutableStateFlow<com.example.data.local.EpisodicMemoryEntity?>(null)
  val selectedEpisodicMemory: StateFlow<com.example.data.local.EpisodicMemoryEntity?> = _selectedEpisodicMemory.asStateFlow()

  private val _probingGapQuestion = MutableStateFlow<String?>(null)
  val probingGapQuestion: StateFlow<String?> = _probingGapQuestion.asStateFlow()

  private val _probeAnswerText = MutableStateFlow("")
  val probeAnswerText: StateFlow<String> = _probeAnswerText.asStateFlow()

  private val _isProbingLoading = MutableStateFlow(false)
  val isProbingLoading: StateFlow<Boolean> = _isProbingLoading.asStateFlow()

  private val _episodicInputText = MutableStateFlow("")
  val episodicInputText: StateFlow<String> = _episodicInputText.asStateFlow()

  private val _episodicImageDesc = MutableStateFlow("")
  val episodicImageDesc: StateFlow<String> = _episodicImageDesc.asStateFlow()

  // Interactive Memory Exploration Guide state flows
  private val _isGuideSessionActive = MutableStateFlow(false)
  val isGuideSessionActive: StateFlow<Boolean> = _isGuideSessionActive.asStateFlow()

  private val _guideConversationMessages = MutableStateFlow<List<MemoryGuideMessage>>(emptyList())
  val guideConversationMessages: StateFlow<List<MemoryGuideMessage>> = _guideConversationMessages.asStateFlow()

  private val _isGuideThinking = MutableStateFlow(false)
  val isGuideThinking: StateFlow<Boolean> = _isGuideThinking.asStateFlow()

  private val _isGuideTtsEnabled = MutableStateFlow(true)
  val isGuideTtsEnabled: StateFlow<Boolean> = _isGuideTtsEnabled.asStateFlow()

  private val _guideInputText = MutableStateFlow("")
  val guideInputText: StateFlow<String> = _guideInputText.asStateFlow()

  private val _activeExploringMemory = MutableStateFlow<com.example.data.local.EpisodicMemoryEntity?>(null)
  val activeExploringMemory: StateFlow<com.example.data.local.EpisodicMemoryEntity?> = _activeExploringMemory.asStateFlow()

  // Phase 4: Gemini Multimodal Live API State Flows
  private val _isLiveMultimodalActive = MutableStateFlow(false)
  val isLiveMultimodalActive: StateFlow<Boolean> = _isLiveMultimodalActive.asStateFlow()

  private val _selectedLiveVoice = MutableStateFlow("Aoede")
  val selectedLiveVoice: StateFlow<String> = _selectedLiveVoice.asStateFlow()

  val liveConnectionStatus: StateFlow<LiveConnectionStatus> = geminiLiveClient.connectionStatus
  val liveMetrics: StateFlow<LiveMetrics> = geminiLiveClient.liveMetrics
  val liveStatusMessage: StateFlow<String> = geminiLiveClient.statusMessage
  val liveMicAmplitude: StateFlow<Float> = liveAudioStreamManager.micAmplitude
  val liveSpeakerAmplitude: StateFlow<Float> = liveAudioStreamManager.speakerAmplitude
  val isLivePlayingSpeaker: StateFlow<Boolean> = liveAudioStreamManager.isPlaying
  val isLiveRecordingMic: StateFlow<Boolean> = liveAudioStreamManager.isRecording
  val isLiveBargeInActive: StateFlow<Boolean> = liveAudioStreamManager.isBargeInActive
  val liveTranscripts = geminiLiveClient.liveTranscripts

  private val _themeMode = MutableStateFlow(
    try {
      ThemeMode.valueOf(appPrefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
    } catch (e: Exception) {
      ThemeMode.DARK
    }
  )
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  private val _fontScale = MutableStateFlow(appPrefs.getFloat("font_scale", 1.0f))
  private val _highContrast = MutableStateFlow(appPrefs.getBoolean("high_contrast", false))

  private val _activeTab = MutableStateFlow(0)
  private val _searchQuery = MutableStateFlow("")
  private val _selectedTypeFilter = MutableStateFlow<EntryType?>(null)
  private val _selectedTagFilter = MutableStateFlow<String?>(null)
  private val _isLogModalOpen = MutableStateFlow(false)
  private val _prefillTranscript = MutableStateFlow("")
  private val _prefillMatterCode = MutableStateFlow("")
  private val _prefillEventTitle = MutableStateFlow("")
  private val _autoStartRecordingInModal = MutableStateFlow(false)
  private val _meetingAlertsEnabled = MutableStateFlow(
    appPrefs.getBoolean("meeting_alerts_enabled", true)
  )
  private val _isRagModalOpen = MutableStateFlow(false)
  private val _ragQuery = MutableStateFlow("")
  private val _ragAnswer = MutableStateFlow<String?>(null)
  private val _ragCitations = MutableStateFlow<List<GroundedCitation>>(emptyList())
  private val _isRagLoading = MutableStateFlow(false)
  private val _isLoggingInProgress = MutableStateFlow(false)
  private val _googleCalendarSyncEnabled = MutableStateFlow(true)
  private val _autoSyncAndReindexEnabled = MutableStateFlow(true)
  private val _ingestionProgress = MutableStateFlow(0.0f)
  private val _ingestionStatusText = MutableStateFlow("")
  private val _isIngesting = MutableStateFlow(false)
  private val _isAuthLoading = MutableStateFlow(false)
  private val _authErrorMessage = MutableStateFlow<String?>(null)
  private val _lastCalendarSyncEventId = MutableStateFlow<String?>(null)

  // Wispr Flow state flows
  private val _wisprInput = MutableStateFlow("")
  private val _wisprContext = MutableStateFlow(WisprContextType.GENERAL)
  private val _wisprTone = MutableStateFlow(WisprTone.AUTO_CLEAN)
  private val _wisprLanguage = MutableStateFlow("English")
  private val _wisprResult = MutableStateFlow<WisprFlowResult?>(null)
  private val _isWisprProcessing = MutableStateFlow(false)
  private val _activeTransform = MutableStateFlow<WisprTransform?>(null)
  private val _transformResult = MutableStateFlow<String?>(null)
  private val _isTransformLoading = MutableStateFlow(false)
  private val _isRecordingAudio = MutableStateFlow(false)
  private val _audioRecordingSeconds = MutableStateFlow(0)
  private val _isExtendedContextMode = MutableStateFlow(false)
  private var recordingJob: Job? = null

  val deviceCalendarManager: DeviceCalendarManager = DeviceCalendarManager(application)

  init {
    val database = AppDatabase.getDatabase(application)
    val geminiService = GeminiService()
    repository = ContextLogRepository(database.contextLogDao(), geminiService)

    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      // Schedule background periodic Google Calendar sync worker
      try {
        GoogleCalendarSyncWorker.schedulePeriodicCalendarSync(application, intervalMinutes = 60)
      } catch (e: Exception) {
        android.util.Log.e("ContextLogViewModel", "Failed to schedule CalendarSyncWorker", e)
      }
    }

    // Schedule 5-minute pre-event alarms when calendar events are loaded or updated
    viewModelScope.launch {
      repository.allCalendarEvents.collect { events ->
        if (_meetingAlertsEnabled.value && events.isNotEmpty()) {
          try {
            com.example.notification.CalendarNotificationScheduler.scheduleAllUpcomingEvents(application, events)
          } catch (e: Exception) {
            android.util.Log.e("ContextLogViewModel", "Failed to schedule calendar alarms", e)
          }
        }
      }
    }

    // Connect speech recognizer partial transcripts
    viewModelScope.launch {
      speechDictationManager.partialTranscript.collect { partial ->
        if (partial.isNotBlank()) {
          if (_inlineDictationTarget.value != null) {
            inlineCallback?.invoke(partial)
          }
          if (_dictationState.value == DictationState.LISTENING) {
            _liveTranscript.value = partial
          }
          if (_isRecordingAudio.value) {
            _wisprInput.value = partial
          }
        }
      }
    }

    // Auto sync snippets & custom dictionary words to live speech dictation manager
    viewModelScope.launch {
      combine(repository.allSnippets, repository.allDictionaryItems) { snippets, dict ->
        val map = mutableMapOf<String, String>()
        snippets.forEach { map[it.triggerPhrase] = it.expandedText }
        dict.forEach { map[it.term] = it.term }
        map
      }.collect { combinedMap ->
        speechDictationManager.setCustomSnippets(combinedMap)
      }
    }

    // Wire Gemini Multimodal Live API dispatcher, audio streaming, and barge-in
    geminiLiveClient.onToolCallDispatcher = { funcName, args ->
      handleLiveToolExecution(funcName, args)
    }

    viewModelScope.launch {
      geminiLiveClient.receivedAudioPcm.collect { pcm24k ->
        liveAudioStreamManager.writeSpeakerPcm(pcm24k)
      }
    }

    viewModelScope.launch {
      geminiLiveClient.interruptionEvents.collect {
        liveAudioStreamManager.flushSpeakerBuffer("Gemini live barge-in interruption")
      }
    }

    viewModelScope.launch {
      geminiLiveClient.toolExecutions.collect { toolInfo ->
        val msg = MemoryGuideMessage(
          sender = GuideSender.GUIDE,
          text = if (toolInfo.toolName == "retrieve_memories") {
            "Consulting archive: ${toolInfo.resultSummary}"
          } else {
            "Updated memory archive: ${toolInfo.resultSummary}"
          },
          executedTools = listOf(toolInfo)
        )
        _guideConversationMessages.value = _guideConversationMessages.value + msg
      }
    }
  }

  // Combined UI Nav & Search State
  private val navSearchFlow: Flow<NavSearchState> = combine(
    combine(_activeTab, _searchQuery, _selectedTypeFilter, _selectedTagFilter) { tab, q, f, tf ->
      NavFilterQuad(tab, q, f, tf)
    },
    combine(_isLogModalOpen, _isRagModalOpen, _themeMode, _prefillTranscript, _prefillMatterCode) { isLogModal, isRagModal, theme, preText, preMatter ->
      Triple(Pair(isLogModal, isRagModal), Pair(theme, preText), preMatter)
    },
    combine(_fontScale, _highContrast) { scale, contrast -> Pair(scale, contrast) }
  ) { quad, sub, acc ->
    val (modals, themeAndText, preMatter) = sub
    NavSearchState(
      activeTab = quad.tab,
      searchQuery = quad.q,
      selectedTypeFilter = quad.f,
      selectedTagFilter = quad.tf,
      isLogModalOpen = modals.first,
      isRagModalOpen = modals.second,
      themeMode = themeAndText.first,
      prefillLogText = themeAndText.second,
      prefillMatterCode = preMatter,
      prefillEventTitle = _prefillEventTitle.value,
      autoStartRecordingInModal = _autoStartRecordingInModal.value,
      meetingAlertsEnabled = _meetingAlertsEnabled.value,
      fontScale = acc.first,
      highContrastMode = acc.second
    )
  }

  // Combined RAG & Ingestion State
  private val ragIngestFlow: Flow<RagIngestState> = combine(
    _ragQuery,
    _ragAnswer,
    _ragCitations,
    _isRagLoading,
    combine(_isLoggingInProgress, _googleCalendarSyncEnabled, _autoSyncAndReindexEnabled, _ingestionProgress, _ingestionStatusText) { logInProg, calSync, autoSync, prog, statusText ->
      Pair(logInProg, Triple(calSync, autoSync, Pair(prog, statusText)))
    }
  ) { query, answer, citations, isRagLoad, rest ->
    val logInProg = rest.first
    val calSync = rest.second.first
    val autoSync = rest.second.second
    val prog = rest.second.third.first
    val statusText = rest.second.third.second
    RagIngestState(query, answer, citations, isRagLoad, logInProg, calSync, autoSync, prog, statusText)
  }

  private val _voiceStudioEngine = MutableStateFlow("Piper Neural (Local VITS)")
  private val _selectedASREngine = MutableStateFlow("Whisper Fast (Local GGML)")
  private val _voiceprintEnrolled = MutableStateFlow(true)
  private val _pendantConnected = MutableStateFlow(true)
  private val _batterySoC = MutableStateFlow(88)

  // Combined DB Flow
  private val dbFlow: Flow<DbState> = combine(
    repository.allNotes,
    repository.allMatters,
    repository.allRollups,
    repository.allDocuments,
    combine(
      repository.chunkCount,
      repository.allCalendarEvents,
      repository.allTokenUsage,
      combine(repository.allActionItems, repository.allBriefingDossiers, repository.allEpisodicMemories) { actions, dossiers, episodicMemories ->
        Triple(actions, dossiers, episodicMemories)
      }
    ) { chunks, calEvents, tokens, (actions, dossiers, episodicMemories) ->
      DbExtras(chunks, calEvents, tokens, actions, dossiers, episodicMemories)
    }
  ) { allNotes, allMatters, allRollups, allDocs, extras ->
    DbState(allNotes, allMatters, allRollups, allDocs, extras.liveChunkCount, extras.calendarEvents, extras.tokenMetrics, extras.actionItems, extras.briefingDossiers, extras.episodicMemories)
  }

  // Combined Auth & Backup Flow
  private val authFlow: Flow<AuthUiState> = combine(
    authManager.authState,
    _isAuthLoading,
    _authErrorMessage,
    _lastCalendarSyncEventId,
    combine(cloudBackupManager.backupFrequency, cloudBackupManager.lastBackupTime, cloudBackupManager.lastBackupStatus, cloudBackupManager.isBackingUp, cloudBackupManager.lastBackupMetadata) { freq, time, status, backingUp, meta ->
      BackupCombinedState(freq, time, status, backingUp, meta)
    }
  ) { authState, isAuthLoading, authError, lastEventId, backupState ->
    AuthUiState(authState, isAuthLoading, authError, lastEventId, backupState)
  }

  // Combined Wispr Flow State
  private val wisprFlow: Flow<WisprCombinedState> = combine(
    combine(_wisprInput, _wisprContext, _wisprTone, _wisprLanguage, _wisprResult) { input, ctx, tone, lang, res ->
      WisprSub1(input, ctx, tone, lang, res)
    },
    combine(_isWisprProcessing, _activeTransform, _transformResult, _isTransformLoading, _isRecordingAudio) { proc, actTrans, resTrans, loadTrans, rec ->
      WisprSub2(proc, actTrans, resTrans, loadTrans, rec)
    },
    combine(
      combine(_audioRecordingSeconds, speechDictationManager.rmsAmplitude, mediaRecorderManager.amplitude, speechDictationManager.errorMessage, _isExtendedContextMode) { sec, sAmp, mAmp, err, ext ->
        RecordingSubState(sec, maxOf(sAmp, mAmp), err, ext)
      },
      combine(speechDictationManager.waveformLevels, speechDictationManager.accessibilityAnnouncement, speechDictationManager.selectedLanguage) { wave, announce, lang ->
        Triple(wave, announce, lang)
      },
      combine(textToSpeechHelper.isSpeaking, textToSpeechHelper.speechRate) { isSpeaking, rate ->
        Pair(isSpeaking, rate)
      }
    ) { recState, speechAcc, ttsState ->
      WisprSub3(
        recSeconds = recState.sec,
        micAmp = recState.micAmp,
        micErr = recState.micErr,
        extendedMode = recState.ext,
        waveformLevels = speechAcc.first,
        announcement = speechAcc.second,
        dictLanguage = speechAcc.third,
        isSpeaking = ttsState.first,
        speechRate = ttsState.second
      )
    },
    combine(repository.allDictionaryItems, repository.allSnippets) { dict, snips ->
      Pair(dict, snips)
    }
  ) { sub1, sub2, sub3, dictSnips ->
    WisprCombinedState(sub1, sub2, sub3, dictSnips.first, dictSnips.second)
  }

  val uiState: StateFlow<ContextLogUiState> = combine(
    navSearchFlow,
    ragIngestFlow,
    dbFlow,
    authFlow,
    wisprFlow
  ) { nav, rag, db, auth, wispr ->

    // Extract all unique tags
    val availableTags = db.allNotes.flatMap { it.tagList }.distinct().sorted()

    val filteredNotes = db.allNotes.filter { note ->
      val matchesSearch = nav.searchQuery.isBlank() ||
        note.cleanText.contains(nav.searchQuery, ignoreCase = true) ||
        note.matterCode.contains(nav.searchQuery, ignoreCase = true) ||
        note.tags.contains(nav.searchQuery, ignoreCase = true)
      val matchesType = nav.selectedTypeFilter == null || note.entryType == nav.selectedTypeFilter
      val matchesTag = nav.selectedTagFilter == null || note.tagList.any { it.equals(nav.selectedTagFilter, ignoreCase = true) }
      matchesSearch && matchesType && matchesTag
    }

    val billingSummaries = db.allMatters.map { matter ->
      val notesForMatter = db.allNotes.filter { it.matterCode == matter.code }
      val distinctBlocksForMatter = notesForMatter.map { it.twoHourBlockStart }.distinct().size
      val matterHours = distinctBlocksForMatter * 2.0
      MatterBillingSummary(
        matterCode = matter.code,
        matterName = matter.name,
        clientName = matter.clientName,
        loggedHours = matterHours,
        entriesCount = notesForMatter.size,
        lastActivity = notesForMatter.maxOfOrNull { it.recordedAt } ?: matter.createdAt
      )
    }

    val startOfToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayNotes = db.allNotes.filter { it.recordedAt >= startOfToday }
    val distinctTodayBlocks = todayNotes.map { it.twoHourBlockStart }.distinct().size
    val computedTodayHours = (distinctTodayBlocks * 2.0).coerceAtLeast(0.0)

    val currentUser = when (val authS = auth.authState) {
      is AuthState.Authenticated -> authS.user
      else -> null
    }

    val calendarStatus = if (currentUser != null && rag.calendarSync) {
      "Connected (${currentUser.email ?: "Google Account"}) • calendar.events active"
    } else if (rag.calendarSync) {
      "Connected (calendar.events scope enabled)"
    } else {
      "Calendar sync paused"
    }

    val totalTokens = db.tokenMetrics.sumOf { it.totalTokens }
    val totalCost = db.tokenMetrics.sumOf { it.estimatedCostUsd }

    ContextLogUiState(
      activeView = _activeView.value,
      dictationState = _dictationState.value,
      liveTranscript = _liveTranscript.value,
      rawVerifiedTranscript = _rawVerifiedTranscript.value,
      recordingDurationSeconds = _recordingDurationSeconds.value,
      toastMessage = _toastMessage.value,
      isProcessingIntelligence = _isProcessingIntelligence.value,
      inlineDictationTarget = _inlineDictationTarget.value,
      vaultFilter = _vaultFilter.value,
      vaultSearchQuery = _vaultSearchQuery.value,

      // Episodic Memory Ingestion State
      episodicMemories = db.episodicMemories,
      isIngestingEpisodic = _isIngestingEpisodic.value,
      episodicIngestStage = _episodicIngestStage.value,
      episodicSearchQuery = _episodicSearchQuery.value,
      selectedLifeStageFilter = _selectedLifeStageFilter.value,
      selectedValenceFilter = _selectedValenceFilter.value,
      selectedEpisodicMemory = _selectedEpisodicMemory.value,
      probingGapQuestion = _probingGapQuestion.value,
      probeAnswerText = _probeAnswerText.value,
      isProbingLoading = _isProbingLoading.value,
      episodicInputText = _episodicInputText.value,
      episodicImageDesc = _episodicImageDesc.value,

      // Interactive Memory Exploration Guide State
      isGuideSessionActive = _isGuideSessionActive.value,
      guideConversationMessages = _guideConversationMessages.value,
      isGuideThinking = _isGuideThinking.value,
      isGuideSpeaking = wispr.sub3.isSpeaking || liveAudioStreamManager.isPlaying.value,
      isGuideTtsEnabled = _isGuideTtsEnabled.value,
      guideInputText = _guideInputText.value,
      activeExploringMemory = _activeExploringMemory.value,

      // Phase 4: Gemini Multimodal Live API UI State
      isLiveMultimodalActive = _isLiveMultimodalActive.value,
      selectedLiveVoice = _selectedLiveVoice.value,
      liveConnectionStatus = geminiLiveClient.connectionStatus.value,
      liveMetrics = geminiLiveClient.liveMetrics.value,
      liveStatusMessage = geminiLiveClient.statusMessage.value,
      liveMicAmplitude = liveAudioStreamManager.micAmplitude.value,
      liveSpeakerAmplitude = liveAudioStreamManager.speakerAmplitude.value,
      isLiveBargeInActive = liveAudioStreamManager.isBargeInActive.value,
      isLiveRecordingMic = liveAudioStreamManager.isRecording.value,
      isLivePlayingSpeaker = liveAudioStreamManager.isPlaying.value,

      activeTab = nav.activeTab,
      todayHours = computedTodayHours,
      targetHours = 8.0,
      isSynced = true,
      notes = filteredNotes,
      matters = db.allMatters,
      billingSummaries = billingSummaries,
      rollups = db.allRollups,
      calendarEvents = db.calendarEvents,
      tokenMetrics = db.tokenMetrics,
      totalTokenSpend = totalTokens,
      totalEstimatedCostUsd = totalCost,
      documents = db.allDocs,
      indexingChunkCount = db.liveChunkCount,
      isIndexing = rag.isRagLoading,
      searchQuery = nav.searchQuery,
      selectedTypeFilter = nav.selectedTypeFilter,
      selectedTagFilter = nav.selectedTagFilter,
      availableTags = availableTags,
      isLogModalOpen = nav.isLogModalOpen,
      prefillLogText = nav.prefillLogText,
      prefillMatterCode = nav.prefillMatterCode,
      prefillEventTitle = nav.prefillEventTitle,
      autoStartRecordingInModal = nav.autoStartRecordingInModal,
      meetingAlertsEnabled = nav.meetingAlertsEnabled,
      isRagModalOpen = nav.isRagModalOpen,
      ragQuery = rag.ragQuery,
      ragAnswer = rag.ragAnswer,
      ragCitations = rag.ragCitations,
      isRagLoading = rag.isRagLoading,
      isLoggingInProgress = rag.isLoggingInProgress,
      googleCalendarSyncEnabled = rag.calendarSync,
      googleCalendarStatus = calendarStatus,
      autoSyncAndReindexEnabled = rag.autoSync,
      ingestionProgress = rag.progress,
      ingestionStatusText = rag.statusText,
      isIngesting = false,
      currentUser = currentUser,
      isAuthLoading = auth.isAuthLoading,
      authErrorMessage = auth.authError,
      lastCalendarSyncEventId = auth.lastEventId,

      // Theme & Cloud Backup State
      themeMode = nav.themeMode,
      backupFrequency = auth.backupState.frequency,
      lastBackupTime = auth.backupState.lastTime,
      lastBackupStatus = auth.backupState.status,
      lastBackupMetadata = auth.backupState.metadata,
      isBackingUp = auth.backupState.isBackingUp,

      // Wispr Flow
      wisprInput = wispr.sub1.input,
      wisprContext = wispr.sub1.ctx,
      wisprTone = wispr.sub1.tone,
      wisprLanguage = wispr.sub1.lang,
      wisprResult = wispr.sub1.res,
      isWisprProcessing = wispr.sub2.processing,
      activeTransform = wispr.sub2.actTransform,
      transformResult = wispr.sub2.resTransform,
      isTransformLoading = wispr.sub2.loadTransform,
      isRecordingAudio = wispr.sub2.recording,
      audioRecordingSeconds = wispr.sub3.recSeconds,
      micAmplitude = wispr.sub3.micAmp,
      micErrorMessage = wispr.sub3.micErr,
      isExtendedContextMode = wispr.sub3.extendedMode,
      dictionaryItems = wispr.dict,
      snippets = wispr.snips,

      // Data Accessibility & Audio Readback State
      isSpeaking = wispr.sub3.isSpeaking,
      speechRate = wispr.sub3.speechRate,
      dictationWaveformLevels = wispr.sub3.waveformLevels,
      dictationAnnouncement = wispr.sub3.announcement,
      dictationLanguage = wispr.sub3.dictLanguage,
      fontScale = nav.fontScale,
      highContrastMode = nav.highContrastMode,

      // Spatial Hardware & VoiceStudio Engine
      actionItems = db.actionItems,
      briefingDossiers = db.briefingDossiers,
      pendantConnected = _pendantConnected.value,
      batterySoC = _batterySoC.value,
      rssiDbm = -62,
      circularBufferSeconds = 300,
      voiceStudioEngine = _voiceStudioEngine.value,
      selectedASREngine = _selectedASREngine.value,
      voiceprintEnrolled = _voiceprintEnrolled.value,
      voiceprintSimilarity = 0.94f
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ContextLogUiState()
  )

  fun setActiveView(view: MainAppView) {
    _activeView.value = view
  }

  fun setVaultFilter(filter: VaultFilter) {
    _vaultFilter.value = filter
  }

  fun setVaultSearchQuery(query: String) {
    _vaultSearchQuery.value = query
  }

  fun clearToastMessage() {
    _toastMessage.value = null
  }

  fun startListeningSession() {
    _liveTranscript.value = ""
    _rawVerifiedTranscript.value = ""
    _dictationState.value = DictationState.LISTENING
    _recordingDurationSeconds.value = 0
    _isRecordingAudio.value = true

    recordingJob?.cancel()
    recordingJob = viewModelScope.launch {
      while (_dictationState.value == DictationState.LISTENING) {
        delay(1000L)
        _recordingDurationSeconds.value += 1
      }
    }

    try {
      mediaRecorderManager.startRecording()
    } catch (e: Exception) {
      android.util.Log.e("ContextLogViewModel", "MediaRecorder start error", e)
    }

    speechDictationManager.startListening(
      initialText = "",
      onResult = { finalTranscript ->
        if (finalTranscript.isNotBlank()) {
          _liveTranscript.value = finalTranscript
          _rawVerifiedTranscript.value = finalTranscript
        }
      }
    )
  }

  fun stopListeningSession() {
    if (_dictationState.value != DictationState.LISTENING) return
    _isRecordingAudio.value = false
    recordingJob?.cancel()
    recordingJob = null

    try {
      mediaRecorderManager.stopRecording()
    } catch (e: Exception) {
      android.util.Log.e("ContextLogViewModel", "MediaRecorder stop error", e)
    }

    val finalRecognized = speechDictationManager.stopListening()
    val transcriptToVerify = finalRecognized.ifBlank { _liveTranscript.value }
    _rawVerifiedTranscript.value = transcriptToVerify
    _dictationState.value = DictationState.VERIFICATION
  }

  fun discardDictation() {
    speechDictationManager.stopListening()
    try { mediaRecorderManager.stopRecording() } catch (_: Exception) {}
    recordingJob?.cancel()
    recordingJob = null
    _liveTranscript.value = ""
    _rawVerifiedTranscript.value = ""
    _dictationState.value = DictationState.IDLE
    _isRecordingAudio.value = false
  }

  fun updateVerifiedTranscript(text: String) {
    _rawVerifiedTranscript.value = text
  }

  fun saveAndProcessDictation(customText: String? = null) {
    val textToProcess = (customText ?: _rawVerifiedTranscript.value).ifBlank { _liveTranscript.value }.trim()
    if (textToProcess.isBlank()) {
      discardDictation()
      return
    }

    _dictationState.value = DictationState.PROCESSING
    _isProcessingIntelligence.value = true

    viewModelScope.launch {
      try {
        val structured = wisprEngine.processAudioOrTextToStructuredNotes(
          rawText = textToProcess,
          tone = WisprTone.AUTO_CLEAN
        )

        val noteTitle = structured.title.ifBlank { "Voice Note ${System.currentTimeMillis() % 10000}" }
        val cleanSummary = structured.executiveSummary.ifBlank { textToProcess }

        // 1. Save note to Room Notes table
        val newNote = ContextNoteEntity(
          title = noteTitle,
          rawTranscript = textToProcess,
          cleanText = cleanSummary,
          executiveSummary = cleanSummary,
          structuredNotes = structured.keyDecisions.joinToString("\n• "),
          entryType = if (structured.calendarEvent?.hasEvent == true) EntryType.REMINDER else EntryType.LOG,
          recordedAt = System.currentTimeMillis()
        )
        repository.insertNote(newNote)

        // 2. Extract and route action items into Room Tasks table
        val extractedTasks = structured.extractedTasks
        var tasksCount = 0
        if (extractedTasks.isNotEmpty()) {
          val tasksToInsert = extractedTasks.map { task ->
            ActionItemEntity(
              title = task.title,
              owner = task.assignee,
              isAssignedToYou = task.assignee.equals("You", ignoreCase = true) || task.assignee.equals("Me", ignoreCase = true),
              actionVerb = task.title.split(" ").firstOrNull() ?: "Execute",
              dueDateFormatted = task.dueDate,
              isCompleted = false,
              priority = task.priority,
              memoryId = newNote.id,
              memoryTitle = newNote.title,
              externalSyncTarget = "Task System",
              externalSyncStatus = "READY"
            )
          }
          repository.insertActionItems(tasksToInsert)
          tasksCount = tasksToInsert.size
        } else if (structured.actionItems.isNotEmpty()) {
          val fallbackTasks = structured.actionItems.map { act ->
            ActionItemEntity(
              title = act,
              owner = "You",
              isAssignedToYou = true,
              actionVerb = act.split(" ").firstOrNull() ?: "Execute",
              dueDateFormatted = "Today, 5:00 PM",
              isCompleted = false,
              priority = "HIGH",
              memoryId = newNote.id,
              memoryTitle = newNote.title,
              externalSyncTarget = "Task System",
              externalSyncStatus = "READY"
            )
          }
          repository.insertActionItems(fallbackTasks)
          tasksCount = fallbackTasks.size
        }

        // 3. Extract and route calendar events into Room & Native Device Calendar
        val cal = structured.calendarEvent
        var eventTitle: String? = null
        if (cal != null && cal.hasEvent) {
          eventTitle = cal.title
          val calEntity = CalendarEventEntity(
            title = cal.title,
            description = cal.description,
            startTime = cal.startEpochMs,
            endTime = cal.endEpochMs,
            location = cal.location,
            isSynced = true
          )
          repository.insertCalendarEvent(calEntity)
          try {
            deviceCalendarManager.insertEvent(
              title = cal.title,
              description = cal.description,
              startEpochMs = cal.startEpochMs,
              endEpochMs = cal.endEpochMs,
              location = cal.location
            )
          } catch (e: Exception) {
            android.util.Log.e("ContextLogViewModel", "Device calendar sync error", e)
          }
        }

        // 4. Auto-update 2-hour executive rollup for immediate report generation
        try {
          val now = System.currentTimeMillis()
          val twoHoursMs = 2 * 60 * 60 * 1000L
          val blockStart = (now / twoHoursMs) * twoHoursMs
          repository.generateAndSaveRollup(blockStart)
        } catch (e: Exception) {
          android.util.Log.w("ContextLogViewModel", "Auto-rollup generation notice", e)
        }

        _toastMessage.value = RoutingToast(
          message = "Note, tasks & event saved to Vault",
          noteTitle = noteTitle,
          tasksCount = tasksCount,
          eventTitle = eventTitle
        )
        _dictationState.value = DictationState.CONFIRMATION

        // Allow user sufficient time to review confirmation or switch to Vault
        delay(6000L)
        if (_dictationState.value == DictationState.CONFIRMATION) {
          _dictationState.value = DictationState.IDLE
          _liveTranscript.value = ""
          _rawVerifiedTranscript.value = ""
        }
      } catch (e: Exception) {
        android.util.Log.e("ContextLogViewModel", "Error in saveAndProcessDictation", e)
        _dictationState.value = DictationState.IDLE
      } finally {
        _isProcessingIntelligence.value = false
      }
    }
  }

  fun generateExecutiveReportForCurrentBlock() {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val twoHoursMs = 2 * 60 * 60 * 1000L
      val blockStart = (now / twoHoursMs) * twoHoursMs
      try {
        repository.generateAndSaveRollup(blockStart)
        _vaultFilter.value = VaultFilter.REPORTS
      } catch (e: Exception) {
        android.util.Log.e("ContextLogViewModel", "Error generating executive report", e)
      }
    }
  }

  fun deleteCalendarEvent(id: String) {
    viewModelScope.launch {
      repository.deleteCalendarEvent(id)
    }
  }

  // Episodic Memory Ingestion Engine Operations
  fun setEpisodicSearchQuery(query: String) {
    _episodicSearchQuery.value = query
  }

  fun setSelectedLifeStageFilter(stage: String?) {
    _selectedLifeStageFilter.value = if (_selectedLifeStageFilter.value == stage) null else stage
  }

  fun setSelectedValenceFilter(valence: String?) {
    _selectedValenceFilter.value = if (_selectedValenceFilter.value == valence) null else valence
  }

  fun setEpisodicInputText(text: String) {
    _episodicInputText.value = text
  }

  fun setEpisodicImageDesc(desc: String) {
    _episodicImageDesc.value = desc
  }

  fun selectEpisodicMemory(memory: com.example.data.local.EpisodicMemoryEntity?) {
    _selectedEpisodicMemory.value = memory
    _probingGapQuestion.value = null
    _probeAnswerText.value = ""
  }

  fun startProbingGap(question: String?) {
    _probingGapQuestion.value = question
    _probeAnswerText.value = ""
  }

  fun setProbeAnswerText(text: String) {
    _probeAnswerText.value = text
  }

  fun deleteEpisodicMemory(id: String) {
    viewModelScope.launch {
      repository.deleteEpisodicMemory(id)
      if (_selectedEpisodicMemory.value?.id == id) {
        _selectedEpisodicMemory.value = null
      }
    }
  }

  fun submitProbeAnswer(memoryId: String, gapQuestion: String, userClarification: String) {
    if (userClarification.isBlank()) return
    viewModelScope.launch {
      _isProbingLoading.value = true
      try {
        val updated = repository.resolveEpisodicGap(memoryId, gapQuestion, userClarification)
        if (updated != null) {
          _selectedEpisodicMemory.value = updated
          _probingGapQuestion.value = null
          _probeAnswerText.value = ""
        }
      } catch (e: Exception) {
        android.util.Log.e("ContextLogViewModel", "Error resolving episodic gap", e)
      } finally {
        _isProbingLoading.value = false
      }
    }
  }

  fun ingestEpisodicMemory(text: String? = null, imageDesc: String? = null) {
    val rawText = text ?: _episodicInputText.value
    val image = imageDesc ?: _episodicImageDesc.value
    if (rawText.isBlank() && image.isBlank()) return

    viewModelScope.launch {
      _isIngestingEpisodic.value = true
      try {
        _episodicIngestStage.value = "Temporal Anchoring & Era Mapping..."
        delay(350)
        _episodicIngestStage.value = "Entity & Relationship Graphing..."
        delay(350)
        _episodicIngestStage.value = "Emotional Profile & Sensory Extraction..."
        delay(350)
        _episodicIngestStage.value = "Unresolved Gaps & Semantic Indexing..."

        val nodeEntity = repository.ingestEpisodicMemory(
          rawText = rawText,
          imageDescription = image.ifBlank { null }
        )
        _selectedEpisodicMemory.value = nodeEntity
        _episodicInputText.value = ""
        _episodicImageDesc.value = ""
      } catch (e: Exception) {
        android.util.Log.e("ContextLogViewModel", "Error ingesting episodic memory", e)
      } finally {
        _isIngestingEpisodic.value = false
        _episodicIngestStage.value = ""
      }
    }
  }

  // --- Interactive Memory Exploration Guide Operations ---

  fun startMemoryGuideSession(memory: com.example.data.local.EpisodicMemoryEntity? = null) {
    _activeExploringMemory.value = memory
    _isGuideSessionActive.value = true
    _guideConversationMessages.value = emptyList()
    _guideInputText.value = ""

    viewModelScope.launch {
      _isGuideThinking.value = true
      val greeting = repository.generateMemoryGuideGreeting(memory)
      val initialMsg = MemoryGuideMessage(
        sender = GuideSender.GUIDE,
        text = greeting
      )
      _guideConversationMessages.value = listOf(initialMsg)
      _isGuideThinking.value = false

      if (_isGuideTtsEnabled.value) {
        textToSpeechHelper.speak(greeting)
      }
    }
  }

  fun sendMemoryGuideMessage(userText: String, sensoryCue: String? = null) {
    if (userText.isBlank() && sensoryCue == null) return
    val textToSend = userText.ifBlank { sensoryCue ?: "" }

    val userMsg = MemoryGuideMessage(
      sender = GuideSender.USER,
      text = textToSend,
      sensoryFocus = sensoryCue
    )
    val currentHistory = _guideConversationMessages.value + userMsg
    _guideConversationMessages.value = currentHistory
    _guideInputText.value = ""

    viewModelScope.launch {
      _isGuideThinking.value = true
      val executedTools = mutableListOf<com.example.data.ai.ToolCallInfo>()
      val responseText = repository.conductMemoryGuideTurn(
        userMessage = textToSend,
        history = currentHistory,
        memoryContext = _activeExploringMemory.value,
        sensoryPromptCue = sensoryCue,
        onToolExecuted = { toolInfo ->
          executedTools.add(toolInfo)
        }
      )
      val guideMsg = MemoryGuideMessage(
        sender = GuideSender.GUIDE,
        text = responseText,
        sensoryFocus = sensoryCue,
        executedTools = executedTools
      )
      _guideConversationMessages.value = currentHistory + guideMsg
      _isGuideThinking.value = false

      if (_isGuideTtsEnabled.value) {
        textToSpeechHelper.speak(responseText)
      }
    }
  }

  fun triggerSensoryAnchorPrompt(cue: String) {
    sendMemoryGuideMessage(userText = "", sensoryCue = cue)
  }

  fun toggleGuideTts() {
    val newState = !_isGuideTtsEnabled.value
    _isGuideTtsEnabled.value = newState
    if (!newState) {
      textToSpeechHelper.stop()
    }
  }

  fun stopGuideSpeaking() {
    textToSpeechHelper.stop()
  }

  fun setGuideInputText(text: String) {
    _guideInputText.value = text
  }

  fun endMemoryGuideSession() {
    textToSpeechHelper.stop()
    stopMultimodalLiveSession()
    _isGuideSessionActive.value = false
    _activeExploringMemory.value = null
    _guideConversationMessages.value = emptyList()
    _guideInputText.value = ""
  }

  // --- Phase 4: Gemini Multimodal Live API Operations ---

  fun startMultimodalLiveSession(memory: com.example.data.local.EpisodicMemoryEntity? = null) {
    _activeExploringMemory.value = memory
    _isGuideSessionActive.value = true
    _isLiveMultimodalActive.value = true
    textToSpeechHelper.stop()

    val contextSummary = if (memory != null) {
      "- Target Memory ID: ${memory.id}\n- Timeframe: ${memory.timeframeReferenced} (${memory.relativeLifeStage})\n- Narrative: ${memory.narrativeSummary}\n- Sensory anchors: ${memory.getSensoryCuesList().joinToString(", ")}\n- Unresolved gaps: ${memory.getUnresolvedGapsList().joinToString("; ")}"
    } else null

    geminiLiveClient.connectLiveSession(viewModelScope, contextSummary)

    liveAudioStreamManager.startCapture(viewModelScope) { pcm16k ->
      geminiLiveClient.sendMicrophonePcmChunk(pcm16k)
    }
  }

  fun stopMultimodalLiveSession() {
    _isLiveMultimodalActive.value = false
    liveAudioStreamManager.stopCapture()
    liveAudioStreamManager.flushSpeakerBuffer("Live session stopped")
    geminiLiveClient.disconnect()
  }

  fun toggleMultimodalLiveMode() {
    if (_isLiveMultimodalActive.value) {
      stopMultimodalLiveSession()
    } else {
      startMultimodalLiveSession(_activeExploringMemory.value)
    }
  }

  fun setLiveVoice(voiceName: String) {
    _selectedLiveVoice.value = voiceName
    geminiLiveClient.setVoice(voiceName)
  }

  fun setSelectedLiveVoice(voiceName: String) {
    setLiveVoice(voiceName)
  }

  fun triggerLiveBargeIn() {
    liveAudioStreamManager.flushSpeakerBuffer("User manual barge-in trigger")
  }

  private fun handleLiveToolExecution(functionName: String, args: org.json.JSONObject): org.json.JSONObject {
    return runBlocking(Dispatchers.IO) {
      if (functionName == "retrieve_memories") {
        val query = args.optString("query", "")
        val timeframe = if (args.has("timeframe")) args.optString("timeframe") else null
        val entityFilter = if (args.has("entity_filter")) args.optString("entity_filter") else null

        val allMemories = repository.getAllEpisodicMemoriesSync()
        val queryTerms = query.lowercase().split(Regex("[\\s,]+")).filter { it.length > 2 }

        val matched = allMemories.filter { mem ->
          val fullText = "${mem.narrativeSummary} ${mem.timeframeReferenced} ${mem.peopleJson} ${mem.locationsJson} ${mem.sensoryCuesJson} ${mem.imageDescription.orEmpty()}".lowercase()
          val matchesQuery = if (queryTerms.isEmpty()) true else queryTerms.any { term -> fullText.contains(term) }
          val matchesTime = if (timeframe.isNullOrBlank()) true else fullText.contains(timeframe.lowercase())
          val matchesEntity = if (entityFilter.isNullOrBlank()) true else fullText.contains(entityFilter.lowercase())
          matchesQuery && matchesTime && matchesEntity
        }.ifEmpty {
          if (entityFilter != null) {
            allMemories.filter { it.peopleJson.contains(entityFilter, ignoreCase = true) || it.locationsJson.contains(entityFilter, ignoreCase = true) }
          } else if (timeframe != null) {
            allMemories.filter { it.timeframeReferenced.contains(timeframe, ignoreCase = true) }
          } else {
            allMemories.take(2)
          }
        }

        val resultObj = org.json.JSONObject()
        if (matched.isNotEmpty()) {
          resultObj.put("status", "success")
          resultObj.put("found", true)
          val resultsArray = org.json.JSONArray()
          for (m in matched.take(3)) {
            resultsArray.put(org.json.JSONObject().apply {
              put("memory_id", m.id)
              put("timeframe", m.timeframeReferenced)
              put("summary", m.narrativeSummary)
              put("sensory_cues", org.json.JSONArray(m.getSensoryCuesList()))
              put("unresolved_gaps", org.json.JSONArray(m.getUnresolvedGapsList()))
              if (!m.imageDescription.isNullOrBlank()) {
                put("photo_context", m.imageDescription)
              }
            })
          }
          resultObj.put("results", resultsArray)
        } else {
          resultObj.put("status", "success")
          resultObj.put("found", false)
          resultObj.put("message", "No matching memories found in archive for '$query'")
        }
        resultObj
      } else if (functionName == "update_memory_node") {
        val memoryId = args.optString("memory_id", "")
        val gapsArray = args.optJSONArray("resolved_gaps")
        val gapsList = mutableListOf<String>()
        if (gapsArray != null) {
          for (i in 0 until gapsArray.length()) {
            gapsList.add(gapsArray.getString(i))
          }
        }
        val newInsights = args.optString("new_insights", "")

        val existing = repository.getEpisodicMemoryByIdSync(memoryId)
        if (existing != null) {
          val currentGaps = existing.getUnresolvedGapsList().toMutableList()
          currentGaps.removeAll { gap -> gapsList.any { g -> gap.contains(g, ignoreCase = true) } }
          val updatedSummary = if (newInsights.isNotBlank()) {
            "${existing.narrativeSummary} [Reflective Insight: $newInsights]"
          } else existing.narrativeSummary

          val updated = existing.copy(
            narrativeSummary = updatedSummary,
            unresolvedGapsJson = org.json.JSONArray(currentGaps).toString()
          )
          repository.updateEpisodicMemory(updated)
        }
        org.json.JSONObject().apply {
          put("status", "success")
          put("updated_id", memoryId)
          put("message", "Memory node updated with reflective insights.")
        }
      } else {
        org.json.JSONObject().apply {
          put("status", "error")
          put("message", "Unknown tool: $functionName")
        }
      }
    }
  }

  fun saveGuideExplorationAsEnrichedMemory() {
    val memory = _activeExploringMemory.value ?: return
    val messages = _guideConversationMessages.value
    if (messages.isEmpty()) return

    viewModelScope.launch {
      val userReflections = messages.filter { it.sender == GuideSender.USER }.joinToString(" ") { it.text }
      if (userReflections.isNotBlank()) {
        val updated = repository.resolveEpisodicGap(
          memoryId = memory.id,
          gapQuestion = "Voice Guide Reflection Insights",
          userClarification = userReflections
        )
        if (updated != null) {
          _activeExploringMemory.value = updated
          _selectedEpisodicMemory.value = updated
        }
      }
    }
  }

  fun seedSampleEpisodicMemoryIfEmpty() {
    viewModelScope.launch {
      val existing = repository.allEpisodicMemories
      // Can be triggered from UI button or initial state
    }
  }

  fun startInlineFieldDictation(targetFieldId: String, currentText: String, onUpdate: (String) -> Unit) {
    if (_inlineDictationTarget.value != null) {
      stopInlineFieldDictation()
    }
    _inlineDictationTarget.value = targetFieldId
    inlineCallback = onUpdate

    speechDictationManager.startListening(
      initialText = currentText,
      onResult = { finalTranscript ->
        inlineCallback?.invoke(finalTranscript)
      }
    )
  }

  fun stopInlineFieldDictation() {
    if (_inlineDictationTarget.value == null) return
    val finalResult = speechDictationManager.stopListening()
    if (finalResult.isNotBlank()) {
      inlineCallback?.invoke(finalResult)
    }
    _inlineDictationTarget.value = null
    inlineCallback = null
  }

  fun selectTab(tabIndex: Int) {
    if (_isRecordingAudio.value && tabIndex != 1) {
      stopAudioRecording()
    }
    _activeTab.value = tabIndex
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectTypeFilter(type: EntryType?) {
    if (_selectedTypeFilter.value == type) {
      _selectedTypeFilter.value = null
    } else {
      _selectedTypeFilter.value = type
    }
  }

  fun selectTagFilter(tag: String?) {
    if (_selectedTagFilter.value == tag) {
      _selectedTagFilter.value = null
    } else {
      _selectedTagFilter.value = tag
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    appPrefs.edit().putString("theme_mode", mode.name).apply()
    _themeMode.value = mode
  }

  fun setBackupFrequency(frequency: BackupFrequency) {
    cloudBackupManager.setBackupFrequency(frequency)
  }

  fun performBackupNow() {
    viewModelScope.launch {
      try {
        cloudBackupManager.performBackupNow()
      } catch (e: Exception) {
        // Handled in CloudBackupManager state
      }
    }
  }

  fun triggerBackgroundWorkerSync() {
    cloudBackupManager.triggerImmediateWorkerSync()
  }

  fun addTagToNote(noteId: String, tag: String) {
    viewModelScope.launch {
      repository.addTagToNote(noteId, tag)
    }
  }

  fun removeTagFromNote(noteId: String, tag: String) {
    viewModelScope.launch {
      repository.removeTagFromNote(noteId, tag)
    }
  }

  fun openLogModal(
    prefillTranscript: String = "",
    prefillMatterCode: String = "",
    prefillEventTitle: String = "",
    autoStartRecording: Boolean = false
  ) {
    _prefillTranscript.value = prefillTranscript
    _prefillMatterCode.value = prefillMatterCode
    _prefillEventTitle.value = prefillEventTitle
    _autoStartRecordingInModal.value = autoStartRecording
    _isLogModalOpen.value = true
  }

  fun closeLogModal() {
    _isLogModalOpen.value = false
    _prefillTranscript.value = ""
    _prefillMatterCode.value = ""
    _prefillEventTitle.value = ""
    _autoStartRecordingInModal.value = false
  }

  fun triggerTest5MinNotification(
    eventTitle: String = "Acme Settlement Deposition",
    matterCode: String = "LGL-9021"
  ) {
    com.example.notification.CalendarNotificationScheduler.triggerImmediateTest5MinAlert(
      context = getApplication(),
      eventTitle = eventTitle,
      matterCode = matterCode
    )
  }

  fun toggleMeetingAlerts(enabled: Boolean) {
    _meetingAlertsEnabled.value = enabled
    appPrefs.edit().putBoolean("meeting_alerts_enabled", enabled).apply()
    if (enabled) {
      viewModelScope.launch {
        val events = repository.allCalendarEvents.firstOrNull() ?: emptyList()
        com.example.notification.CalendarNotificationScheduler.scheduleAllUpcomingEvents(getApplication(), events)
      }
    }
  }

  fun openRagModal() {
    _isRagModalOpen.value = true
  }

  fun closeRagModal() {
    _isRagModalOpen.value = false
  }

  fun setRagQuery(query: String) {
    _ragQuery.value = query
  }

  fun toggleGoogleCalendarSync(enabled: Boolean) {
    _googleCalendarSyncEnabled.value = enabled
  }

  fun toggleAutoSyncAndReindex(enabled: Boolean) {
    _autoSyncAndReindexEnabled.value = enabled
    if (enabled) {
      reindexAllDocuments()
    }
  }

  fun toggleExtendedContextMode(enabled: Boolean) {
    _isExtendedContextMode.value = enabled
    if (enabled && _wisprContext.value == WisprContextType.GENERAL) {
      _wisprContext.value = WisprContextType.LONG_CONTEXT_SYNC
    }
  }

  fun loadLongContextSample() {
    val longMeetingTranscript = """
      [09:00 AM - Sprint & Architecture Sync]
      Participants: Akash (Lead Counsel / Eng), Priya (Product), David (Cloud Architect)
      
      Akash: Let's review the Q3 deliverable timeline for Matter LGL-9021 and client ContextLog deployment. We need to finalize the Google Calendar OAuth permissions, especially the calendar.events scope.
      
      Priya: Yes, we also noticed that users dictating speech in noisy environments need the natural speech cleanup to remove 'um' and 'actually no wait' self-corrections. Can we ensure the Gemini 3.5 Flash engine processes both 2-hour rollups and immediate transcription?
      
      David: On the cloud backend, we validated the SQLite Room database and vector embeddings with 768 dimensions. We must ensure document chunks are indexed in real-time. Action item for me: deploy the automated reindexing worker by Thursday 5 PM.
      
      Akash: Agreed. I will draft the legal settlement memorandum for matter CTX-2024-08, review the NDA clauses, and schedule the follow-up client deposition for next Tuesday at 3:30 PM.
      
      Priya: Great. Decision made: we migrate to the single-tap speech flow UI with live mic waveform feedback, and auto-sync calendar reminders upon note completion. Meeting adjourned.
    """.trimIndent()

    _wisprInput.value = longMeetingTranscript
    _wisprContext.value = WisprContextType.LONG_CONTEXT_SYNC
    _isExtendedContextMode.value = true
  }

  fun signInWithGoogle() {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      val result = authManager.signInWithGoogle()
      if (result.isFailure) {
        _authErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Google Sign-In failed"
      }
      _isAuthLoading.value = false
    }
  }

  fun signOut() {
    viewModelScope.launch {
      _isAuthLoading.value = true
      authManager.signOut()
      _isAuthLoading.value = false
    }
  }

  fun syncNoteToCalendar(note: ContextNoteEntity) {
    viewModelScope.launch {
      val success = repository.syncNoteToCalendar(note)
      if (success) {
        _lastCalendarSyncEventId.value = "Synced ${note.matterCode} event"
      }
    }
  }

  fun syncCalendarEvents() {
    viewModelScope.launch {
      _lastCalendarSyncEventId.value = "Syncing Google Calendar events..."
      val token = authManager.getAccessToken()
      val events = repository.fetchAndSyncCalendarEvents(token)
      _lastCalendarSyncEventId.value = "Synced ${events.size} upcoming calendar events"
    }
  }

  fun submitLog(rawTranscript: String) {
    if (rawTranscript.isBlank()) return
    viewModelScope.launch {
      _isLoggingInProgress.value = true
      repository.parseAndSaveVoiceNote(
        rawTranscript = rawTranscript,
        syncToCalendar = _googleCalendarSyncEnabled.value
      )
      _isLoggingInProgress.value = false
      _isLogModalOpen.value = false
    }
  }

  fun submitRagSearch(query: String = _ragQuery.value) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _isRagLoading.value = true
      val result = repository.performRAGSearch(query)
      _ragAnswer.value = result.first
      _ragCitations.value = result.second
      _isRagLoading.value = false
    }
  }

  fun ingestDocument(title: String, content: String) {
    if (title.isBlank() || content.isBlank()) return
    viewModelScope.launch {
      _isIngesting.value = true
      _isRagLoading.value = true
      _ingestionProgress.value = 0.05f
      _ingestionStatusText.value = "Initializing document ingestion..."

      repository.ingestDocument(
        title = title,
        content = content,
        onProgress = { statusText, progressFloat ->
          _ingestionStatusText.value = statusText
          _ingestionProgress.value = progressFloat
        }
      )

      _isIngesting.value = false
      _isRagLoading.value = false
    }
  }

  fun reindexAllDocuments() {
    viewModelScope.launch {
      _isIngesting.value = true
      _isRagLoading.value = true
      _ingestionProgress.value = 0.05f
      _ingestionStatusText.value = "Starting full index re-synchronization..."

      repository.reindexAllDocuments(
        onProgress = { statusText, progressFloat ->
          _ingestionStatusText.value = statusText
          _ingestionProgress.value = progressFloat
        }
      )

      _isIngesting.value = false
      _isRagLoading.value = false
    }
  }

  fun deleteNote(id: String) {
    viewModelScope.launch {
      repository.deleteNote(id)
    }
  }

  fun addMatter(code: String, name: String, clientName: String) {
    if (code.isBlank() || name.isBlank()) return
    viewModelScope.launch {
      repository.addMatter(code, name, clientName)
    }
  }

  // --- Wispr Flow Operations ---

  fun setWisprInput(text: String) {
    _wisprInput.value = text
  }

  fun setWisprContext(type: WisprContextType) {
    _wisprContext.value = type
  }

  fun setWisprTone(tone: WisprTone) {
    _wisprTone.value = tone
  }

  fun setWisprLanguage(language: String) {
    _wisprLanguage.value = language
  }

  fun runWisprFlow(
    input: String = _wisprInput.value,
    contextType: WisprContextType = _wisprContext.value,
    tone: WisprTone = _wisprTone.value,
    language: String = _wisprLanguage.value
  ) {
    if (input.isBlank()) return
    viewModelScope.launch {
      _isWisprProcessing.value = true
      val targetLang = if (language.equals("English", ignoreCase = true)) null else language
      val result = repository.processWisprFlow(
        rawInput = input,
        contextType = contextType,
        tone = tone,
        targetLanguage = targetLang
      )
      _wisprResult.value = result
      _isWisprProcessing.value = false
    }
  }

  fun runAiTransform(transform: WisprTransform, customPrompt: String? = null) {
    val textToTransform = _wisprResult.value?.cleanText ?: _wisprInput.value
    if (textToTransform.isBlank()) return
    viewModelScope.launch {
      _isTransformLoading.value = true
      _activeTransform.value = transform
      val result = repository.executeAiTransform(
        input = textToTransform,
        transform = transform,
        customInstruction = customPrompt
      )
      _transformResult.value = result
      _isTransformLoading.value = false
    }
  }

  fun saveWisprResultAsNote(matterCode: String = "CTX-2024-08", syncToCalendar: Boolean = false) {
    val result = _wisprResult.value ?: return
    val textToSave = result.toneRewrittenText ?: result.formattedText.ifBlank { result.cleanText }
    viewModelScope.launch {
      _isLoggingInProgress.value = true
      repository.parseAndSaveVoiceNote(
        rawTranscript = textToSave,
        syncToCalendar = syncToCalendar
      )
      _isLoggingInProgress.value = false
    }
  }

  fun saveDirectTranscript(text: String, title: String = "Voice Note") {
    if (text.isBlank()) return
    viewModelScope.launch {
      _isLoggingInProgress.value = true
      repository.parseAndSaveVoiceNote(
        rawTranscript = text,
        syncToCalendar = false
      )
      _isLoggingInProgress.value = false
    }
  }

  fun toggleAudioRecording() {
    if (_isRecordingAudio.value) {
      stopAudioRecording()
    } else {
      startAudioRecording()
    }
  }

  private fun startAudioRecording() {
    _isRecordingAudio.value = true
    _audioRecordingSeconds.value = 0
    recordingJob?.cancel()
    recordingJob = viewModelScope.launch {
      while (_isRecordingAudio.value) {
        delay(1000)
        _audioRecordingSeconds.value = _audioRecordingSeconds.value + 1
      }
    }

    // Start MediaRecorder component caching audio on device
    mediaRecorderManager.startRecording()

    // Start Speech dictation listener for real-time text streaming
    speechDictationManager.startListening(initialText = "") { finalResult ->
      if (finalResult.isNotBlank()) {
        _wisprInput.value = finalResult
      }
    }
  }

  private fun stopAudioRecording() {
    _isRecordingAudio.value = false
    recordingJob?.cancel()
    
    // Stop recording and retrieve cached audio file
    val audioFile = mediaRecorderManager.stopRecording()
    speechDictationManager.stopListening()

    // Process the audio and transcript with Gemini API utility function
    processAudioFileThroughGemini(audioFile)
  }

  /**
   * Transmits raw text or cached audio recording to the Gemini API
   * and automatically routes structured meeting notes, tasks, and calendar events into Room and native calendar.
   */
  fun processAudioFileThroughGemini(audioFile: File? = null) {
    viewModelScope.launch {
      _isWisprProcessing.value = true
      val audioBase64 = if (audioFile != null) mediaRecorderManager.getAudioBase64(audioFile) else null
      val currentText = _wisprInput.value

      val structured = wisprEngine.processAudioOrTextToStructuredNotes(
        rawText = currentText,
        audioBase64 = audioBase64,
        audioMimeType = "audio/mp4"
      )

      val noteTitle = structured.title.ifBlank { "Voice Note ${System.currentTimeMillis() % 10000}" }
      val cleanSummary = structured.executiveSummary.ifBlank { currentText }

      // 1. Save synthesized note to Room Notes table
      val newNote = ContextNoteEntity(
        title = noteTitle,
        rawTranscript = currentText.ifBlank { cleanSummary },
        cleanText = cleanSummary,
        executiveSummary = cleanSummary,
        structuredNotes = structured.keyDecisions.joinToString("\n• "),
        entryType = if (structured.calendarEvent?.hasEvent == true) EntryType.REMINDER else EntryType.LOG,
        recordedAt = System.currentTimeMillis()
      )
      repository.insertNote(newNote)

      // 2. Extract and route action items into Room Tasks table
      val extractedTasks = structured.extractedTasks
      if (extractedTasks.isNotEmpty()) {
        val tasksToInsert = extractedTasks.map { task ->
          ActionItemEntity(
            title = task.title,
            owner = task.assignee,
            isAssignedToYou = task.assignee.equals("You", ignoreCase = true) || task.assignee.equals("Me", ignoreCase = true),
            actionVerb = task.title.split(" ").firstOrNull() ?: "Execute",
            dueDateFormatted = task.dueDate,
            isCompleted = false,
            priority = task.priority,
            memoryId = newNote.id,
            memoryTitle = newNote.title,
            externalSyncTarget = "Task System",
            externalSyncStatus = "READY"
          )
        }
        repository.insertActionItems(tasksToInsert)
      } else if (structured.actionItems.isNotEmpty()) {
        val fallbackTasks = structured.actionItems.map { act ->
          ActionItemEntity(
            title = act,
            owner = "You",
            isAssignedToYou = true,
            actionVerb = act.split(" ").firstOrNull() ?: "Execute",
            dueDateFormatted = "Today, 5:00 PM",
            isCompleted = false,
            priority = "HIGH",
            memoryId = newNote.id,
            memoryTitle = newNote.title,
            externalSyncTarget = "Task System",
            externalSyncStatus = "READY"
          )
        }
        repository.insertActionItems(fallbackTasks)
      }

      // 3. Extract and route calendar events into Room & Native Device Calendar
      val cal = structured.calendarEvent
      if (cal != null && cal.hasEvent) {
        val calEntity = CalendarEventEntity(
          title = cal.title,
          description = cal.description,
          startTime = cal.startEpochMs,
          endTime = cal.endEpochMs,
          location = cal.location,
          isSynced = true
        )
        repository.insertCalendarEvent(calEntity)
        try {
          deviceCalendarManager.insertEvent(
            title = cal.title,
            description = cal.description,
            startEpochMs = cal.startEpochMs,
            endEpochMs = cal.endEpochMs,
            location = cal.location
          )
        } catch (e: Exception) {
          android.util.Log.e("ContextLogViewModel", "Device calendar sync error", e)
        }
      }

      _wisprResult.value = WisprFlowResult(
        rawTranscript = currentText,
        cleanText = cleanSummary,
        formattedText = cleanSummary,
        toneRewrittenText = cleanSummary,
        actionItems = structured.actionItems,
        structuredMeeting = structured,
        latencyMs = 120L,
        tokenCountEstimate = (cleanSummary.length / 4).coerceAtLeast(12)
      )

      _isWisprProcessing.value = false
    }
  }

  fun addDictionaryItem(term: String, category: String, notes: String? = null) {
    if (term.isBlank()) return
    viewModelScope.launch {
      repository.addDictionaryItem(term, category, notes)
    }
  }

  fun deleteDictionaryItem(id: String) {
    viewModelScope.launch {
      repository.deleteDictionaryItem(id)
    }
  }

  fun addSnippet(triggerPhrase: String, expandedText: String, description: String = "", category: String = "GENERAL") {
    if (triggerPhrase.isBlank() || expandedText.isBlank()) return
    viewModelScope.launch {
      repository.addSnippet(triggerPhrase, expandedText, description, category)
    }
  }

  fun deleteSnippet(id: String) {
    viewModelScope.launch {
      repository.deleteSnippet(id)
    }
  }

  fun speakText(text: String) {
    textToSpeechHelper.speak(text)
  }

  fun stopSpeaking() {
    textToSpeechHelper.stop()
  }

  fun setSpeechRate(rate: Float) {
    textToSpeechHelper.setSpeechRate(rate)
  }

  fun setDictationLanguage(languageCode: String) {
    speechDictationManager.setLanguage(languageCode)
  }

  fun selectDictationLanguage(languageCode: String) {
    setDictationLanguage(languageCode)
  }

  fun setFontScale(scale: Float) {
    _fontScale.value = scale
    appPrefs.edit().putFloat("font_scale", scale).apply()
  }

  fun toggleHighContrast(enabled: Boolean) {
    _highContrast.value = enabled
    appPrefs.edit().putBoolean("high_contrast", enabled).apply()
  }

  // Deterministic Task Pipeline
  fun toggleTask(id: String, completed: Boolean) {
    viewModelScope.launch {
      repository.toggleActionItemCompletion(id, completed)
    }
  }

  fun addTask(
    title: String,
    owner: String = "You",
    isAssignedToYou: Boolean = true,
    dueDateFormatted: String = "Today, 5:00 PM",
    priority: String = "HIGH",
    externalSyncTarget: String = "Task System",
    memoryTitle: String = "Manual Action Item"
  ) {
    viewModelScope.launch {
      val newItem = ActionItemEntity(
        title = title.trim(),
        owner = owner.trim(),
        isAssignedToYou = isAssignedToYou,
        actionVerb = title.split(" ").firstOrNull() ?: "Execute",
        dueDateFormatted = dueDateFormatted,
        isCompleted = false,
        priority = priority,
        memoryTitle = memoryTitle,
        externalSyncTarget = externalSyncTarget,
        externalSyncStatus = "READY"
      )
      repository.insertActionItem(newItem)
    }
  }

  fun updateTask(item: ActionItemEntity) {
    viewModelScope.launch {
      repository.updateActionItem(item)
    }
  }

  fun deleteTask(id: String) {
    viewModelScope.launch {
      repository.deleteActionItem(id)
    }
  }

  fun updateSyncStatus(id: String, status: String, target: String) {
    viewModelScope.launch {
      repository.updateActionItemSyncStatus(id, status, target)
    }
  }

  // Vector RAG & Briefing Dossiers
  fun askRagQuestion(query: String) {
    if (query.isBlank()) return
    _ragQuery.value = query
    _isRagLoading.value = true
    viewModelScope.launch {
      val (answer, citations) = repository.performRAGSearch(query)
      _ragAnswer.value = answer
      _ragCitations.value = citations
      _isRagLoading.value = false
    }
  }

  fun generateBriefingDossier(targetPersonOrTopic: String) {
    if (targetPersonOrTopic.isBlank()) return
    viewModelScope.launch {
      _isRagLoading.value = true
      repository.generatePreMeetingBriefing(targetPersonOrTopic)
      _isRagLoading.value = false
    }
  }

  fun deleteBriefingDossier(id: String) {
    viewModelScope.launch {
      repository.deleteBriefingDossier(id)
    }
  }

  suspend fun generatePromptExport(targetLlm: String, userTask: String): String {
    return repository.generateContextPrompt(targetLlm, userTask)
  }

  // VoiceStudio Local Engines & Telemetry
  fun setVoiceStudioEngine(engine: String) {
    _voiceStudioEngine.value = engine
  }

  fun setSelectedASREngine(engine: String) {
    _selectedASREngine.value = engine
  }

  fun toggleVoiceprintEnrollment() {
    _voiceprintEnrolled.value = !_voiceprintEnrolled.value
  }

  fun togglePendantConnection() {
    _pendantConnected.value = !_pendantConnected.value
  }

  fun purgeAudioForNote(id: String) {
    viewModelScope.launch {
      val note = repository.allNotes.first().firstOrNull { it.id == id }
      if (note != null) {
        val updated = note.copy(audioPurged = true)
        repository.insertNote(updated)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    textToSpeechHelper.destroy()
    speechDictationManager.destroy()
    liveAudioStreamManager.release()
    geminiLiveClient.disconnect()
  }
}

private data class NavFilterQuad(
  val tab: Int,
  val q: String,
  val f: EntryType?,
  val tf: String?
)

private data class RecordingSubState(
  val sec: Int,
  val micAmp: Float,
  val micErr: String?,
  val ext: Boolean
)

private data class NavSearchState(
  val activeTab: Int,
  val searchQuery: String,
  val selectedTypeFilter: EntryType?,
  val selectedTagFilter: String?,
  val isLogModalOpen: Boolean,
  val isRagModalOpen: Boolean,
  val themeMode: ThemeMode,
  val prefillLogText: String = "",
  val prefillMatterCode: String = "",
  val prefillEventTitle: String = "",
  val autoStartRecordingInModal: Boolean = false,
  val meetingAlertsEnabled: Boolean = true,
  val fontScale: Float = 1.0f,
  val highContrastMode: Boolean = false
)

private data class RagIngestState(
  val ragQuery: String,
  val ragAnswer: String?,
  val ragCitations: List<GroundedCitation>,
  val isRagLoading: Boolean,
  val isLoggingInProgress: Boolean,
  val calendarSync: Boolean,
  val autoSync: Boolean,
  val progress: Float,
  val statusText: String
)

private data class DbExtras(
  val liveChunkCount: Int,
  val calendarEvents: List<com.example.data.local.CalendarEventEntity>,
  val tokenMetrics: List<TokenUsageEntity>,
  val actionItems: List<ActionItemEntity>,
  val briefingDossiers: List<BriefingDossierEntity>,
  val episodicMemories: List<com.example.data.local.EpisodicMemoryEntity>
)

private data class DbState(
  val allNotes: List<ContextNoteEntity>,
  val allMatters: List<MatterEntity>,
  val allRollups: List<TwoHourRollupEntity>,
  val allDocs: List<DocumentEntity>,
  val liveChunkCount: Int,
  val calendarEvents: List<com.example.data.local.CalendarEventEntity> = emptyList(),
  val tokenMetrics: List<TokenUsageEntity> = emptyList(),
  val actionItems: List<ActionItemEntity> = emptyList(),
  val briefingDossiers: List<BriefingDossierEntity> = emptyList(),
  val episodicMemories: List<com.example.data.local.EpisodicMemoryEntity> = emptyList()
)

private data class BackupCombinedState(
  val frequency: BackupFrequency,
  val lastTime: Long,
  val status: String,
  val isBackingUp: Boolean,
  val metadata: BackupMetadata?
)

private data class AuthUiState(
  val authState: AuthState,
  val isAuthLoading: Boolean,
  val authError: String?,
  val lastEventId: String?,
  val backupState: BackupCombinedState
)

private data class WisprSub1(
  val input: String,
  val ctx: WisprContextType,
  val tone: WisprTone,
  val lang: String,
  val res: WisprFlowResult?
)

private data class WisprSub2(
  val processing: Boolean,
  val actTransform: WisprTransform?,
  val resTransform: String?,
  val loadTransform: Boolean,
  val recording: Boolean
)

private data class WisprSub3(
  val recSeconds: Int,
  val micAmp: Float,
  val micErr: String?,
  val extendedMode: Boolean,
  val waveformLevels: FloatArray = floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
  val announcement: String? = null,
  val dictLanguage: String = "en-US",
  val isSpeaking: Boolean = false,
  val speechRate: Float = 1.0f
)

private data class WisprCombinedState(
  val sub1: WisprSub1,
  val sub2: WisprSub2,
  val sub3: WisprSub3,
  val dict: List<DictionaryItemEntity>,
  val snips: List<SnippetEntity>
)
