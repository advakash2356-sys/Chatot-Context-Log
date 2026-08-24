package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContextNoteEntity
import com.example.data.local.DailyTokenAggregate
import com.example.data.local.MatterTokenAggregate
import com.example.data.local.TokenUsageEntity
import com.example.util.TextExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recharts-inspired Data Visualization Screen for AI Token Expenditure and Saved Context Logs.
 * Displays daily token trends, cost analysis, matter token distribution, and interactive inspection charts.
 */
@Composable
fun TokenExpenditureVisualizer(
    tokenMetrics: List<TokenUsageEntity>,
    notes: List<ContextNoteEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var chartViewMode by remember { mutableStateOf(0) } // 0: Daily Bar Chart, 1: Cumulative Area Curve

    // Calculate aggregated metrics
    val totalTokens = tokenMetrics.sumOf { it.totalTokens }.coerceAtLeast(1)
    val totalPromptTokens = tokenMetrics.sumOf { it.promptTokens }
    val totalCandidateTokens = tokenMetrics.sumOf { it.candidatesTokens }
    val totalCostUsd = tokenMetrics.sumOf { it.estimatedCostUsd }
    val totalNotesCount = notes.size.coerceAtLeast(1)
    val avgTokensPerNote = totalTokens / totalNotesCount
    // Paralegal / manual transcription savings model: ~$0.50 per manual voice transcription
    val estimatedManualCost = totalNotesCount * 0.50
    val costSavingsUsd = (estimatedManualCost - totalCostUsd).coerceAtLeast(0.0)

    // Build 7-day daily time series
    val dailyMap = tokenMetrics.groupBy { it.dateString }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayLabelFormat = SimpleDateFormat("EEE (M/d)", Locale.US)

    // Generate last 7 days series
    val last7Days = remember(tokenMetrics) {
        val now = System.currentTimeMillis()
        (6 downTo 0).map { dayOffset ->
            val time = now - (dayOffset * 86400000L)
            val dateKey = dateFormat.format(Date(time))
            val label = dayLabelFormat.format(Date(time))
            val dayItems = dailyMap[dateKey] ?: emptyList()
            DailyChartPoint(
                dateKey = dateKey,
                label = label,
                promptTokens = dayItems.sumOf { it.promptTokens },
                candidateTokens = dayItems.sumOf { it.candidatesTokens },
                totalTokens = dayItems.sumOf { it.totalTokens },
                costUsd = dayItems.sumOf { it.estimatedCostUsd },
                requestCount = dayItems.size
            )
        }
    }

    // Matter-wise breakdown
    val matterAggregates = remember(tokenMetrics) {
        tokenMetrics.groupBy { it.matterCode }
            .map { (matter, items) ->
                MatterTokenAggregate(
                    matterCode = matter,
                    totalTokens = items.sumOf { it.totalTokens },
                    totalCostUsd = items.sumOf { it.estimatedCostUsd },
                    requestCount = items.size
                )
            }
            .sortedByDescending { it.totalTokens }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Header Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Token,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "AI Token Expenditure & Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Real-time Gemini 3.5 Flash metrics & context log intelligence",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    val report = buildString {
                        appendLine("=== CHATOT AI TOKEN EXPENDITURE & BILLING REPORT ===")
                        appendLine("Generated: ${Date()}")
                        appendLine("Total Tokens: $totalTokens")
                        appendLine("Prompt Tokens: $totalPromptTokens | Candidate Tokens: $totalCandidateTokens")
                        appendLine("Total AI Cost: $${String.format(Locale.US, "%.4f", totalCostUsd)}")
                        appendLine("Saved Context Notes: $totalNotesCount")
                        appendLine("Avg Tokens / Note: $avgTokensPerNote")
                        appendLine("Estimated Cost Savings: $${String.format(Locale.US, "%.2f", costSavingsUsd)}")
                        appendLine("\n--- Daily Breakdown ---")
                        last7Days.forEach {
                            appendLine("${it.dateKey} (${it.label}): ${it.totalTokens} tokens ($${String.format(Locale.US, "%.5f", it.costUsd)}) across ${it.requestCount} requests")
                        }
                        appendLine("\n--- Matter Breakdown ---")
                        matterAggregates.forEach {
                            appendLine("Matter [${it.matterCode}]: ${it.totalTokens} tokens | $${String.format(Locale.US, "%.5f", it.totalCostUsd)} | ${it.requestCount} logs")
                        }
                    }
                    TextExportHelper.exportAndShareTextFile(context, report, "Chatot_Token_Billing_Report")
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("export_token_report_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Top 4 KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Total Tokens",
                value = if (totalTokens >= 1000) String.format(Locale.US, "%.1fk", totalTokens / 1000.0) else "$totalTokens",
                subtitle = "${totalPromptTokens} in / ${totalCandidateTokens} out",
                icon = Icons.Default.Token,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "AI Cost (USD)",
                value = "$${String.format(Locale.US, "%.4f", totalCostUsd)}",
                subtitle = "$0.075/1M prompt",
                icon = Icons.Default.MonetizationOn,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Context Notes",
                value = "$totalNotesCount",
                subtitle = "Saved & Embedded",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Cost Savings",
                value = "$${String.format(Locale.US, "%.2f", costSavingsUsd)}",
                subtitle = "vs manual transcription",
                icon = Icons.Default.Speed,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        // Chart Container Card (Recharts Styled)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("token_chart_container"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Token Expenditure",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Stacked prompt & candidate consumption",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle View Buttons
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ChartToggleChip(label = "Bar", isSelected = chartViewMode == 0) { chartViewMode = 0 }
                        ChartToggleChip(label = "Curve", isSelected = chartViewMode == 1) { chartViewMode = 1 }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    if (chartViewMode == 0) {
                        RechartsBarChart(
                            points = last7Days,
                            selectedIndex = selectedDayIndex,
                            onSelectIndex = { selectedDayIndex = it }
                        )
                    } else {
                        RechartsAreaChart(
                            points = last7Days,
                            selectedIndex = selectedDayIndex,
                            onSelectIndex = { selectedDayIndex = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF3B82F6), label = "Prompt Tokens (In)")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color(0xFF8B5CF6), label = "Candidate Tokens (Out)")
                }

                // Selected Day Inspector Tooltip
                val activePoint = selectedDayIndex?.let { if (it in last7Days.indices) last7Days[it] else null }
                if (activePoint != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(activePoint.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${activePoint.requestCount} AI requests processed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${activePoint.totalTokens} tokens",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "$${String.format(Locale.US, "%.5f", activePoint.costUsd)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Matter Token Breakdown Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Token Allocation by Matter Code",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (matterAggregates.isEmpty()) {
                    Text(
                        text = "No matter token data recorded yet. Voice notes will automatically allocate usage.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val maxMatterTokens = matterAggregates.maxOfOrNull { it.totalTokens }?.coerceAtLeast(1) ?: 1
                    matterAggregates.take(5).forEach { matter ->
                        val ratio = (matter.totalTokens.toFloat() / maxMatterTokens.toFloat()).coerceIn(0.05f, 1f)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = matter.matterCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "(${matter.requestCount} logs)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${matter.totalTokens} tokens ($${String.format(Locale.US, "%.4f", matter.totalCostUsd)})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                            )
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

data class DailyChartPoint(
    val dateKey: String,
    val label: String,
    val promptTokens: Int,
    val candidateTokens: Int,
    val totalTokens: Int,
    val costUsd: Double,
    val requestCount: Int
)

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RechartsBarChart(
    points: List<DailyChartPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    val maxTokens = points.maxOfOrNull { it.totalTokens }?.coerceAtLeast(100) ?: 100
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val totalFraction = (point.totalTokens.toFloat() / maxTokens.toFloat()).coerceIn(0.04f, 1f)
            val promptRatio = if (point.totalTokens > 0) point.promptTokens.toFloat() / point.totalTokens.toFloat() else 0.6f

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelectIndex(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Stacked Bar
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 24.dp else 18.dp)
                        .fillMaxHeight(totalFraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Candidate (top)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight((1f - promptRatio).coerceAtLeast(0.01f))
                                .background(Color(0xFF8B5CF6))
                        )
                        // Prompt (bottom)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(promptRatio.coerceAtLeast(0.01f))
                                .background(Color(0xFF3B82F6))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = point.label.split(" ").firstOrNull() ?: "",
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RechartsAreaChart(
    points: List<DailyChartPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    val maxTokens = points.maxOfOrNull { it.totalTokens }?.coerceAtLeast(100) ?: 100

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (points.isNotEmpty()) {
                    onSelectIndex((selectedIndex?.plus(1) ?: 0) % points.size)
                }
            }
    ) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val fillPath = Path()
        val linePath = Path()

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normY = (point.totalTokens.toFloat() / maxTokens.toFloat()).coerceIn(0.05f, 0.95f)
            val y = h - (normY * h)

            if (index == 0) {
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
                linePath.moveTo(x, y)
            } else {
                fillPath.lineTo(x, y)
                linePath.lineTo(x, y)
            }

            if (index == points.size - 1) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }

        // Draw Area Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.45f),
                    Color(0xFF3B82F6).copy(alpha = 0.05f)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Draw Smooth Line Stroke
        drawPath(
            path = linePath,
            color = Color(0xFF8B5CF6),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Circles for each node
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normY = (point.totalTokens.toFloat() / maxTokens.toFloat()).coerceIn(0.05f, 0.95f)
            val y = h - (normY * h)
            val isSelected = selectedIndex == index

            drawCircle(
                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF8B5CF6),
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun ChartToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
