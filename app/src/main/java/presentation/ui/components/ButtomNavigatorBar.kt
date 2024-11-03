package presentation.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.elena.autoplanner.R

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    // Lista de elementos con iconos de tipo `drawable`
    val items = listOf(
        BottomNavItem("More", R.drawable.baseline_dehaze_24, "more"),
        BottomNavItem("Tasks", R.drawable.baseline_checklist_24, "tasks"),
        BottomNavItem("Calendar", R.drawable.baseline_calendar_today_24, "calendar"),
        BottomNavItem("Profile", R.drawable.baseline_person_24, "profile")
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFF9800), // Color Naranja cuando está seleccionada
                    unselectedIconColor = Color.Gray // Color Gris cuando no está seleccionada
                )
            )
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: Int,
    val route: String
)
