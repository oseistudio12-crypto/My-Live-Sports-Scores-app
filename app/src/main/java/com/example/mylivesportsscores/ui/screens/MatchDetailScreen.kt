package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylivesportsscores.data.remote.SportmonksEvent
import com.example.mylivesportsscores.data.remote.SportmonksFixture
import com.example.mylivesportsscores.data.remote.SportmonksLineup
import com.example.mylivesportsscores.data.remote.SportmonksStatistic
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailTab
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailUiState
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MatchDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MatchDetailUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProfessionalMatchHeader(fixture = state.fixture)
                        
                        TabRow(selectedTabIndex = selectedTab.ordinal) {
                            MatchDetailTab.entries.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.setSelectedTab(tab) },
                                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                MatchDetailTab.OVERVIEW -> MatchOverview(fixture = state.fixture, aiSummary = state.aiSummary)
                                MatchDetailTab.STATS -> MatchStats(fixture = state.fixture)
                                MatchDetailTab.LINEUPS -> MatchLineups(fixture = state.fixture)
                            }
                        }
                    }
                }
                is MatchDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalMatchHeader(fixture: SportmonksFixture) {
    val homeParticipant = fixture.participants.find { it.meta?.location == "home" }
    val awayParticipant = fixture.participants.find { it.meta?.location == "away" }
    val homeScore = fixture.scores.find { it.description == "CURRENT" && it.participant_id == homeParticipant?.id }?.score?.goals
        ?: fixture.scores.find { it.participant_id == homeParticipant?.id }?.score?.goals
    val awayScore = fixture.scores.find { it.description == "CURRENT" && it.participant_id == awayParticipant?.id }?.score?.goals
        ?: fixture.scores.find { it.participant_id == awayParticipant?.id }?.score?.goals

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = fixture.league?.name?.uppercase() ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamHeaderItem(name = homeParticipant?.name ?: "Home", score = homeScore, logoUrl = homeParticipant?.image_path)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                Text(
                    text = fixture.starting_at.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            TeamHeaderItem(name = awayParticipant?.name ?: "Away", score = awayScore, logoUrl = awayParticipant?.image_path)
        }
    }
}

@Composable
fun TeamHeaderItem(name: String, score: Int?, logoUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "Team Logo",
            modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (score != null) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun MatchOverview(fixture: SportmonksFixture, aiSummary: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AiSummaryCard(summary = aiSummary)
        }

        if (fixture.events.isNotEmpty()) {
            item {
                Text(text = "Key Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(fixture.events.sortedByDescending { it.minute }) { event ->
                ProfessionalEventItem(event)
            }
        }
    }
}

@Composable
fun MatchStats(fixture: SportmonksFixture) {
    if (fixture.statistics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Statistics not available for this match.")
        }
        return
    }

    val homeId = fixture.participants.find { it.meta?.location == "home" }?.id
    val awayId = fixture.participants.find { it.meta?.location == "away" }?.id

    val statsByCode = fixture.statistics.groupBy { it.data?.type?.name ?: "Unknown" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        statsByCode.forEach { (name, stats) ->
            item {
                val homeVal = stats.find { it.participant_id == homeId }?.data?.value?.toString() ?: "0"
                val awayVal = stats.find { it.participant_id == awayId }?.data?.value?.toString() ?: "0"
                StatRow(label = name, homeValue = homeVal, awayValue = awayVal)
            }
        }
    }
}

@Composable
fun StatRow(label: String, homeValue: String, awayValue: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = homeValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(text = awayValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { calculateProgress(homeValue, awayValue) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    }
}

private fun calculateProgress(h: String, a: String): Float {
    val hv = h.filter { it.isDigit() }.toFloatOrNull() ?: 0f
    val av = a.filter { it.isDigit() }.toFloatOrNull() ?: 0f
    if (hv + av == 0f) return 0.5f
    return hv / (hv + av)
}

@Composable
fun MatchLineups(fixture: SportmonksFixture) {
    if (fixture.lineups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lineups not available for this match.")
        }
        return
    }

    val homeId = fixture.participants.find { it.meta?.location == "home" }?.id
    val awayId = fixture.participants.find { it.meta?.location == "away" }?.id

    val homeStarters = fixture.lineups.filter { it.participant_id == homeId && it.type_id == 11 }
    val awayStarters = fixture.lineups.filter { it.participant_id == awayId && it.type_id == 11 }
    
    val homeFormation = fixture.formations.find { it.participant_id == homeId }?.formation
    val awayFormation = fixture.formations.find { it.participant_id == awayId }?.formation

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "HOME FORMATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = homeFormation ?: "N/A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(text = "AWAY FORMATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = awayFormation ?: "N/A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Home Starters", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Text(text = "Away Starters", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.outline)
            }
        }
        
        val maxSize = maxOf(homeStarters.size, awayStarters.size)
        items(maxSize) { index ->
            Row(modifier = Modifier.fillMaxWidth()) {
                val homePlayer = homeStarters.getOrNull(index)
                val awayPlayer = awayStarters.getOrNull(index)
                
                Text(
                    text = homePlayer?.let { "${it.jersey_number ?: ""} ${it.player_name ?: ""}" } ?: "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = awayPlayer?.let { "${it.player_name ?: ""} ${it.jersey_number ?: ""}" } ?: "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun ProfessionalEventItem(event: SportmonksEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${event.minute}'",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        val (icon, color) = when (event.type_id) {
            1 -> "⚽" to Color.Green
            2 -> "🟨" to Color.Yellow
            3 -> "🟥" to Color.Red
            4 -> "🔄" to Color.Blue
            else -> "•" to MaterialTheme.colorScheme.outline
        }

        Text(text = icon, modifier = Modifier.padding(horizontal = 8.dp))
        
        Text(
            text = "${event.player_name ?: "Unknown Player"} - ${getEventDescription(event.type_id)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AiSummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "✨", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Match Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

fun getEventDescription(typeId: Int?): String {
    return when (typeId) {
        1 -> "Goal"
        2 -> "Yellow Card"
        3 -> "Red Card"
        4 -> "Substitution"
        else -> "Event"
    }
}
