package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextNoteEntity
import com.example.data.local.EntryType
import com.example.data.local.TwoHourRollupEntity
import com.example.ui.components.BillingRollupVisualizer
import com.example.ui.components.IndexingStatusBanner
import com.example.ui.components.NoteCard
import com.example.ui.components.TodayRollupCard
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer
import com.example.ui.theme.NeutralBackground
import com.example.ui.theme.NeutralOnSurface
import com.example.ui.theme.NeutralOnSurfaceVariant
import com.example.ui.theme.NeutralOutline
import com.example.ui.theme.PurpleOnSecondaryContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondaryContainer
import com.example.util.TextExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardTab(
  todayHours: Double,
  targetHours: Double,
  isSynced: Boolean,
  notes: List<ContextNoteEntity>,
  rollups: List<TwoHourRollupEntity> = emptyList(),
  calendarEvents: List<CalendarEventEntity> = emptyList(),
  indexingChunkCount: Int,
  isIndexing: Boolean,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedTypeFilter: EntryType?,
  onTypeFilterSelect: (EntryType?) -> Unit,
  onViewAllClick: () -> Unit,
  onNavigateToBilling: () -> Unit = {},
  onSyncCalendarClick: () -> Unit = {},
  onDeleteNote: (String) -> Unit,
  onRagBannerClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(NeutralBackground)
      .padding(horizontal = 16.dp)
      .testTag("dashboard_tab")
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))

      // Billing Rollup & Usage Statistics Dashboard Component
      BillingRollupVisualizer(
        todayHours = todayHours,
        targetHours = targetHours,
        notes = notes,
        rollups = rollups,
        calendarEvents = calendarEvents,
        onNavigateToBilling = onNavigateToBilling
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Synced Google Calendar Context Events Section
      if (calendarEvents.isNotEmpty()) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_calendar_events_card"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = M3Primary, modifier = Modifier.size(16.dp))
                Text(
                  text = "Google Calendar Context (${calendarEvents.size})",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              IconButton(
                onClick = onSyncCalendarClick,
                modifier = Modifier.size(24.dp)
              ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync Calendar", tint = M3Primary, modifier = Modifier.size(16.dp))
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(calendarEvents.take(5)) { evt ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = CardDefaults.outlinedCardBorder(),
                  modifier = Modifier.width(220.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                      text = "[${evt.matterCode}] ${evt.title}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${timeFormat.format(Date(evt.startTime))} - ${timeFormat.format(Date(evt.endTime))}",
                      fontSize = 10.sp,
                      color = M3Primary,
                      fontWeight = FontWeight.SemiBold
                    )
                    if (evt.location.isNotBlank()) {
                      Text(text = evt.location, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      IndexingStatusBanner(
        chunkCount = indexingChunkCount,
        isIndexing = isIndexing,
        onBannerClick = onRagBannerClick
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Search & Filter Row
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search logs, matter codes, FTS & semantic...", fontSize = 13.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = PurplePrimary,
            modifier = Modifier.size(20.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_search_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PurplePrimary,
          unfocusedBorderColor = NeutralOutline
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Type Filter Chips & Export Logs Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          item {
            FilterChip(
              selected = selectedTypeFilter == null,
              onClick = { onTypeFilterSelect(null) },
              label = { Text("ALL LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PurpleSecondaryContainer,
                selectedLabelColor = PurpleOnSecondaryContainer
              ),
              modifier = Modifier.testTag("filter_chip_all")
            )
          }

          items(EntryType.entries.toTypedArray()) { type ->
            FilterChip(
              selected = selectedTypeFilter == type,
              onClick = { onTypeFilterSelect(if (selectedTypeFilter == type) null else type) },
              label = { Text(type.name, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PurpleSecondaryContainer,
                selectedLabelColor = PurpleOnSecondaryContainer
              ),
              modifier = Modifier.testTag("filter_chip_${type.name.lowercase()}")
            )
          }
        }

        IconButton(
          onClick = {
            val report = TextExportHelper.formatContextLogsReportText(
              notes = notes,
              filterLabel = selectedTypeFilter?.name ?: "All Context Logs",
              billingRollups = rollups,
              calendarEvents = calendarEvents
            )
            TextExportHelper.exportAndShareTextFile(
              context = context,
              content = report,
              fileNamePrefix = "context_logs_export"
            )
          },
          modifier = Modifier.size(32.dp).testTag("export_logs_quick_btn")
        ) {
          Icon(imageVector = Icons.Default.Download, contentDescription = "Export Logs", tint = M3Primary, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Context Logs (${notes.size})",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = NeutralOnSurface
        )

        TextButton(
          onClick = onViewAllClick,
          modifier = Modifier.testTag("view_all_logs_button")
        ) {
          Text("View Timeline >", color = PurplePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
    }

    if (notes.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NeutralOutline.copy(alpha = 0.1f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No context logs match query.",
            fontSize = 13.sp,
            color = NeutralOnSurfaceVariant
          )
        }
      }
    } else {
      items(notes, key = { it.id }) { note ->
        NoteCard(
          note = note,
          onDeleteClick = onDeleteNote
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}
