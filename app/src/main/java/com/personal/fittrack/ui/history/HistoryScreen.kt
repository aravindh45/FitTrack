package com.personal.fittrack.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personal.fittrack.domain.calc.CalorieNeedCalculator
import com.personal.fittrack.domain.calc.DateUtils
import com.personal.fittrack.domain.model.DailySummary
import com.personal.fittrack.domain.model.FoodLogEntry
import com.personal.fittrack.domain.model.GoalType
import com.personal.fittrack.domain.model.treatsExcessAsBad
import com.personal.fittrack.ui.common.ChartPoint
import com.personal.fittrack.ui.common.MealLogCard
import com.personal.fittrack.ui.common.MonthCalendarHeatmap
import com.personal.fittrack.ui.common.NutrientMiniCard
import com.personal.fittrack.ui.common.PeriodSummaryCard
import com.personal.fittrack.ui.common.ProfessionalBarChart
import com.personal.fittrack.ui.theme.CalorieApproachingLimit
import com.personal.fittrack.ui.theme.CalorieOnTrack
import com.personal.fittrack.ui.theme.CalorieOverBudget
import com.personal.fittrack.ui.theme.GoogleBlue
import com.personal.fittrack.ui.theme.GoogleYellow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val listSections = listOf("Daily Log", "Charts")
private val chartSections = listOf("Week", "Month", "All Time")
private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")
private val WEEK_RANGE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
private val WEEK_RANGE_DAY_ONLY_FORMATTER = DateTimeFormatter.ofPattern("d")
private const val HISTORY_WINDOW_DAYS = 365L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val summaries by viewModel.summaries.collectAsState()
    val profile by viewModel.profile.collectAsState()
    var selectedSection by remember { mutableIntStateOf(0) }

    val today = DateUtils.todayEpochDay()
    // One Health Connect query for the whole window History can show - sliced per
    // day/week/month wherever it's needed, instead of a separate query per chart tab.
    val activeCaloriesByDay by produceState(initialValue = emptyMap<Long, Int>(), viewModel) {
        value = viewModel.activeCaloriesBurnedByDay(today - HISTORY_WINDOW_DAYS, today)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            listSections.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    shape = SegmentedButtonDefaults.itemShape(index, listSections.size)
                ) {
                    Text(label)
                }
            }
        }

        if (summaries.isEmpty() || profile == null) {
            Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Text(
                    text = "No history yet - log a meal to start building your timeline.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        val dailyGoal = remember(profile) { CalorieNeedCalculator.dailyCalorieGoal(profile!!) }
        val goalType = profile!!.goalType

        when (selectedSection) {
            0 -> DailyLogList(summaries, viewModel, activeCaloriesByDay, dailyGoal, goalType)
            else -> ChartsSection(summaries, dailyGoal, goalType, activeCaloriesByDay)
        }
    }
}

@Composable
private fun DailyLogList(
    summaries: List<DailySummary>,
    viewModel: HistoryViewModel,
    activeCaloriesByDay: Map<Long, Int>,
    dailyGoal: Int,
    goalType: GoalType
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(summaries, key = { it.dateEpochDay }) { summary ->
            DailyCard(
                summary = summary,
                activeCaloriesBurned = activeCaloriesByDay[summary.dateEpochDay] ?: 0,
                dailyGoal = dailyGoal,
                goalType = goalType,
                onRelog = viewModel::relogEntry,
                onDelete = { viewModel.deleteEntry(it) }
            )
        }
    }
}

/** One day's worth of logging, grouped into a single card: a centered dd/MM/yyyy header,
 * a row of small nutrient tiles summarizing the whole day, an energy-balance row folding in
 * Health Connect's activity burn (when available), then every individual meal logged that
 * day (each with its own time stamp and re-log/delete actions). */
