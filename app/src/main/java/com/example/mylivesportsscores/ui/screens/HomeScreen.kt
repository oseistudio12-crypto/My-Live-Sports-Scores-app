package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mylivesportsscores.data.model.NewsArticle
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.ui.viewmodel.HomeUiState
import com.example.mylivesportsscores.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    favorites: List<String>,
    onToggleFavorite: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HomeUiState.Success -> {
                    if (state.matches.isEmpty()) {
                        EmptyHomeContent(onRefresh = { viewModel.refreshData() })
                    } else {
                        HomeContent(
                            matches = state.matches,
                            news = state.news,
                            favorites = favorites,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
                is HomeUiState.Error -> {
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
fun EmptyHomeContent(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No matches or fixtures found for today.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefresh) {
            Text("Retry")
        }
    }
}

@Composable
fun HomeContent(
    matches: List<SportMatch>,
    news: List<NewsArticle>,
    favorites: List<String>,
    onToggleFavorite: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Live & Scheduled Matches",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(matches) { match ->
            MatchCard(
                match = match,
                isFavorite = favorites.contains("${match.homeTeam}_vs_${match.awayTeam}"),
                onToggleFavorite = {
                    onToggleFavorite("match", "${match.homeTeam}_vs_${match.awayTeam}", "${match.homeTeam} vs ${match.awayTeam}")
                }
            )
        }

        if (news.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sports News",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(news) { article ->
                NewsCard(article = article)
            }
        }
    }
}

@Composable
fun MatchCard(
    match: SportMatch,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.league,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = match.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (match.status.contains("Live", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (match.startingAt.isNotEmpty()) {
                Text(
                    text = "Starts at: ${match.startingAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = match.homeTeam, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (match.homeScore != null) {
                        Text(text = match.homeScore.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Text(text = "vs", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = match.awayTeam, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (match.awayScore != null) {
                        Text(text = match.awayScore.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = article.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = article.source, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
