package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class LiveConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_READY,
    STREAMING_AUDIO,
    INTERRUPTED,
    ERROR
}

data class LiveMetrics(
    val timeToFirstChunkMs: Long = 0L,
    val totalAudioChunksSent: Long = 0L,
    val totalAudioChunksReceived: Long = 0L,
    val totalToolCallsHandled: Int = 0,
    val activeVoiceName: String = "Aoede",
    val modelName: String = "gemini-2.0-flash-exp"
)

data class LiveTranscriptSnippet(
    val sender: String,
    val text: String,
    val isRealtime: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * GeminiMultimodalLiveClient
 * 
 * Implements Phase 4: Gemini Multimodal Live API Integration Pipeline.
 * Bidirectional stateful WebSocket audio streaming with `gemini-2.0-flash-exp`.
 * 
 * - Audio In: 16kHz PCM Mono Little-Endian
 * - Audio Out: 24kHz PCM Mono Little-Endian
 * - Latency: 300ms–500ms time-to-first-chunk tracking
 * - Native Interruption (Barge-in) handling
 * - Live Function Calling: `retrieve_memories` and `update_memory_node`
 */
class GeminiMultimodalLiveClient(
    customApiKey: String? = null
) {
    private val configuredKey: String? = customApiKey
    private val apiKey: String
        get() {
            if (!configuredKey.isNullOrBlank() && configuredKey != "MY_GEMINI_API_KEY" && !configuredKey.startsWith("YOUR_")) {
                return configuredKey
            }
            return try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")) key else ""
            } catch (e: Exception) {
                ""
            }
        }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for streaming WebSocket
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var scope: CoroutineScope? = null

    private val _connectionStatus = MutableStateFlow(LiveConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<LiveConnectionStatus> = _connectionStatus.asStateFlow()

    private val _liveMetrics = MutableStateFlow(LiveMetrics())
    val liveMetrics: StateFlow<LiveMetrics> = _liveMetrics.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to connect")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _liveTranscripts = MutableStateFlow<List<LiveTranscriptSnippet>>(emptyList())
    val liveTranscripts: StateFlow<List<LiveTranscriptSnippet>> = _liveTranscripts.asStateFlow()

    private val _receivedAudioPcm = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val receivedAudioPcm: SharedFlow<ByteArray> = _receivedAudioPcm.asSharedFlow()

    private val _interruptionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val interruptionEvents: SharedFlow<Unit> = _interruptionEvents.asSharedFlow()

    private val _toolExecutions = MutableSharedFlow<ToolCallInfo>(extraBufferCapacity = 16)
    val toolExecutions: SharedFlow<ToolCallInfo> = _toolExecutions.asSharedFlow()

    private var lastUserAudioSentTime: Long = 0L
    private var isFirstChunkInTurn = true
    private var selectedVoice: String = "Aoede"

    var onToolCallDispatcher: (suspend (name: String, args: JSONObject) -> JSONObject)? = null

    companion object {
        const val LIVE_WS_ENDPOINT = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        const val LIVE_MODEL = "models/gemini-2.0-flash-exp"

        val AVAILABLE_VOICES = listOf("Aoede", "Puck", "Charon", "Fenrir", "Kore")

        const val LIVE_SYSTEM_INSTRUCTION = """You are a warm, perceptive Memory Exploration Guide embedded inside a voice-first personal recording application.
You speak naturally over audio in 1 to 3 short sentences per turn.
Ask Socratic sensory questions about memories (sounds, weather, presence, atmosphere).
Strictly avoid markdown, bullet points, numbers, or clinical language.
Use retrieve_memories to fetch context when people, places, or past events are mentioned.
Weave retrieved context into conversation seamlessly without referencing database records or tools."""
    }

    fun setVoice(voiceName: String) {
        if (voiceName in AVAILABLE_VOICES) {
            selectedVoice = voiceName
            _liveMetrics.value = _liveMetrics.value.copy(activeVoiceName = voiceName)
        }
    }

    /**
     * Connects to the Gemini Multimodal Live API WebSocket session.
     */
    fun connectLiveSession(
        coroutineScope: CoroutineScope,
        memoryContextSummary: String? = null
    ) {
        if (_connectionStatus.value == LiveConnectionStatus.CONNECTED_READY ||
            _connectionStatus.value == LiveConnectionStatus.STREAMING_AUDIO) {
            return
        }

        scope = coroutineScope
        _connectionStatus.value = LiveConnectionStatus.CONNECTING
        _statusMessage.value = "Connecting to Gemini 2.0 Flash Live API..."

        if (apiKey.isBlank()) {
            // Offline/Dev Fallback Mode
            Log.w("GeminiLiveClient", "No Gemini API key available. Entering Simulated Multimodal Live mode.")
            _connectionStatus.value = LiveConnectionStatus.CONNECTED_READY
            _statusMessage.value = "Connected (Simulated Multimodal Live Pipeline)"
            return
        }

        try {
            val url = "$LIVE_WS_ENDPOINT?key=$apiKey"
            val request = Request.Builder().url(url).build()

            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.i("GeminiLiveClient", "Multimodal Live WebSocket Connected!")
                    _connectionStatus.value = LiveConnectionStatus.CONNECTED_READY
                    _statusMessage.value = "Connected to Gemini Live (2.0 Flash Audio Pipeline)"
                    sendSetupHandshake(ws, memoryContextSummary)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleIncomingServerMessage(text)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    Log.i("GeminiLiveClient", "WebSocket Closing: $code / $reason")
                    _connectionStatus.value = LiveConnectionStatus.DISCONNECTED
                    _statusMessage.value = "Disconnected ($reason)"
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e("GeminiLiveClient", "WebSocket Error", t)
                    _connectionStatus.value = LiveConnectionStatus.ERROR
                    _statusMessage.value = "Connection error: ${t.localizedMessage ?: "Unknown error"}"
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _connectionStatus.value = LiveConnectionStatus.DISCONNECTED
                    _statusMessage.value = "Session closed"
                }
            })
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Failed to open Live WebSocket", e)
            _connectionStatus.value = LiveConnectionStatus.ERROR
            _statusMessage.value = "Initialization failed: ${e.message}"
        }
    }

    /**
     * Sends the initial Setup packet to configure audio modalities, voice, system instruction, and tools.
     */
    private fun sendSetupHandshake(ws: WebSocket, memoryContextSummary: String?) {
        try {
            val setupObj = JSONObject()
            val setupDetails = JSONObject()

            setupDetails.put("model", LIVE_MODEL)

            // Generation Config: Audio Modality & Voice Selection
            val genConfig = JSONObject()
            genConfig.put("responseModalities", JSONArray().put("AUDIO"))
            
            val speechConfig = JSONObject().apply {
                put("voiceConfig", JSONObject().apply {
                    put("prebuiltVoiceConfig", JSONObject().apply {
                        put("voiceName", selectedVoice)
                    })
                })
            }
            genConfig.put("speechConfig", speechConfig)
            setupDetails.put("generationConfig", genConfig)

            // System Instruction with memory context
            val fullInstruction = buildString {
                append(LIVE_SYSTEM_INSTRUCTION)
                if (!memoryContextSummary.isNullOrBlank()) {
                    append("\n\nCURRENT USER MEMORY CONTEXT:\n$memoryContextSummary")
                }
            }

            val sysInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", fullInstruction))
                })
            }
            setupDetails.put("systemInstruction", sysInstructionObj)

            // Function Declarations (Tools)
            val toolsArray = JSONArray()
            val funcDeclarations = JSONArray()

            // Tool 1: retrieve_memories
            val retrieveTool = JSONObject().apply {
                put("name", "retrieve_memories")
                put("description", "Fetches archived personal memories, past recordings, and photos based on semantic queries and entities.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Search query or entity names")
                        })
                        put("timeframe", JSONObject().apply {
                            put("type", "string")
                            put("description", "Optional era or year filter")
                        })
                        put("entity_filter", JSONObject().apply {
                            put("type", "string")
                            put("description", "Optional entity name filter")
                        })
                    })
                    put("required", JSONArray().apply { put("query") })
                })
            }
            funcDeclarations.put(retrieveTool)

            // Tool 2: update_memory_node
            val updateTool = JSONObject().apply {
                put("name", "update_memory_node")
                put("description", "Saves newly clarified facts or emotional insights back to the permanent memory archive.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("memory_id", JSONObject().apply {
                            put("type", "string")
                            put("description", "Target memory ID")
                        })
                        put("resolved_gaps", JSONObject().apply {
                            put("type", "array")
                            put("items", JSONObject().put("type", "string"))
                            put("description", "Clarified details")
                        })
                        put("new_insights", JSONObject().apply {
                            put("type", "string")
                            put("description", "New insight shared by user")
                        })
                    })
                    put("required", JSONArray().apply {
                        put("memory_id")
                        put("new_insights")
                    })
                })
            }
            funcDeclarations.put(updateTool)

            toolsArray.put(JSONObject().put("functionDeclarations", funcDeclarations))
            setupDetails.put("tools", toolsArray)

            setupObj.put("setup", setupDetails)

            val jsonString = setupObj.toString()
            Log.d("GeminiLiveClient", "Sending Setup Packet to Live API: $jsonString")
            ws.send(jsonString)
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error sending setup handshake", e)
        }
    }

    /**
     * Sends a 16kHz PCM audio chunk from microphone to Gemini Live session.
     */
    fun sendMicrophonePcmChunk(pcm16k: ByteArray) {
        if (pcm16k.isEmpty()) return

        lastUserAudioSentTime = System.currentTimeMillis()
        isFirstChunkInTurn = true

        val base64Data = Base64.encodeToString(pcm16k, Base64.NO_WRAP)

        _liveMetrics.value = _liveMetrics.value.copy(
            totalAudioChunksSent = _liveMetrics.value.totalAudioChunksSent + 1
        )

        if (webSocket == null || _connectionStatus.value != LiveConnectionStatus.CONNECTED_READY && _connectionStatus.value != LiveConnectionStatus.STREAMING_AUDIO) {
            return
        }

        try {
            val realtimeObj = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        })
                    })
                })
            }
            webSocket?.send(realtimeObj.toString())
            _connectionStatus.value = LiveConnectionStatus.STREAMING_AUDIO
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error sending mic chunk", e)
        }
    }

    /**
     * Handles incoming messages from the Gemini Live WebSocket.
     */
    private fun handleIncomingServerMessage(rawJson: String) {
        try {
            val root = JSONObject(rawJson)

            // 1. Server Content (Audio parts, transcripts, interruptions)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                // Check for Interruption (Barge-in)
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.i("GeminiLiveClient", "Gemini signaled user interruption (Barge-in)!")
                    _connectionStatus.value = LiveConnectionStatus.INTERRUPTED
                    scope?.launch {
                        _interruptionEvents.emit(Unit)
                    }
                }

                // Check for Model Turn Audio Parts
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Audio PCM chunk (24kHz)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.optString("data", "")
                                if (base64Audio.isNotBlank()) {
                                    val decodedBytes = Base64.decode(base64Audio, Base64.DEFAULT)

                                    if (isFirstChunkInTurn && lastUserAudioSentTime > 0) {
                                        val latency = System.currentTimeMillis() - lastUserAudioSentTime
                                        _liveMetrics.value = _liveMetrics.value.copy(
                                            timeToFirstChunkMs = latency.coerceAtLeast(180L)
                                        )
                                        isFirstChunkInTurn = false
                                    }

                                    _liveMetrics.value = _liveMetrics.value.copy(
                                        totalAudioChunksReceived = _liveMetrics.value.totalAudioChunksReceived + 1
                                    )

                                    scope?.launch {
                                        _receivedAudioPcm.emit(decodedBytes)
                                    }
                                }
                            }

                            // Text transcripts if returned alongside audio
                            if (part.has("text")) {
                                val text = part.optString("text", "")
                                if (text.isNotBlank()) {
                                    addTranscriptSnippet("Guide", text)
                                }
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    _connectionStatus.value = LiveConnectionStatus.CONNECTED_READY
                }
            }

            // 2. Real-Time Tool Calls (Function Calls)
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id")
                        val funcName = call.optString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        Log.i("GeminiLiveClient", "Live API Tool Call: $funcName with args $args")
                        handleLiveToolCall(callId, funcName, args)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error parsing server message", e)
        }
    }

    /**
     * Executes tool call and returns toolResponse back into the live WebSocket session.
     */
    private fun handleLiveToolCall(callId: String, functionName: String, args: JSONObject) {
        scope?.launch(Dispatchers.IO) {
            val resultObj = onToolCallDispatcher?.invoke(functionName, args) ?: JSONObject().apply {
                put("status", "success")
                put("message", "Executed $functionName")
            }

            val summary = if (functionName == "retrieve_memories") {
                "Query: '${args.optString("query")}'"
            } else {
                "Updated Node: '${args.optString("memory_id")}'"
            }

            val toolInfo = ToolCallInfo(
                toolName = functionName,
                argsJson = args.toString(),
                resultSummary = summary
            )
            _toolExecutions.emit(toolInfo)

            _liveMetrics.value = _liveMetrics.value.copy(
                totalToolCallsHandled = _liveMetrics.value.totalToolCallsHandled + 1
            )

            // Format LiveClientToolResponse
            val responseObj = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", callId)
                            put("name", functionName)
                            put("response", JSONObject().apply {
                                put("result", resultObj)
                            })
                        })
                    })
                })
            }

            try {
                webSocket?.send(responseObj.toString())
                Log.d("GeminiLiveClient", "Sent Tool Response for $functionName")
            } catch (e: Exception) {
                Log.e("GeminiLiveClient", "Error sending tool response over WebSocket", e)
            }
        }
    }

    fun addTranscriptSnippet(sender: String, text: String) {
        val snippet = LiveTranscriptSnippet(sender = sender, text = text)
        _liveTranscripts.value = (_liveTranscripts.value + snippet).takeLast(20)
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Session ended by user")
            webSocket = null
        } catch (e: Exception) {
            Log.w("GeminiLiveClient", "Error closing WebSocket", e)
        }
        _connectionStatus.value = LiveConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected"
    }
}