@Composable
private fun DailyCard(
    summary: DailySummary,
    activeCaloriesBurned: Int,
    dailyGoal: Int,
    goalType: GoalType,
    onRelog: (FoodLogEntry) -> Unit,
    onDelete: (Long) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = DateUtils.formatEpochDay(summary.dateEpochDay),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                NutrientMiniCard("Calories", "${summary.totalCalories}", GoogleBlue, Modifier.weight(1f))
                NutrientMiniCard("Protein", "${summary.totalProteinG.toInt()}g", CalorieOnTrack, Modifier.weight(1f))
                NutrientMiniCard("Carbs", "${summary.totalCarbsG.toInt()}g", CalorieApproachingLimit, Modifier.weight(1f))
                NutrientMiniCard("Fat", "${summary.totalFatG.toInt()}g", CalorieOverBudget, Modifier.weight(1f))
                NutrientMiniCard("Fiber", "${summary.totalFiberG.toInt()}g", GoogleYellow, Modifier.weight(1f))
            }

            if (activeCaloriesBurned > 0 && dailyGoal > 0) {
                val netDeficit = (dailyGoal - summary.totalCalories) + activeCaloriesBurned
                val isDeficit = netDeficit >= 0
                val isOnTrack = if (isDeficit) true else !goalType.treatsExcessAsBad()
                val deficitColor = if (isOnTrack) CalorieOnTrack else CalorieOverBudget
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    NutrientMiniCard("Active Burn", "$activeCaloriesBurned kcal", GoogleBlue, Modifier.weight(1f))
                    NutrientMiniCard(
                        if (isDeficit) "Deficit" else "Surplus",
                        "${abs(netDeficit)} kcal",
                        deficitColor,
                        Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateContentSize()) {
                summary.entries.forEach { entry ->
                    MealLogCard(
                        entry = entry,
                        onRelog = { onRelog(entry) },
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }
}

/** "< label >" navigation row, shared by the Week and Month chart tabs - Google Fit uses the
 * exact same pattern (arrows either side of "January 2022"), just with a date-range label
 * instead of a month name for the Week tab. The right arrow is disabled once you're back at
 * the current period, since there's no future data to look at. */
@Composable
private fun PeriodNavigationHeader(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartsSection(summaries: List<DailySummary>, dailyGoal: Int, goalType: GoalType, activeCaloriesByDay: Map<Long, Int>) {
    var chartTab by remember { mutableIntStateOf(0) }
    val byDay = remember(summaries) { summaries.associateBy { it.dateEpochDay } }
    val today = DateUtils.todayEpochDay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            chartSections.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = chartTab == index,
                    onClick = { chartTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index, chartSections.size)
                ) {
                    Text(label)
                }
            }
        }

        when (chartTab) {
            0 -> WeekChartTab(byDay, summaries, today, dailyGoal, goalType, activeCaloriesByDay)
            1 -> MonthChartTab(byDay, summaries, today, dailyGoal, goalType, activeCaloriesByDay)
            else -> {
                val daysTracked = summaries.size
                val totalConsumed = summaries.sumOf { it.totalCalories }
                val activeBurn = activeCaloriesByDay.values.sum()
                PeriodSummaryCard(
                    periodLabel = "All-Time",
                    goalType = goalType,
                    daysTracked = daysTracked,
                    totalConsumed = totalConsumed,
                    totalGoal = dailyGoal * daysTracked,
                    averageDailyCalories = if (daysTracked > 0) totalConsumed / daysTracked else 0,
                    activeCaloriesBurned = activeBurn
                )
            }
        }
    }
}

private fun sumActiveCalories(activeCaloriesByDay: Map<Long, Int>, startDay: Long, endDay: Long): Int =
    activeCaloriesByDay.filterKeys { it in startDay..endDay }.values.sum()

@Composable
private fun WeekChartTab(
    byDay: Map<Long, DailySummary>,
    summaries: List<DailySummary>,
    today: Long,
    dailyGoal: Int,
    goalType: GoalType,
    activeCaloriesByDay: Map<Long, Int>
) {
    var weeksAgo by remember { mutableIntStateOf(0) }
    val startOfWeek = remember(weeksAgo) { DateUtils.startOfWeekEpochDay() - weeksAgo * 7L }
    val endOfWeek = startOfWeek + 6
    val statsEndDay = minOf(today, endOfWeek)

    val weekPoints = remember(byDay, startOfWeek) { buildDayLabeledPoints(byDay, startOfWeek, endOfWeek) }
    val stats = remember(summaries, startOfWeek, statsEndDay) { periodStats(summaries, startOfWeek, statsEndDay, dailyGoal) }
    val activeBurn = remember(activeCaloriesByDay, startOfWeek, statsEndDay) { sumActiveCalories(activeCaloriesByDay, startOfWeek, statsEndDay) }

    PeriodNavigationHeader(
        label = formatWeekRange(startOfWeek, endOfWeek),
        onPrevious = { weeksAgo++ },
        onNext = { if (weeksAgo > 0) weeksAgo-- },
        nextEnabled = weeksAgo > 0
    )
    ProfessionalBarChart(
        points = weekPoints,
        goal = dailyGoal,
        goalType = goalType,
        modifier = Modifier.fillMaxWidth().height(170.dp)
    )
    PeriodSummaryCard(
        periodLabel = "This Week's",
        goalType = goalType,
        daysTracked = stats.daysTracked,
        totalConsumed = stats.totalConsumed,
        totalGoal = stats.totalGoal,
        averageDailyCalories = stats.averageDailyCalories,
        activeCaloriesBurned = activeBurn
    )
}

