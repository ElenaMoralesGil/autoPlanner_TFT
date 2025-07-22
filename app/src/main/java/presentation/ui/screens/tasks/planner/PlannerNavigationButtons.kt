package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState

@Composable
fun PlannerNavigationButtons(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReviewStep = state.currentStep == PlannerStep.REVIEW_PLAN

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        OutlinedButton(
            onClick = {
                if (isReviewStep) {

                    onIntent(PlannerIntent.GoToPreviousStep)
                } else {

                    onIntent(PlannerIntent.CancelPlanner)
                }
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isReviewStep) "Back" else "Cancel",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                if (isReviewStep) {
                    onIntent(PlannerIntent.AddPlanToCalendar)
                } else {

                    onIntent(PlannerIntent.GeneratePlan)
                }
            },
            enabled = when {
                isReviewStep -> !state.requiresResolution && !state.isLoading
                else -> state.canGeneratePlan && !state.isLoading
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isReviewStep) "Add Plan" else "Generate Plan",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}