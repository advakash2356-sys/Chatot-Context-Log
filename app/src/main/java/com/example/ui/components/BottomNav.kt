package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.M3OnPrimaryContainer
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer

data class NavItem(val label: String, val icon: ImageVector, val tag: String)

@Composable
fun BottomNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val navItems = listOf(
        NavItem("Memories", Icons.Default.Schedule, "nav_memories"),
        NavItem("Tasks", Icons.Default.CheckCircle, "nav_tasks"),
        NavItem("Ask Neo", Icons.Default.Psychology, "nav_ask_neo"),
        NavItem("Settings", Icons.Default.Tune, "nav_settings")
    )

    NavigationBar(
        tonalElevation = 8.dp
    ) {
        navItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
                modifier = Modifier.testTag(item.tag),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = M3OnPrimaryContainer,
                    selectedTextColor = M3Primary,
                    indicatorColor = M3PrimaryContainer
                )
            )
        }
    }
}