@Composable
private fun MonthChartTab(
    byDay: Map<Long, DailySummary>,
    summaries: List<DailySummary>,
    today: Long,
    dailyGoal: Int,
    goalType: GoalType,
    activeCaloriesByDay: Map<Long, Int>
) {
    var monthsAgo by remember { mutableIntStateOf(0) }
    val displayedMonth = remember(monthsAgo) { YearMonth.now().minusMonths(monthsAgo.toLong()) }
    val todayDate = remember { LocalDate.ofEpochDay(today) }
    val startOfMonth = displayedMonth.atDay(1).toEpochDay()
    val statsEndDay = if (monthsAgo == 0) today else displayedMonth.atEndOfMonth().toEpochDay()

    val caloriesByDayOfMonth = remember(byDay, startOfMonth, statsEndDay) {
        (startOfMonth..statsEndDay).associate { day ->
            LocalDate.ofEpochDay(day).dayOfMonth to (byDay[day]?.totalCalories ?: 0)
        }
    }
    val stats = remember(summaries, startOfMonth, statsEndDay) { periodStats(summaries, startOfMonth, statsEndDay, dailyGoal) }
    val activeBurn = remember(activeCaloriesByDay, startOfMonth, statsEndDay) { sumActiveCalories(activeCaloriesByDay, startOfMonth, statsEndDay) }

    PeriodNavigationHeader(
        label = displayedMonth.format(MONTH_YEAR_FORMATTER),
        onPrevious = { monthsAgo++ },
        onNext = { if (monthsAgo > 0) monthsAgo-- },
        nextEnabled = monthsAgo > 0
    )
    MonthCalendarHeatmap(
        year = displayedMonth.year,
        month = displayedMonth.monthValue,
        caloriesByDay = caloriesByDayOfMonth,
        goal = dailyGoal,
        goalType = goalType,
        today = todayDate
    )
    PeriodSummaryCard(
        periodLabel = "This Month's",
        goalType = goalType,
        daysTracked = stats.daysTracked,
        totalConsumed = stats.totalConsumed,
        totalGoal = stats.totalGoal,
        averageDailyCalories = stats.averageDailyCalories,
        activeCaloriesBurned = activeBurn
    )
}

private data class PeriodStats(val daysTracked: Int, val totalConsumed: Int, val totalGoal: Int, val averageDailyCalories: Int)

/** "Days tracked" counts days that actually have a logged entry in range, not every
 * calendar day elapsed - an un-logged day isn't a 100%-deficit day, it's just missing data. */
private fun periodStats(summaries: List<DailySummary>, startDay: Long, endDay: Long, goal: Int): PeriodStats {
    val inRange = summaries.filter { it.dateEpochDay in startDay..endDay }
    val totalConsumed = inRange.sumOf { it.totalCalories }
    val daysTracked = inRange.size
    return PeriodStats(
        daysTracked = daysTracked,
        totalConsumed = totalConsumed,
        totalGoal = goal * daysTracked,
        averageDailyCalories = if (daysTracked > 0) totalConsumed / daysTracked else 0
    )
}

private fun buildDayLabeledPoints(byDay: Map<Long, DailySummary>, startDay: Long, endDay: Long): List<ChartPoint> =
    (startDay..endDay).map { day ->
        val label = LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        ChartPoint(label, byDay[day]?.totalCalories ?: 0)
    }

/** "Oct 11 - 17" when the week sits inside one month, "Oct 28 - Nov 3" when it spans two. */
private fun formatWeekRange(startEpochDay: Long, endEpochDay: Long): String {
    val start = LocalDate.ofEpochDay(startEpochDay)
    val end = LocalDate.ofEpochDay(endEpochDay)
    val startText = start.format(WEEK_RANGE_FORMATTER)
    val endText = if (start.month == end.month) end.format(WEEK_RANGE_DAY_ONLY_FORMATTER) else end.format(WEEK_RANGE_FORMATTER)
    return "$startText - $endText"
}
