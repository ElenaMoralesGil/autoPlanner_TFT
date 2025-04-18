package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.calendar.getIconRes

@Composable
fun ReviewViewToggle(
    availableViews: List<CalendarView>,
    selectedView: CalendarView,
    onViewSelected: (CalendarView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.Center
    ) {
        availableViews.forEachIndexed { index, view ->
            val isSelected = view == selectedView


            val shape = when {
                availableViews.size == 1 -> RoundedCornerShape(50)
                index == 0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                index == availableViews.lastIndex -> RoundedCornerShape(
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                )

                else -> RoundedCornerShape(0.dp)
            }


            IconToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onViewSelected(view) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = shape
                    )
                    .clip(shape),
                colors = androidx.compose.material3.IconButtonDefaults.iconToggleButtonColors(
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = view.getIconRes()),
                    contentDescription = view.name,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}