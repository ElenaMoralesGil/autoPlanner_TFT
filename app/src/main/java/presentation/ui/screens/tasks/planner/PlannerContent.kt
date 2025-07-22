package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState

@Composable
fun PlannerContent(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onReviewTaskClick: (Task) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = if (state.currentStep == PlannerStep.REVIEW_PLAN) "Review Your Plan" else "Configure Your Auto-Plan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            when (state.currentStep) {
                PlannerStep.TIME_INPUT, PlannerStep.PRIORITY_INPUT, PlannerStep.ADDITIONAL_OPTIONS -> {

                    PlannerConfigurationStep(
                        state = state,
                        onIntent = onIntent,
                        onStartTimeClick = onStartTimeClick,
                        onEndTimeClick = onEndTimeClick
                    )
                }

                PlannerStep.REVIEW_PLAN -> {

                    PlanReviewScreen(
                        state = state,
                        onIntent = onIntent,
                        onReviewTaskClick = onReviewTaskClick
                    )
                }

            }
        }

        if (state.error != null) {
            Text(
                "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}