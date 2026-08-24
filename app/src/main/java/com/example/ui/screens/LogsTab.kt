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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContextNoteEntity
import com.example.data.local.EntryType
import com.example.ui.components.NoteCard
import com.example.ui.theme.M3OnPrimaryContainer
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer

@Composable
fun LogsTab(
    notes: List<ContextNoteEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTypeFilter: EntryType?,
    onTypeFilterSelect: (EntryType?) -> Unit,
    availableTags: List<String> = emptyList(),
    selectedTagFilter: String? = null,
    onTagFilterSelect: (String?) -> Unit = {},
    onAddTagToNote: (noteId: String, tag: String) -> Unit = { _, _ -> },
    onRemoveTagFromNote: (noteId: String, tag: String) -> Unit = { _, _ -> },
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("logs_tab")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = "Context Log Stream",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chronological timeline automatically partitioned into 2-hour billing blocks.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Filter logs...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = M3Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logs_tab_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Entry Type Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { onTypeFilterSelect(null) },
                            label = { Text("ALL LOGS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3PrimaryContainer,
                                selectedLabelColor = M3OnPrimaryContainer
                            ),
                            modifier = Modifier.testTag("logs_filter_chip_all")
                        )
                    }

                    items(EntryType.entries.toTypedArray()) { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { onTypeFilterSelect(if (selectedTypeFilter == type) null else type) },
                            label = { Text(type.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3PrimaryContainer,
                                selectedLabelColor = M3OnPrimaryContainer
                            ),
                            modifier = Modifier.testTag("logs_filter_chip_${type.name.lowercase()}")
                        )
                    }
                }

                // Custom Tag Granular Filters
                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Tags",
                            tint = M3Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TAG FILTERS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("tags_filter_row")
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTagFilter == null,
                                onClick = { onTagFilterSelect(null) },
                                label = { Text("All Tags", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("tag_filter_all")
                            )
                        }

                        items(availableTags) { tag ->
                            val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagFilterSelect(if (isSelected) null else tag) },
                                label = { Text("#$tag", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("tag_filter_$tag")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (notes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTagFilter != null) "No notes found with tag #$selectedTagFilter" else "No context notes logged yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onDeleteClick = onDeleteNote,
                    onAddTag = onAddTagToNote,
                    onRemoveTag = onRemoveTagFromNote
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
