package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylivesportsscores.data.model.NewsArticle
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.ui.viewmodel.HomeUiState
import com.example.mylivesportsscores.ui.viewmodel.HomeViewModel
import com.example.mylivesportsscores.ui.viewmodel.MatchFilter
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    favorites: List<String>,
    onToggleFavorite: (String, String, String, String?) -> Unit,
    onMatchClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            placeholder = { Text("Search teams or leagues...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            "OSEI LIVE",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive 
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    }) {
                        Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }
                    if (!isSearchActive) {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            DateCalendar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setDate(it) }
            )
            
            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is HomeUiState.Success -> {
                        if (state.filteredMatches.isEmpty()) {
                            EmptyHomeContent(onRefresh = { viewModel.refreshData() })
                        } else {
                            HomeContent(
                                matches = state.filteredMatches,
                                news = state.news,
                                favorites = favorites,
                                onToggleFavorite = onToggleFavorite,
                                onMatchClick = onMatchClick
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
}

@Composable
fun DateCalendar(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    val dates = remember {
        (-3..3).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates) { date ->
            val isSelected = isSameDay(date, selectedDate)
            val dayName = SimpleDateFormat("EEE", Locale.US).format(date.time)
            val dayNumber = SimpleDateFormat("dd", Locale.US).format(date.time)

            Card(
                onClick = { onDateSelected(date) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.width(60.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FilterRow(
    selectedFilter: MatchFilter,
    onFilterSelected: (MatchFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MatchFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.name) }
            )
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
        Text("No matches found for this date or filter.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

@Composable
fun HomeContent(
    matches: List<SportMatch>,
    news: List<NewsArticle>,
    favorites: List<String>,
    onToggleFavorite: (String, String, String, String?) -> Unit,
    onMatchClick: (String) -> Unit
) {
    val groupedMatches = matches.groupBy { it.league }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedMatches.forEach { (league, leagueMatches) ->
            item {
                val firstMatch = leagueMatches.firstOrNull()
                CompetitionHeader(league, firstMatch?.leagueLogoUrl, firstMatch?.countryLogoUrl)
            }
            items(leagueMatches) { match ->
                ProfessionalMatchCard(
                    match = match,
                    isFavorite = favorites.contains("${match.homeTeam}_vs_${match.awayTeam}"),
                    onToggleFavorite = {
                        onToggleFavorite("match", "${match.homeTeam}_vs_${match.awayTeam}", "${match.homeTeam} vs ${match.awayTeam}", match.homeLogoUrl)
                    },
                    onClick = { onMatchClick(match.id) }
                )
            }
        }
    }
}

@Composable
fun CompetitionHeader(league: String, logoUrl: String?, countryLogoUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (countryLogoUrl != null) {
            AsyncImage(
                model = countryLogoUrl,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = league,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProfessionalMatchCard(
    match: SportMatch,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLive = match.status.contains("Live", ignoreCase = true) || match.status.contains("INPLAY", ignoreCase = true)
                
                Surface(
                    color = if (isLive) Color.Red else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = match.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = match.homeLogoUrl,
                        contentDescription = "Home Logo",
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.homeTeam,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Score or Time
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.homeScore != null && match.awayScore != null) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = match.startingAt.takeLast(5),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Away Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = match.awayLogoUrl,
                        contentDescription = "Away Logo",
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.awayTeam,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
