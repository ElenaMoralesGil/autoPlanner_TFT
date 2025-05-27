package com.elena.autoplanner.presentation.ui.screens.profile


import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.TimeSeriesStat
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.presentation.effects.ProfileEffect
import com.elena.autoplanner.presentation.intents.ProfileIntent
import com.elena.autoplanner.presentation.states.StatsTimeFrame
import com.elena.autoplanner.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToEditProfile: () -> Unit, 
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(state?.user?.uid) { 
        if (state?.user != null) {

            Log.d(
                "ProfileScreen",
                "User detected (UID: ${state?.user?.uid}), sending LoadData intent."
            )
            viewModel.sendIntent(ProfileIntent.LoadData)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ProfileEffect.NavigateToLoginScreen -> onNavigateToLogin()
                is ProfileEffect.NavigateToRegisterScreen -> onNavigateToRegister()
                is ProfileEffect.NavigateToEditProfileScreen -> {
                    onNavigateToEditProfile()
                } 
                is ProfileEffect.ReAuthenticationRequired -> {
                    snackbarHostState.showSnackbar("Please log in again to delete your account.")
                    onNavigateToLogin() 
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ProfileTopAppBar(isLoggedIn = state?.isLoggedIn ?: false) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                state?.isLoading == true && (state?.user == null || state?.stats == null) -> { 
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state?.isLoggedIn == true -> {
                    state?.user?.let { user ->
                        ProfileContent(
                            user = user,
                            stats = state?.stats,
                            selectedTimeFrame = state?.selectedTimeFrame ?: StatsTimeFrame.WEEKLY,
                            onTimeFrameSelected = { tf ->
                                viewModel.sendIntent(
                                    ProfileIntent.SelectTimeFrame(
                                        tf
                                    )
                                )
                            },
                            onLogout = { viewModel.sendIntent(ProfileIntent.Logout) },
                            onEditProfile = { viewModel.sendIntent(ProfileIntent.NavigateToEditProfile) },
                            onDeleteAccount = { viewModel.sendIntent(ProfileIntent.RequestDeleteAccount) }
                        )
                    }
                }

                else -> { 
                    LoggedOutContent(
                        onLogin = { viewModel.sendIntent(ProfileIntent.NavigateToLogin) },
                        onRegister = { viewModel.sendIntent(ProfileIntent.NavigateToRegister) }
                    )
                }
            }


            if (state?.showDeleteConfirmDialog == true) {
                AlertDialog(
                    containerColor = MaterialTheme.colorScheme.surface,
                    onDismissRequest = { viewModel.sendIntent(ProfileIntent.CancelDeleteAccount) },
                    title = { Text("Delete Account?") },
                    text = { Text("This action is permanent and cannot be undone. Are you sure?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.sendIntent(ProfileIntent.ConfirmDeleteAccount) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.sendIntent(ProfileIntent.CancelDeleteAccount) }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (state?.isLoading == true && state?.showDeleteConfirmDialog == false) {
                Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(isLoggedIn: Boolean) {
    TopAppBar(
        title = { Text("Profile") },
        actions = {
            if (isLoggedIn) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            titleContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun ProfileContent(

    user: User,
    stats: ProfileStats?,
    selectedTimeFrame: StatsTimeFrame,
    onTimeFrameSelected: (StatsTimeFrame) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        UserInfoSection(user = user)
        TimeFrameSelector(
            selectedTimeFrame = selectedTimeFrame,
            onTimeFrameSelected = onTimeFrameSelected
        )

        DisplayedStatsSection(stats = stats, selectedTimeFrame = selectedTimeFrame)
        ActionsSection(
            onEditProfile = onEditProfile,
            onDeleteAccount = onDeleteAccount,
            onLogout = onLogout
        )
    }
}

@Composable
fun TimeFrameSelector(
    selectedTimeFrame: StatsTimeFrame,
    onTimeFrameSelected: (StatsTimeFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsTimeFrame.entries.forEach { timeFrame ->
            FilterChip(
                selected = timeFrame == selectedTimeFrame,
                onClick = { onTimeFrameSelected(timeFrame) },
                label = { Text(timeFrame.displayName) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = if (timeFrame == selectedTimeFrame) {
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp,
                    enabled = true,
                    selected = timeFrame == selectedTimeFrame
                )
            )
        }
    }
}

@Composable
fun DisplayedStatsSection(stats: ProfileStats?, selectedTimeFrame: StatsTimeFrame) {


    val dataList: List<Any?> = remember(stats, selectedTimeFrame) {
        when (selectedTimeFrame) {
            StatsTimeFrame.WEEKLY -> listOf( 
                stats?.completedTasksDailyForWeek,
                stats?.successRateDailyForWeek,
                stats?.totalCompletedWeekly,
                stats?.overallSuccessRateWeekly
            )

            StatsTimeFrame.MONTHLY -> listOf( 
                stats?.completedTasksWeeklyForMonth,
                stats?.successRateWeeklyForMonth,
                stats?.totalCompletedMonthly,
                stats?.overallSuccessRateMonthly
            )

            StatsTimeFrame.YEARLY -> listOf( 
                stats?.completedTasksMonthlyForYear,
                stats?.successRateMonthlyForYear,
                stats?.totalCompletedYearly,
                stats?.overallSuccessRateYearly
            )
        }
    }


    val completedTasksData = when (selectedTimeFrame) {
        StatsTimeFrame.WEEKLY -> dataList[0] as? TimeSeriesStat<LocalDate>
        StatsTimeFrame.MONTHLY -> dataList[0] as? TimeSeriesStat<LocalDate> 
        StatsTimeFrame.YEARLY -> dataList[0] as? TimeSeriesStat<YearMonth>
    }
    val successRateData = when (selectedTimeFrame) {
        StatsTimeFrame.WEEKLY -> dataList[1] as? TimeSeriesStat<LocalDate>
        StatsTimeFrame.MONTHLY -> dataList[1] as? TimeSeriesStat<LocalDate> 
        StatsTimeFrame.YEARLY -> dataList[1] as? TimeSeriesStat<YearMonth>
    }
    val totalCompleted = dataList[2] as? Int
    val overallSuccess = dataList[3] as? Float

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        StatCard(
            title = "Completed Tasks (${selectedTimeFrame.displayName})",
            value = totalCompleted?.toString() ?: "-",
            timeSeriesData = completedTasksData, 
            selectedTimeFrame = selectedTimeFrame,
            isPercentage = false
        )
        StatCard(
            title = "Success Rate (${selectedTimeFrame.displayName})",
            value = "${String.format("%.1f", overallSuccess ?: 0f)}%",
            timeSeriesData = successRateData, 
            selectedTimeFrame = selectedTimeFrame,
            isPercentage = true
        )
    }
}

@Composable
fun UserInfoSection(user: User) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = (user.displayName ?: user.email).toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun <K : Comparable<K>> StatCard(

    title: String,
    value: String,
    timeSeriesData: TimeSeriesStat<K>?,
    selectedTimeFrame: StatsTimeFrame,
    isPercentage: Boolean, 
) {

    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    val chartStyle = m3ChartStyle(
        axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        axisGuidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        axisLineColor = MaterialTheme.colorScheme.outline

    )


    LaunchedEffect(timeSeriesData) {
        val entries =
            timeSeriesData?.entries?.entries?.sortedBy { it.key }?.mapIndexed { index, entry ->
                entryOf(index.toFloat(), entry.value) 
            } ?: emptyList()
        chartEntryModelProducer.setEntries(entries)
    }


    val bottomAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, chartValues ->
            val data = timeSeriesData?.entries?.entries?.sortedBy { it.key }
            val index = value.toInt()
            if (data != null && index >= 0 && index < data.size) {
                when (val key = data.elementAt(index).key) {
                    is LocalDate -> when (selectedTimeFrame) {
                        StatsTimeFrame.WEEKLY -> key.format(DateTimeFormatter.ofPattern("E"))
                        StatsTimeFrame.MONTHLY -> key.format(DateTimeFormatter.ofPattern("dd")) 
                        else -> ""
                    }

                    is YearMonth -> key.format(DateTimeFormatter.ofPattern("MMM")) 
                    else -> ""
                }
            } else {
                ""
            }
        }

    val startAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, chartValues ->
            if (isPercentage) "${value.toInt()}%" else value.toInt().toString()
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))


            if (timeSeriesData != null && timeSeriesData.entries.isNotEmpty()) {
                ProvideChartStyle(chartStyle = chartStyle) { 
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = MaterialTheme.colorScheme.primary.hashCode(), 
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                        )
                                    )
                                )
                            )
                        ),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(
                            valueFormatter = startAxisValueFormatter,
                            guideline = LineComponent(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    .hashCode(),
                                strokeWidthDp = 1f,
                                dynamicShader = verticalGradient(
                                    arrayOf(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
                                    )
                                ),
                            )
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = bottomAxisValueFormatter,
                            guideline = null 
                        ),
                        chartScrollState = rememberChartScrollState(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) 
                    )
                }
            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }
}

@Composable
fun ActionsSection(
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ActionItem(text = "Edit Profile", onClick = onEditProfile)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionItem(text = "Delete Account", isDestructive = true, onClick = onDeleteAccount)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionItem(text = "Logout", onClick = onLogout)
    }
}

@Composable
fun ActionItem(text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoggedOutContent(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Log in or register to view your profile and statistics.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Register", color = MaterialTheme.colorScheme.primary)
        }
    }
}