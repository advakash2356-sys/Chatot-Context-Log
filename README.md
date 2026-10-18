# Chatot-Context-Log

Chatot Context voice king — a voice-first episodic memory, context logging, and matter management Android application by Adv. Akash Sharma.

## Features

- **Voice & Audio Processing:** Live audio stream handling, media recording, speech dictation, audio playback, tactile waveform visualization, and text-to-speech support.
- **Multimodal AI & Memory Engines:** Gemini Multimodal Live client, Gemini service integration, Voice Flow Engine, Episodic Memory Ingestion Engine, Spatial Context Helper, and Memory Exploration Guide.
- **Document & Retrieval Management:** Document chunking, note embeddings, context note full-text search (FTS), RAG search modal, and source-grounded answering.
- **Local Data & Matter Tracking:** Room/SQLite database with entities for matters, context notes, action items, episodic memories, document chunks, dictionary snippets, and two-hour rollups.
- **Calendar & Notifications:** Device calendar and Google Calendar integration, calendar synchronization workers, boot receiver, and calendar alarm/notification scheduling.
- **Token Billing & Usage:** Token billing service, token usage tracking, and visualizers for billing and token expenditure rollups.
- **Data Synchronization:** Cloud backup manager and background sync workers.
- **User Interface:** Screens and tabs for Dashboard, Voice-First Capture, Capture, Logs, Tasks, Vault, Memories, Episodic Memory, Flow Studio, Billing, Settings, and Ask Query.

## Tech Stack

- **Language & Platform:** Kotlin, Android
- **Build System:** Gradle (Kotlin DSL: `build.gradle.kts`)
- **AI / Multimodal:** Google Gemini (`GeminiService`, `GeminiMultimodalLiveClient`)
- **Persistence:** Local database with DAO (`ContextLogDao`, `AppDatabase`), Room/SQLite entities, and FTS
- **Background Tasks & Sync:** Android WorkManager (`ContextLogSyncWorker`, `GoogleCalendarSyncWorker`), BroadcastReceivers
- **Audio & Media:** Android MediaRecorder, Speech Dictation, Audio Stream & Playback APIs
- **Integrations:** Google Calendar, Device Calendar

## Setup

Environment configuration is managed via `.env.example` in the project root. Specific build commands and prerequisite toolchain versions are not documented in the repository; build and run the application using a compatible Android development environment (such as Android Studio with Gradle).

## Usage

- **Voice & Context Capture:** Access `VoiceFirstCaptureScreen` or `CaptureScreen` to record dictations, stream live audio, and generate context notes.
- **Matter & Task Management:** Organize legal matters, context notes, and action items via the `LogsTab`, `TasksTab`, and `VaultScreen`.
- **Querying & Memory Exploration:** Search context logs, explore episodic memories, and perform grounded RAG queries in `AskQueryTab` and `MemoryExplorationGuideModal`.
- **Calendar Synchronization:** Sync schedules and alarms across device and Google Calendar services.
- **Billing & Token Tracking:** Monitor token consumption and expenditure analytics through `BillingTab`.

## Project Structure

```
Chatot-Context-Log/
├── .env.example
├── .gitignore
└── app/
    ├── .gitignore
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── androidTest/
        │   └── java/com/example/ExampleInstrumentedTest.kt
        └── main/
            ├── AndroidManifest.xml
            └── java/com/example/
                ├── ContextLogApplication.kt
                ├── MainActivity.kt
                ├── data/
                │   ├── ai/
                │   │   ├── DocumentChunker.kt
                │   │   ├── EpisodicMemoryIngestionEngine.kt
                │   │   ├── GeminiMultimodalLiveClient.kt
                │   │   ├── GeminiService.kt
                │   │   ├── MemoryExplorationGuideEngine.kt
                │   │   ├── SpatialContextHelper.kt
                │   │   ├── VoiceFlowEngine.kt
                │   │   └── VoiceFlowModels.kt
                │   ├── audio/
                │   │   ├── AudioCaptureState.kt
                │   │   ├── AudioPlaybackManager.kt
                │   │   ├── LiveAudioStreamManager.kt
                │   │   ├── MediaRecorderManager.kt
                │   │   ├── SpeechDictationManager.kt
                │   │   └── TextToSpeechHelper.kt
                │   ├── auth/
                │   │   └── AuthManager.kt
                │   ├── billing/
                │   │   └── TokenBillingService.kt
                │   ├── calendar/
                │   │   ├── DeviceCalendarManager.kt
                │   │   └── GoogleCalendarService.kt
                │   ├── local/
                │   │   ├── ActionItemEntity.kt
                │   │   ├── AppDatabase.kt
                │   │   ├── CalendarEventEntity.kt
                │   │   ├── ContextLogDao.kt
                │   │   ├── ContextNoteEntity.kt
                │   │   ├── ContextNoteFtsEntity.kt
                │   │   ├── DictionaryAndSnippets.kt
                │   │   ├── DocumentChunkEntity.kt
                │   │   ├── EpisodicMemoryEntity.kt
                │   │   ├── MatterEntity.kt
                │   │   ├── NoteEmbeddingEntity.kt
                │   │   ├── TokenUsageEntity.kt
                │   │   └── TwoHourRollupEntity.kt
                │   ├── repository/
                │   │   └── ContextLogRepository.kt
                │   └── sync/
                │       ├── CloudBackupManager.kt
                │       ├── ContextLogSyncWorker.kt
                │       └── GoogleCalendarSyncWorker.kt
                ├── notification/
                │   ├── BootCompletedReceiver.kt
                │   ├── CalendarAlarmReceiver.kt
                │   └── CalendarNotificationScheduler.kt
                ├── ui/
                │   ├── components/
                │   │   ├── Ambient3DBackground.kt
                │   │   ├── BillingRollupVisualizer.kt
                │   │   ├── BottomNav.kt
                │   │   ├── DocumentIngestionSheet.kt
                │   │   ├── FastTactileWaveform.kt
                │   │   ├── HoveringDictationAssistant.kt
                │   │   ├── IndexingStatusBanner.kt
                │   │   ├── LogEntryModal.kt
                │   │   ├── NoteCard.kt
                │   │   ├── RagSearchModal.kt
                │   │   ├── SourceGroundedAnswer.kt
                │   │   ├── Spatial3D.kt
                │   │   ├── TactileRecordButton.kt
                │   │   ├── TodayRollupCard.kt
                │   │   ├── TokenExpenditureVisualizer.kt
                │   │   └── TopHeader.kt
                │   ├── screens/
                │   │   ├── AskQueryTab.kt
                │   │   ├── BillingTab.kt
                │   │   ├── CaptureScreen.kt
                │   │   ├── DashboardTab.kt
                │   │   ├── EpisodicMemoryScreen.kt
                │   │   ├── FlowStudioTab.kt
                │   │   ├── LogsTab.kt
                │   │   ├── MemoriesTab.kt
                │   │   ├── MemoryExplorationGuideModal.kt
                │   │   ├── SettingsTab.kt
                │   │   ├── TasksTab.kt
                │   │   ├── VaultScreen.kt
                │   │   └── VoiceFirstCaptureScreen.kt
                │   ├── theme/
                │   │   ├── Color.kt
                │   │   ├── Theme.kt
                │   │   └── Type.kt
                │   └── viewmodel/
                │       └── ContextLogViewModel.kt
