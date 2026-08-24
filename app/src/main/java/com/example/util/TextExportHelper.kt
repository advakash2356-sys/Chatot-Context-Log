package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ai.StructuredMeetingNotes
import com.example.data.ai.WisprFlowResult
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextNoteEntity
import com.example.data.local.TwoHourRollupEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TextExportHelper {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Formats structured meeting notes into a clean, professional Markdown or Plain Text document.
     */
    fun formatMeetingMinutesText(
        meetingNotes: StructuredMeetingNotes?,
        chatotResult: WisprFlowResult?,
        toneName: String = "Concise"
    ): String {
        val nowStr = dateFormatter.format(Date())
        val sb = StringBuilder()

        val title = meetingNotes?.title ?: "Meeting Minutes & Executive Summary"
        sb.appendLine("==================================================")
        sb.appendLine(title.uppercase())
        sb.appendLine("==================================================")
        sb.appendLine("Generated: $nowStr")
        sb.appendLine("Engine: Voice Intelligence (Gemini 3.5 Flash)")
        sb.appendLine("Tone Directive: $toneName")
        if (chatotResult != null) {
            sb.appendLine("Latency: ${chatotResult.latencyMs}ms | Original Words: ${chatotResult.rawTranscript.split("\\s+".toRegex()).size}")
        }
        sb.appendLine("--------------------------------------------------")
        sb.appendLine()

        sb.appendLine("1. EXECUTIVE SUMMARY")
        sb.appendLine("-------------------")
        val summary = meetingNotes?.executiveSummary ?: chatotResult?.cleanText ?: "No summary available."
        sb.appendLine(summary)
        sb.appendLine()

        if (meetingNotes != null && meetingNotes.actionItems.isNotEmpty()) {
            sb.appendLine("2. ACTION ITEMS & DELIVERABLES")
            sb.appendLine("-----------------------------")
            meetingNotes.actionItems.forEachIndexed { idx, item ->
                sb.appendLine("[ ] ${idx + 1}. $item")
            }
            sb.appendLine()
        }

        if (meetingNotes != null && meetingNotes.keyDecisions.isNotEmpty()) {
            sb.appendLine("3. KEY DECISIONS & RESOLUTIONS")
            sb.appendLine("------------------------------")
            meetingNotes.keyDecisions.forEachIndexed { idx, decision ->
                sb.appendLine("• $decision")
            }
            sb.appendLine()
        }

        if (chatotResult != null && chatotResult.rawTranscript.isNotBlank()) {
            sb.appendLine("4. ORIGINAL VOICE DICTATION TRANSCRIPT")
            sb.appendLine("--------------------------------------")
            sb.appendLine(chatotResult.rawTranscript)
            sb.appendLine()
        }

        sb.appendLine("==================================================")
        sb.appendLine("CONFIDENTIAL & PRIVILEGED — ContextLog AI System")
        sb.appendLine("==================================================")

        return sb.toString()
    }

    /**
     * Formats all or filtered context logs into an exportable report text.
     */
    fun formatContextLogsReportText(
        notes: List<ContextNoteEntity>,
        filterLabel: String = "All Logs",
        billingRollups: List<TwoHourRollupEntity> = emptyList(),
        calendarEvents: List<CalendarEventEntity> = emptyList()
    ): String {
        val nowStr = dateFormatter.format(Date())
        val sb = StringBuilder()

        sb.appendLine("==================================================")
        sb.appendLine("CONTEXTLOG AI — COMPREHENSIVE ACTIVITY & CONTEXT REPORT")
        sb.appendLine("==================================================")
        sb.appendLine("Export Date: $nowStr")
        sb.appendLine("Scope: $filterLabel (${notes.size} entries)")
        sb.appendLine("Storage Backend: Local Room DB (Zero-Latency)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine()

        if (calendarEvents.isNotEmpty()) {
            sb.appendLine("SYNCED GOOGLE CALENDAR SESSIONS (${calendarEvents.size})")
            sb.appendLine("==================================================")
            calendarEvents.forEach { event ->
                val start = dateFormatter.format(Date(event.startTime))
                val end = dateFormatter.format(Date(event.endTime))
                sb.appendLine("[${event.matterCode}] ${event.title}")
                sb.appendLine("  Time: $start -> $end")
                if (event.location.isNotBlank()) sb.appendLine("  Location: ${event.location}")
                if (event.description.isNotBlank()) sb.appendLine("  Notes: ${event.description}")
                sb.appendLine()
            }
            sb.appendLine("--------------------------------------------------")
            sb.appendLine()
        }

        if (billingRollups.isNotEmpty()) {
            sb.appendLine("TWO-HOUR BILLING ROLLUP SUMMARIES (${billingRollups.size})")
            sb.appendLine("==================================================")
            billingRollups.forEach { rollup ->
                val blockDate = dateFormatter.format(Date(rollup.twoHourBlockStart))
                sb.appendLine("[$blockDate] Matter: ${rollup.matterCode} | Hours: ${rollup.estimatedHours}")
                sb.appendLine("Executive Summary: ${rollup.executiveSummary}")
                if (rollup.formattedBillableText.isNotBlank()) {
                    sb.appendLine("Billable Narrative: ${rollup.formattedBillableText}")
                }
                sb.appendLine()
            }
            sb.appendLine("--------------------------------------------------")
            sb.appendLine()
        }

        sb.appendLine("DETAILED CONTEXT LOGS (${notes.size})")
        sb.appendLine("==================================================")
        notes.forEachIndexed { idx, note ->
            val recordedStr = dateFormatter.format(Date(note.recordedAt))
            sb.appendLine("${idx + 1}. [${note.matterCode}] ${note.entryType.name} (Level ${note.depthLevel})")
            sb.appendLine("   Recorded: $recordedStr | Synced GCal: ${if (note.syncedToCalendar) "YES" else "NO"}")
            if (note.tagList.isNotEmpty()) {
                sb.appendLine("   Tags: ${note.tagList.joinToString(" ") { "#$it" }}")
            }
            sb.appendLine("   Clean Content:")
            sb.appendLine("   ${note.cleanText.replace("\n", "\n   ")}")
            if (note.rawTranscript.isNotBlank() && note.rawTranscript != note.cleanText) {
                sb.appendLine("   Raw Voice Transcript: ${note.rawTranscript}")
            }
            sb.appendLine()
        }

        sb.appendLine("==================================================")
        sb.appendLine("END OF CONTEXTLOG REPORT")
        sb.appendLine("==================================================")

        return sb.toString()
    }

    /**
     * Exports text content to a local file in the app's cache/exports directory and opens the share chooser.
     */
    fun exportAndShareTextFile(
        context: Context,
        content: String,
        fileNamePrefix: String = "contextlog_export"
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val fileName = "${fileNamePrefix}_${fileDateFormatter.format(Date())}.txt"
            val file = File(exportDir, fileName)

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            val fileUri: Uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                // Fallback to standard share intent if FileProvider is unavailable
                null
            } ?: run {
                sharePlainText(context, content, "Share $fileNamePrefix")
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export $fileName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "Exported $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            sharePlainText(context, content, "Share $fileNamePrefix")
        }
    }

    /**
     * Fallback share plain text directly.
     */
    fun sharePlainText(context: Context, content: String, title: String = "Share") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
