package com.elena.autoplanner.presentation.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <-- Import getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.navigation.Screen

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    onMoreClick: () -> Unit, // Add callback for More button
) {
    val items = listOf(
        BottomNavItem("More", R.drawable.baseline_dehaze_24, "more_drawer_trigger"),
        BottomNavItem("Tasks", R.drawable.baseline_checklist_24, Screen.Tasks.routeBase),
        BottomNavItem("Calendar", R.drawable.baseline_calendar_today_24, Screen.Calendar.route),
        BottomNavItem("Profile", R.drawable.baseline_person_24, Screen.Profile.route)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        // Use 'by' for property delegation to get the NavBackStackEntry? directly
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        // Now you can safely access destination and route
        val currentRouteBase = navBackStackEntry?.destination?.route?.substringBefore("?")

        items.forEach { item ->
            val isSelected = currentRouteBase == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = isSelected, // More is never selected
                onClick = {
                    if (item.route == "more_drawer_trigger") {
                        onMoreClick() // Open the drawer
                    } else {
                        // Standard navigation for other items
                        navController.navigate(item.route) { // Navigate to routeBase for Tasks
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface
                ),
                alwaysShowLabel = true
            )
        }
    }
}

// BottomNavItem data class remains the same
data class BottomNavItem(
    val label: String,
    val icon: Int,
    val route: String,
)