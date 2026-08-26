package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.util.HapticFeedbackManager

data class NavItem(val label: String, val icon: ImageVector, val tag: String)

@Composable
fun BottomNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager.getInstance(context) }

    val navItems = listOf(
        NavItem("Capture", Icons.Default.Mic, "nav_voice"),
        NavItem("Memories", Icons.Default.Schedule, "nav_memories"),
        NavItem("Tasks", Icons.Default.CheckCircle, "nav_tasks"),
        NavItem("Ask Neo", Icons.Default.Psychology, "nav_ask_neo"),
        NavItem("Settings", Icons.Default.Tune, "nav_settings")
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0F121C).copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val iconColor = animateColorAsState(
                        targetValue = if (isSelected) ElectricCyan else Color(0xFF64748B),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_icon_color"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                hapticManager.triggerSelection()
                                onTabSelected(index)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag(item.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(ElectricCyan.copy(alpha = 0.18f), NeonViolet.copy(alpha = 0.18f))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor.value,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) ElectricCyan else Color(0xFF64748B),
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}
