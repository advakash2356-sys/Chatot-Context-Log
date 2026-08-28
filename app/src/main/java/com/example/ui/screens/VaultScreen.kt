package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActionItemEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextNoteEntity
import com.example.data.local.TwoHourRollupEntity
import com.example.ui.components.FloatingDictationAssistant
import com.example.ui.components.HoveringDictationButton
import com.example.ui.theme.ActiveAccent
import com.example.ui.theme.ActiveAccentSubtle
import com.example.ui.theme.ActiveDestructive
import com.example.ui.theme.ActiveSuccess
import com.example.ui.theme.MonoBackground
import com.example.ui.theme.MonoBorder
import com.example.ui.theme.MonoBorderSubtle
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoTextMuted
import com.example.ui.theme.MonoTextPrimary
import com.example.ui.theme.MonoTextSecondary
import com.example.ui.theme.MonoWhite
import com.example.ui.viewmodel.MainAppView
import com.example.ui.viewmodel.VaultFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
  notes: List<ContextNoteEntity>,
  actionItems: List<ActionItemEntity>,
  calendarEvents: List<CalendarEventEntity>,
  rollups: List<TwoHourRollupEntity> = emptyList(),
  episodicItemCount: Int = 0,
  currentFilter: VaultFilter,
  searchQuery: String,
  inlineDictationTarget: String? = null,
  onFilterSelect: (VaultFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onToggleTask: (String, Boolean) -> Unit,
  onDeleteNote: (String) -> Unit,
  onDeleteTask: (String) -> Unit,
  onDeleteCalendarEvent: (String) -> Unit,
  onGenerateReport: () -> Unit = {},
  onNavigateToCapture: () -> Unit,
  onNavigateToEpisodic: () -> Unit = {},
  onStartInlineDictation: (String, String, (String) -> Unit) -> Unit = { _, _, _ -> },
  onStopInlineDictation: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedNoteForDetail by remember { mutableStateOf<ContextNoteEntity?>(null) }

  // Filter notes by search query
  val filteredNotes = notes.filter { note ->
    searchQuery.isBlank() ||
      note.title.contains(searchQuery, ignoreCase = true) ||
      note.cleanText.contains(searchQuery, ignoreCase = true) ||
      note.executiveSummary.contains(searchQuery, ignoreCase = true) ||
      note.structuredNotes.contains(searchQuery, ignoreCase = true)
  }

  // Filter tasks by search query
  val filteredTasks = actionItems.filter { task ->
    searchQuery.isBlank() ||
      task.title.contains(searchQuery, ignoreCase = true) ||
      task.owner.contains(searchQuery, ignoreCase = true)
  }

  // Filter calendar events by search query
  val filteredCalendar = calendarEvents.filter { event ->
    searchQuery.isBlank() ||
      event.title.contains(searchQuery, ignoreCase = true) ||
      (event.location?.contains(searchQuery, ignoreCase = true) == true)
  }

  // Filter reports by search query
  val filteredRollups = rollups.filter { rollup ->
    searchQuery.isBlank() ||
      rollup.executiveSummary.contains(searchQuery, ignoreCase = true) ||
      rollup.formattedBillableText.contains(searchQuery, ignoreCase = true) ||
      (rollup.matterCode?.contains(searchQuery, ignoreCase = true) == true)
  }

  val totalVaultCount = notes.size + actionItems.size + calendarEvents.size + rollups.size

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MonoBackground)
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      // Top Minimalist View Switcher [ CAPTURE | VAULT | EPISODIC ]
      MinimalistViewSwitcher(
        activeView = MainAppView.VAULT,
        vaultCount = totalVaultCount,
        episodicCount = episodicItemCount,
        onSelectCapture = onNavigateToCapture,
        onSelectVault = {},
        onSelectEpisodic = onNavigateToEpisodic
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Minimalist Search Bar with Hovering Dictate Icon
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MonoSurface)
          .border(
            1.dp,
            if (inlineDictationTarget == "vault_search") ActiveAccent else MonoBorder,
            RoundedCornerShape(10.dp)
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search Vault",
          tint = MonoTextMuted,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          textStyle = TextStyle(
            color = MonoTextPrimary,
            fontSize = 14.sp
          ),
          cursorBrush = SolidColor(ActiveAccent),
          modifier = Modifier
            .weight(1f)
            .testTag("vault_search_input"),
          decorationBox = { innerTextField ->
            if (searchQuery.isEmpty() && inlineDictationTarget != "vault_search") {
              Text(
                text = "Search notes, tasks, events, reports...",
                style = TextStyle(
                  color = MonoTextMuted,
                  fontSize = 14.sp
                )
              )
            }
            innerTextField()
          }
        )
        if (searchQuery.isNotEmpty()) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear Search",
            tint = MonoTextMuted,
            modifier = Modifier
              .size(18.dp)
              .clickable { onSearchQueryChange("") }
          )
          Spacer(modifier = Modifier.width(6.dp))
        }

        // Hovering Dictation Button in Search Bar
        HoveringDictationButton(
          isDictating = inlineDictationTarget == "vault_search",
          onToggleDictation = {
            if (inlineDictationTarget == "vault_search") {
              onStopInlineDictation()
            } else {
              onStartInlineDictation("vault_search", searchQuery) { updated ->
                onSearchQueryChange(updated)
              }
            }
          },
          compact = true,
          testTag = "vault_search_hover_dictate"
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Filter Chips Row [ All | Notes | Tasks | Calendar | Reports ]
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          VaultFilterChip(
            label = "ALL",
            count = totalVaultCount,
            isSelected = currentFilter == VaultFilter.ALL,
            onClick = { onFilterSelect(VaultFilter.ALL) },
            testTag = "filter_chip_all"
          )
        }
        item {
          VaultFilterChip(
            label = "NOTES",
            count = notes.size,
            isSelected = currentFilter == VaultFilter.NOTES,
            onClick = { onFilterSelect(VaultFilter.NOTES) },
            testTag = "filter_chip_notes"
          )
        }
        item {
          VaultFilterChip(
            label = "TASKS",
            count = actionItems.size,
            isSelected = currentFilter == VaultFilter.TASKS,
            onClick = { onFilterSelect(VaultFilter.TASKS) },
            testTag = "filter_chip_tasks"
          )
        }
        item {
          VaultFilterChip(
            label = "CALENDAR",
            count = calendarEvents.size,
            isSelected = currentFilter == VaultFilter.CALENDAR,
            onClick = { onFilterSelect(VaultFilter.CALENDAR) },
            testTag = "filter_chip_calendar"
          )
        }
        item {
          VaultFilterChip(
            label = "REPORTS",
            count = rollups.size,
            isSelected = currentFilter == VaultFilter.REPORTS,
            onClick = { onFilterSelect(VaultFilter.REPORTS) },
            testTag = "filter_chip_reports"
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Unified Content Feed
      val showNotes = currentFilter == VaultFilter.ALL || currentFilter == VaultFilter.NOTES
      val showTasks = currentFilter == VaultFilter.ALL || currentFilter == VaultFilter.TASKS
      val showCalendar = currentFilter == VaultFilter.ALL || currentFilter == VaultFilter.CALENDAR
      val showReports = currentFilter == VaultFilter.ALL || currentFilter == VaultFilter.REPORTS

      val hasAnyItems = (showNotes && filteredNotes.isNotEmpty()) ||
        (showTasks && filteredTasks.isNotEmpty()) ||
        (showCalendar && filteredCalendar.isNotEmpty()) ||
        (showReports && filteredRollups.isNotEmpty())

      if (!hasAnyItems) {
        // Minimalist Clean Empty State
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
          contentAlignment = Alignment.TopCenter
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MonoSurface)
                .border(1.dp, MonoBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (currentFilter) {
                  VaultFilter.REPORTS -> Icons.Default.Assessment
                  VaultFilter.TASKS -> Icons.Default.TaskAlt
                  VaultFilter.CALENDAR -> Icons.Default.CalendarToday
                  else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = MonoTextMuted,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = if (searchQuery.isNotBlank()) "No matching results" else when (currentFilter) {
                VaultFilter.REPORTS -> "No Reports Generated Yet"
                VaultFilter.TASKS -> "No Action Items Extracted"
                VaultFilter.CALENDAR -> "No Scheduled Events"
                VaultFilter.NOTES -> "No Notes in Memory"
                VaultFilter.ALL -> "Vault is Empty"
              },
              style = TextStyle(
                color = MonoWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
              )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = if (searchQuery.isNotBlank()) "Try changing your search terms." else when (currentFilter) {
                VaultFilter.REPORTS -> "Dictate or type notes to auto-generate executive work reports."
                else -> "Switch to Capture to record or type your thoughts."
              },
              style = TextStyle(
                color = MonoTextSecondary,
                fontSize = 13.sp
              ),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (currentFilter == VaultFilter.REPORTS && notes.isNotEmpty()) {
              Spacer(modifier = Modifier.height(20.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(MonoWhite)
                  .clickable { onGenerateReport() }
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("empty_state_generate_report_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Generate Executive Report",
                    style = TextStyle(
                      color = Color.Black,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            } else if (searchQuery.isBlank()) {
              Spacer(modifier = Modifier.height(24.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(MonoWhite)
                  .clickable { onNavigateToCapture() }
                  .padding(horizontal = 16.dp, vertical = 10.dp)
                  .testTag("empty_state_capture_button"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Capture Thought",
                  style = TextStyle(
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                )
              }
            }
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 32.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          // Section: Reports & Rollups
          if (showReports && filteredRollups.isNotEmpty()) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                VaultSectionHeader(title = "EXECUTIVE WORK REPORTS (${filteredRollups.size})")
                Text(
                  text = "+ Generate",
                  style = TextStyle(
                    color = ActiveAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  modifier = Modifier
                    .clickable { onGenerateReport() }
                    .padding(4.dp)
                )
              }
            }
            items(filteredRollups, key = { it.id }) { rollup ->
              VaultReportItem(
                rollup = rollup,
                onCopy = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Executive Report", "${rollup.executiveSummary}\n\n${rollup.formattedBillableText}")
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                }
              )
            }
          }

          // Section: Tasks
          if (showTasks && filteredTasks.isNotEmpty()) {
            item {
              VaultSectionHeader(title = "TASKS & ACTION ITEMS (${filteredTasks.size})")
            }
            items(filteredTasks, key = { it.id }) { task ->
              VaultTaskItem(
                task = task,
                onToggle = { isDone -> onToggleTask(task.id, isDone) },
                onDelete = { onDeleteTask(task.id) }
              )
            }
          }

          // Section: Calendar Events
          if (showCalendar && filteredCalendar.isNotEmpty()) {
            item {
              VaultSectionHeader(title = "CALENDAR EVENTS (${filteredCalendar.size})")
            }
            items(filteredCalendar, key = { it.id }) { event ->
              VaultCalendarItem(
                event = event,
                onDelete = { onDeleteCalendarEvent(event.id) }
              )
            }
          }

          // Section: Notes
          if (showNotes && filteredNotes.isNotEmpty()) {
            item {
              VaultSectionHeader(title = "SYNTHESIZED NOTES & MEMORY (${filteredNotes.size})")
            }
            items(filteredNotes, key = { it.id }) { note ->
              VaultNoteItem(
                note = note,
                onClick = { selectedNoteForDetail = note },
                onDelete = { onDeleteNote(note.id) },
                onCopy = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Note", "${note.title}\n\n${note.executiveSummary}")
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
              )
            }
          }
        }
      }
    }

    // Floating assistant when searching or typing
    if (searchQuery.isNotBlank() || inlineDictationTarget == "vault_search") {
      FloatingDictationAssistant(
        isVisible = true,
        isDictating = inlineDictationTarget == "vault_search",
        onToggleDictation = {
          if (inlineDictationTarget == "vault_search") {
            onStopInlineDictation()
          } else {
            onStartInlineDictation("vault_search", searchQuery) { updated ->
              onSearchQueryChange(updated)
            }
          }
        },
        statusText = "Tap to speak search terms",
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(24.dp)
      )
    }

    // Note Details Modal Dialog for in-depth inspection of note memories
    selectedNoteForDetail?.let { note ->
      BasicAlertDialog(
        onDismissRequest = { selectedNoteForDetail = null }
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MonoBorder, RoundedCornerShape(16.dp)),
          color = MonoSurface
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Note Details & Memory",
                style = TextStyle(
                  color = MonoTextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )
              )
              IconButton(
                onClick = { selectedNoteForDetail = null },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = MonoTextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = note.title,
              style = TextStyle(
                color = MonoWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            )

            val fullDateFormatted = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(note.recordedAt))
            Text(
              text = fullDateFormatted,
              style = TextStyle(
                color = MonoTextMuted,
                fontSize = 12.sp
              )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "EXECUTIVE SUMMARY",
              style = TextStyle(
                color = MonoTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = note.executiveSummary.ifBlank { note.cleanText },
              style = TextStyle(
                color = MonoTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )
            )

            if (note.structuredNotes.isNotBlank()) {
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "KEY DECISIONS & MEMORY POINTS",
                style = TextStyle(
                  color = MonoTextMuted,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(MonoSurfaceElevated)
                  .padding(10.dp)
              ) {
                Text(
                  text = note.structuredNotes,
                  style = TextStyle(
                    color = MonoTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                  )
                )
              }
            }

            if (note.rawTranscript.isNotBlank() && note.rawTranscript != note.cleanText) {
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "RAW VOICE TRANSCRIPT",
                style = TextStyle(
                  color = MonoTextMuted,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "\"${note.rawTranscript}\"",
                style = TextStyle(
                  color = MonoTextMuted,
                  fontSize = 12.sp,
                  lineHeight = 16.sp
                )
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MonoSurfaceElevated)
                  .border(1.dp, MonoBorder, RoundedCornerShape(8.dp))
                  .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Note", "${note.title}\n\n${note.executiveSummary}\n\n${note.structuredNotes}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                  }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MonoTextSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Copy",
                    style = TextStyle(
                      color = MonoTextSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(ActiveDestructive.copy(alpha = 0.15f))
                  .border(1.dp, ActiveDestructive.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                  .clickable {
                    onDeleteNote(note.id)
                    selectedNoteForDetail = null
                  }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = ActiveDestructive,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Delete",
                    style = TextStyle(
                      color = ActiveDestructive,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VaultFilterChip(
  label: String,
  count: Int,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) MonoWhite else MonoSurface)
      .border(
        width = 1.dp,
        color = if (isSelected) MonoWhite else MonoBorder,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = label,
        style = TextStyle(
          color = if (isSelected) Color.Black else MonoTextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$count",
        style = TextStyle(
          color = if (isSelected) Color.Black.copy(alpha = 0.7f) else MonoTextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
      )
    }
  }
}

@Composable
private fun VaultSectionHeader(title: String) {
  Text(
    text = title,
    style = TextStyle(
      color = MonoTextMuted,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    ),
    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
  )
}

/**
 * Executive Report Item
 */
@Composable
private fun VaultReportItem(
  rollup: TwoHourRollupEntity,
  onCopy: () -> Unit
) {
  val blockFormat = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
  val startFormatted = blockFormat.format(Date(rollup.twoHourBlockStart))

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorderSubtle, RoundedCornerShape(12.dp))
      .padding(16.dp)
      .testTag("report_item_${rollup.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ActiveAccentSubtle),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = null,
            tint = ActiveAccent,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "2-Hour Executive Rollup",
            style = TextStyle(
              color = MonoWhite,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          )
          Text(
            text = "Block starting $startFormatted",
            style = TextStyle(
              color = MonoTextMuted,
              fontSize = 11.sp
            )
          )
        }
      }

      IconButton(
        onClick = onCopy,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Copy report",
          tint = MonoTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = rollup.executiveSummary,
      style = TextStyle(
        color = MonoTextPrimary,
        fontSize = 13.sp,
        lineHeight = 19.sp
      )
    )

    if (rollup.formattedBillableText.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(MonoSurfaceElevated)
          .padding(8.dp)
      ) {
        Text(
          text = rollup.formattedBillableText,
          style = TextStyle(
            color = MonoTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        )
      }
    }
  }
}

/**
 * Minimalist Task Item
 */
@Composable
private fun VaultTaskItem(
  task: ActionItemEntity,
  onToggle: (Boolean) -> Unit,
  onDelete: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorderSubtle, RoundedCornerShape(10.dp))
      .padding(14.dp)
      .testTag("task_item_${task.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Interactive Checkbox
    Box(
      modifier = Modifier
        .size(22.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(if (task.isCompleted) ActiveSuccess else Color.Transparent)
        .border(
          width = 1.5.dp,
          color = if (task.isCompleted) ActiveSuccess else MonoBorder,
          shape = RoundedCornerShape(6.dp)
        )
        .clickable { onToggle(!task.isCompleted) }
        .testTag("task_checkbox_${task.id}"),
      contentAlignment = Alignment.Center
    ) {
      if (task.isCompleted) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Completed",
          tint = MonoBackground,
          modifier = Modifier.size(14.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = task.title,
        style = TextStyle(
          color = if (task.isCompleted) MonoTextMuted else MonoWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
      )

      Spacer(modifier = Modifier.height(3.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (task.dueDateFormatted.isNotBlank()) {
          Text(
            text = task.dueDateFormatted,
            style = TextStyle(
              color = ActiveAccent,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          )
        }

        if (task.priority.isNotBlank()) {
          Text(
            text = "•  ${task.priority}",
            style = TextStyle(
              color = MonoTextMuted,
              fontSize = 11.sp
            )
          )
        }
      }
    }

    IconButton(
      onClick = onDelete,
      modifier = Modifier.size(28.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete task",
        tint = MonoTextMuted,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

/**
 * Minimalist Note Item
 */
@Composable
private fun VaultNoteItem(
  note: ContextNoteEntity,
  onClick: () -> Unit = {},
  onDelete: () -> Unit,
  onCopy: () -> Unit
) {
  val dateFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.recordedAt))

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorderSubtle, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(16.dp)
      .testTag("note_item_${note.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = note.title,
          style = TextStyle(
            color = MonoWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        )
        Text(
          text = dateFormatted,
          style = TextStyle(
            color = MonoTextMuted,
            fontSize = 11.sp
          )
        )
      }

      Row {
        IconButton(
          onClick = onCopy,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy note",
            tint = MonoTextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete note",
            tint = MonoTextMuted,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = note.executiveSummary.ifBlank { note.cleanText },
      style = TextStyle(
        color = MonoTextPrimary,
        fontSize = 13.sp,
        lineHeight = 19.sp
      )
    )

    if (note.structuredNotes.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(MonoSurfaceElevated)
          .padding(8.dp)
      ) {
        Text(
          text = note.structuredNotes,
          style = TextStyle(
            color = MonoTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        )
      }
    }
  }
}

/**
 * Minimalist Calendar Event Item
 */
@Composable
private fun VaultCalendarItem(
  event: CalendarEventEntity,
  onDelete: () -> Unit
) {
  val timeFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
  val startFormatted = timeFormat.format(Date(event.startTime))

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorderSubtle, RoundedCornerShape(10.dp))
      .padding(14.dp)
      .testTag("calendar_item_${event.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(ActiveAccentSubtle)
        .border(1.dp, ActiveAccent, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Event,
        contentDescription = null,
        tint = ActiveAccent,
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = event.title,
        style = TextStyle(
          color = MonoWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = startFormatted,
        style = TextStyle(
          color = MonoTextSecondary,
          fontSize = 12.sp
        )
      )

      if (!event.location.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MonoTextMuted,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = event.location,
            style = TextStyle(
              color = MonoTextMuted,
              fontSize = 11.sp
            )
          )
        }
      }
    }

    IconButton(
      onClick = onDelete,
      modifier = Modifier.size(28.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete calendar event",
        tint = MonoTextMuted,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
