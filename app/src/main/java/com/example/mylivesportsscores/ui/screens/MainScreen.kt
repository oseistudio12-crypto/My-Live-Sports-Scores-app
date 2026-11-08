package com.example.mylivesportsscores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mylivesportsscores.ui.viewmodel.AuthViewModel
import com.example.mylivesportsscores.ui.viewmodel.FavoritesViewModel
import com.example.mylivesportsscores.ui.viewmodel.HomeViewModel
import com.example.mylivesportsscores.ui.viewmodel.StandingsViewModel
import com.example.mylivesportsscores.ui.screens.StandingsScreen
import com.example.mylivesportsscores.data.repository.SportRepository
import com.example.mylivesportsscores.data.repository.AiRepository
import com.example.mylivesportsscores.ui.viewmodel.MatchDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    favoritesViewModel: FavoritesViewModel,
    standingsViewModel: StandingsViewModel,
    sportRepository: SportRepository,
    aiRepository: AiRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val favorites by favoritesViewModel.favorites.collectAsState()
    val favoriteIds = favorites.map { it.targetId }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = currentRoute == "favorites",
                    onClick = {
                        navController.navigate("favorites") {
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Standings") },
                    label = { Text("Standings") },
                    selected = currentRoute == "standings",
                    onClick = {
                        navController.navigate("standings") {
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = homeViewModel,
                    favorites = favoriteIds,
                    onToggleFavorite = { type, id, name, imageUrl ->
                        if (favoriteIds.contains(id)) {
                            val fav = favorites.find { it.targetId == id }
                            fav?.let { favoritesViewModel.removeFavorite(it.favoriteId) }
                        } else {
                            favoritesViewModel.addFavorite(type, id, name, imageUrl)
                        }
                    },
                    onMatchClick = { matchId ->
                        navController.navigate("match_detail/$matchId")
                    }
                )
            }
            composable("match_detail/{matchId}") { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                val matchDetailViewModel: MatchDetailViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MatchDetailViewModel(sportRepository, aiRepository, matchId) as T
                        }
                    }
                )
                MatchDetailScreen(
                    viewModel = matchDetailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("favorites") {
                FavoritesScreen(viewModel = favoritesViewModel)
            }
            composable("standings") {
                StandingsScreen(viewModel = standingsViewModel)
            }
            composable("profile") {
                ProfileScreen(viewModel = authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AuthViewModel) {
    val user by viewModel.user.collectAsState()
    var goalNotifications by remember { mutableStateOf(true) }
    var kickoffNotifications by remember { mutableStateOf(true) }
    var matchFinishedNotifications by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Account & Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(40.dp)), contentAlignment = Alignment.Center) {
                            Text(text = user?.displayName?.take(1) ?: "U", style = MaterialTheme.typography.displaySmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = user?.displayName ?: "User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            item {
                Text(text = "NOTIFICATION PREFERENCES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            item {
                NotificationSettingRow(title = "Goal Notifications", description = "Get notified when a goal is scored", checked = goalNotifications, onCheckedChange = { goalNotifications = it })
            }
            item {
                NotificationSettingRow(title = "Match Kickoff", description = "Get notified when a match starts", checked = kickoffNotifications, onCheckedChange = { kickoffNotifications = it })
            }
            item {
                NotificationSettingRow(title = "Match Finished", description = "Get notified when a match ends", checked = matchFinishedNotifications, onCheckedChange = { matchFinishedNotifications = it })
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun NotificationSettingRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
