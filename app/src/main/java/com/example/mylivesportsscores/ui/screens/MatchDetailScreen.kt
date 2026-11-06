package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mylivesportsscores.data.remote.SportmonksEvent
import com.example.mylivesportsscores.data.remote.SportmonksFixture
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailUiState
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Details") },
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
                    MatchDetailContent(fixture = state.fixture, aiSummary = state.aiSummary)
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
fun MatchDetailContent(fixture: SportmonksFixture, aiSummary: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MatchHeader(fixture)
        }

        item {
            AiSummaryCard(summary = aiSummary)
        }

        if (fixture.events.isNotEmpty()) {
            item {
                Text(
                    text = "Match Events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(fixture.events.sortedBy { it.minute }) { event ->
                EventItem(event)
            }
        } else {
            item {
                Text("No match events available.")
            }
        }
    }
}

@Composable
fun MatchHeader(fixture: SportmonksFixture) {
    val homeParticipant = fixture.participants.find { it.meta?.location == "home" }
    val awayParticipant = fixture.participants.find { it.meta?.location == "away" }
    val homeScore = fixture.scores.find { it.description == "CURRENT" && it.id == homeParticipant?.id }?.score?.goals
    val awayScore = fixture.scores.find { it.description == "CURRENT" && it.id == awayParticipant?.id }?.score?.goals

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = fixture.league?.name ?: "", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColumn(name = homeParticipant?.name ?: "Home", score = homeScore)
                Text(text = "vs", style = MaterialTheme.typography.titleMedium)
                TeamColumn(name = awayParticipant?.name ?: "Away", score = awayScore)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = fixture.starting_at, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TeamColumn(name: String, score: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (score != null) {
            Text(text = score.toString(), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun AiSummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Match Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EventItem(event: SportmonksEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${event.minute}'",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${event.player_name ?: "Unknown Player"} - ${getEventDescription(event.type_id)}",
            style = MaterialTheme.typography.bodyMedium
        )
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
