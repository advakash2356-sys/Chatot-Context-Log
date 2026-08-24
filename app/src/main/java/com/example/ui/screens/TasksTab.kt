package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActionItemEntity
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer

enum class TaskFilter {
    ALL,
    ASSIGNED_TO_YOU,
    ASSIGNED_TO_OTHERS,
    PENDING,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTab(
    actionItems: List<ActionItemEntity>,
    onToggleTask: (String, Boolean) -> Unit,
    onAddTask: (ActionItemEntity) -> Unit,
    onUpdateTask: (ActionItemEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onUpdateSyncStatus: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var isAddModalOpen by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<ActionItemEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    var exportIntegrationTask by remember { mutableStateOf<ActionItemEntity?>(null) }

    val filteredList = remember(actionItems, selectedFilter) {
        when (selectedFilter) {
            TaskFilter.ALL -> actionItems
            TaskFilter.ASSIGNED_TO_YOU -> actionItems.filter { it.isAssignedToYou }
            TaskFilter.ASSIGNED_TO_OTHERS -> actionItems.filter { !it.isAssignedToYou }
            TaskFilter.PENDING -> actionItems.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> actionItems.filter { it.isCompleted }
        }
    }

    val totalCount = actionItems.size
    val youCount = actionItems.count { it.isAssignedToYou && !it.isCompleted }
    val pendingCount = actionItems.count { !it.isCompleted }
    val completedCount = actionItems.count { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Overview Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Deterministic Action Item Pipeline",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-extracted commitments with owner and deadline",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { isAddModalOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary),
                        modifier = Modifier.testTag("add_commitment_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metric Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskMetricBadge(label = "Total", count = totalCount, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TaskMetricBadge(label = "Your Tasks", count = youCount, color = Color(0xFF188038), modifier = Modifier.weight(1f))
                    TaskMetricBadge(label = "Pending", count = pendingCount, color = Color(0xFFE37400), modifier = Modifier.weight(1f))
                    TaskMetricBadge(label = "Completed", count = completedCount, color = Color(0xFF5F6368), modifier = Modifier.weight(1f))
                }
            }
        }

        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TaskFilter.values()) { filter ->
                val label = when (filter) {
                    TaskFilter.ALL -> "All ($totalCount)"
                    TaskFilter.ASSIGNED_TO_YOU -> "Assigned to You ($youCount)"
                    TaskFilter.ASSIGNED_TO_OTHERS -> "Assigned to Others"
                    TaskFilter.PENDING -> "Pending ($pendingCount)"
                    TaskFilter.COMPLETED -> "Completed ($completedCount)"
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = M3PrimaryContainer,
                        selectedLabelColor = M3Primary
                    )
                )
            }
        }

        // Action Items List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No Action Items in This View",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Commitments made during meetings and conversations are extracted automatically.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    ActionItemCard(
                        item = item,
                        onToggleComplete = { completed -> onToggleTask(item.id, completed) },
                        onEditClick = { taskToEdit = item },
                        onDeleteClick = { taskToDelete = item.id },
                        onExportClick = { exportIntegrationTask = item }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add / Edit Commitment Modal Dialog
    if (isAddModalOpen || taskToEdit != null) {
        var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
        var owner by remember { mutableStateOf(taskToEdit?.owner ?: "You") }
        var actionVerb by remember { mutableStateOf(taskToEdit?.actionVerb ?: "Follow up") }
        var dueDate by remember { mutableStateOf(taskToEdit?.dueDateFormatted ?: "Today, 5:00 PM") }
        var priority by remember { mutableStateOf(taskToEdit?.priority ?: "HIGH") }

        AlertDialog(
            onDismissRequest = {
                isAddModalOpen = false
                taskToEdit = null
            },
            title = { Text(if (taskToEdit != null) "Edit Commitment" else "Add New Action Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task / Commitment Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = owner,
                            onValueChange = { owner = it },
                            label = { Text("Owner (You / Name)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = actionVerb,
                            onValueChange = { actionVerb = it },
                            label = { Text("Action Verb") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date / Time") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priority,
                            onValueChange = { priority = it },
                            label = { Text("Priority (HIGH/MED)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            if (taskToEdit != null) {
                                onUpdateTask(
                                    taskToEdit!!.copy(
                                        title = title,
                                        owner = owner,
                                        isAssignedToYou = owner.equals("You", ignoreCase = true),
                                        actionVerb = actionVerb,
                                        dueDateFormatted = dueDate,
                                        priority = priority
                                    )
                                )
                            } else {
                                onAddTask(
                                    ActionItemEntity(
                                        title = title,
                                        owner = owner,
                                        isAssignedToYou = owner.equals("You", ignoreCase = true),
                                        actionVerb = actionVerb,
                                        dueDateFormatted = dueDate,
                                        priority = priority
                                    )
                                )
                            }
                        }
                        isAddModalOpen = false
                        taskToEdit = null
                    }
                ) {
                    Text("Save Commitment")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddModalOpen = false
                    taskToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Action Item") },
            text = { Text("Are you sure you want to remove this commitment from your task pipeline?") },
            confirmButton = {
                Button(
                    onClick = {
                        taskToDelete?.let { onDeleteTask(it) }
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export to Native Productivity Services Dialog
    if (exportIntegrationTask != null) {
        val task = exportIntegrationTask!!
        AlertDialog(
            onDismissRequest = { exportIntegrationTask = null },
            icon = { Icon(Icons.Default.Extension, contentDescription = null, tint = M3Primary) },
            title = { Text("Export Commitment to Productivity Suite") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select a target ecosystem to export \"${task.title}\":",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Integration Grid
                    listOf(
                        Triple("ClickUp", "Task Manager", Color(0xFF7B68EE)),
                        Triple("Notion", "Database Sync", Color(0xFF2E2E2E)),
                        Triple("Apple Reminders", "Native iOS/macOS", Color(0xFF007AFF)),
                        Triple("Todoist", "Quick Task List", Color(0xFFE44332)),
                        Triple("Google Calendar", "Event Block", Color(0xFF4285F4))
                    ).forEach { (targetName, category, badgeColor) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val payload = """
[NEOSAPIEN COMMITMENT EXPORT]
Target Service: $targetName ($category)
Title: ${task.title}
Owner: ${task.owner}
Action Verb: ${task.actionVerb}
Due Date: ${task.dueDateFormatted}
Priority: ${task.priority}
Source Memory: ${task.memoryTitle}
Status: EXPORTED_READY
                                    """.trimIndent()
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Export Payload", payload))
                                    onUpdateSyncStatus(task.id, "SYNCED", targetName)
                                    Toast.makeText(context, "Exported & formatted for $targetName! Copied to clipboard.", Toast.LENGTH_LONG).show()
                                    exportIntegrationTask = null
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(targetName.take(1), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column {
                                        Text(targetName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = M3Primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exportIntegrationTask = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TaskMetricBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun ActionItemCard(
    item: ActionItemEntity,
    onToggleComplete: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val isDone = item.isCompleted

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_item_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Checkbox
            IconButton(
                onClick = { onToggleComplete(!isDone) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Mark Incomplete" else "Mark Complete",
                    tint = if (isDone) Color(0xFF188038) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Task Content Column
            Column(modifier = Modifier.weight(1f)) {
                // Badges Row: Action Verb, Owner, Priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action Verb Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(M3PrimaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.actionVerb.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = M3Primary
                        )
                    }

                    // Owner Pill
                    val ownerColor = if (item.isAssignedToYou) Color(0xFF188038) else Color(0xFF1A73E8)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ownerColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(10.dp), tint = ownerColor)
                            Text(
                                text = item.owner,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ownerColor
                            )
                        }
                    }

                    // Priority Pill
                    val (prioColor, prioLabel) = when (item.priority.uppercase()) {
                        "HIGH" -> Pair(Color(0xFFD93025), "HIGH")
                        "LOW" -> Pair(Color(0xFF5F6368), "LOW")
                        else -> Pair(Color(0xFFE37400), "MED")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(prioColor.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prioLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = prioColor
                        )
                    }

                    // Sync Status
                    if (!item.externalSyncTarget.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F0FE))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "→ ${item.externalSyncTarget}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A73E8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title Text
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Due Date & Source Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFE37400))
                        Text(
                            text = item.dueDateFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE37400)
                        )
                    }

                    if (!item.memoryTitle.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = item.memoryTitle ?: "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons: Export, Edit, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onExportClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = M3Primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
