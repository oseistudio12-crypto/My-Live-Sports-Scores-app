package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylivesportsscores.data.remote.SportmonksStanding
import com.example.mylivesportsscores.ui.viewmodel.StandingsUiState
import com.example.mylivesportsscores.ui.viewmodel.StandingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(viewModel: StandingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Leagues & Standings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is StandingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StandingsUiState.Success -> {
                    StandingsContent(standingsMap = state.standingsMap)
                }
                is StandingsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadStandings() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsContent(standingsMap: Map<String, List<SportmonksStanding>>) {
    val leagueNames = standingsMap.keys.toList()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 16.dp) {
            leagueNames.forEachIndexed { index, name ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(name) }
                )
            }
        }

        val currentLeagueStandings = standingsMap[leagueNames[selectedTabIndex]] ?: emptyList()
        LeagueTable(standings = currentLeagueStandings)
    }
}

@Composable
fun LeagueTable(standings: List<SportmonksStanding>) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "#", weight = 0.1f, isHeader = true)
            TableCell(text = "TEAM", weight = 0.4f, isHeader = true, textAlign = TextAlign.Start)
            TableCell(text = "P", weight = 0.1f, isHeader = true)
            TableCell(text = "W", weight = 0.1f, isHeader = true)
            TableCell(text = "GD", weight = 0.15f, isHeader = true)
            TableCell(text = "PTS", weight = 0.15f, isHeader = true)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(standings.sortedBy { it.position }) { standing ->
                StandingRow(standing)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun StandingRow(standing: SportmonksStanding) {
    val played = standing.details.find { it.type?.code == "played" }?.value ?: 0
    val won = standing.details.find { it.type?.code == "won" }?.value ?: 0
    val goalsFor = standing.details.find { it.type?.code == "goals-for" }?.value ?: 0
    val goalsAgainst = standing.details.find { it.type?.code == "goals-against" }?.value ?: 0
    val gd = goalsFor - goalsAgainst

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = standing.position.toString(), weight = 0.1f, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.weight(0.4f), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = standing.participant?.image_path,
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = standing.participant?.name ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        
        TableCell(text = played.toString(), weight = 0.1f)
        TableCell(text = won.toString(), weight = 0.1f)
        TableCell(text = (if (gd > 0) "+$gd" else gd.toString()), weight = 0.15f)
        TableCell(text = standing.points.toString(), weight = 0.15f, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight? = null,
    color: Color? = null
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = textAlign,
        style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight ?: if (isHeader) FontWeight.Bold else FontWeight.Normal,
        color = color ?: if (isHeader) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
}
