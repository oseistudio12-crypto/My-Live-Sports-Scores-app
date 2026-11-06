package com.example.mylivesportsscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylivesportsscores.data.repository.AuthManager
import com.example.mylivesportsscores.data.repository.FirestoreRepository
import com.example.mylivesportsscores.ui.screens.MainScreen
import com.example.mylivesportsscores.ui.screens.SignInScreen
import com.example.mylivesportsscores.ui.theme.MyLiveSportsScoresTheme
import com.example.mylivesportsscores.ui.viewmodel.AuthViewModel
import com.example.mylivesportsscores.ui.viewmodel.FavoritesViewModel
import com.example.mylivesportsscores.ui.viewmodel.HomeViewModel
import com.example.mylivesportsscores.util.configureAppCheck
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mylivesportsscores.data.repository.MultiSportRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Configure App Check before any UI or Firebase operations
        configureAppCheck(this, intent)
        
        enableEdgeToEdge()
        
        val authManager = AuthManager(applicationContext)
        val firestoreRepository = FirestoreRepository(applicationContext)
        

        val multiSportRepository = MultiSportRepository()

        setContent {
            MyLiveSportsScoresTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return AuthViewModel(authManager) as T
                            }
                        }
                    )

                    AppContent(
                        authViewModel = authViewModel,
                        firestoreRepository = firestoreRepository,
                        multiSportRepository = multiSportRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    authViewModel: AuthViewModel,
    firestoreRepository: FirestoreRepository,
    multiSportRepository: MultiSportRepository
) {
    val user by authViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn()
    }

    if (user == null) {
        SignInScreen(viewModel = authViewModel)
    } else {
        val homeViewModel: HomeViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(multiSportRepository) as T
                }
            }
        )

        val favoritesViewModel: FavoritesViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FavoritesViewModel(firestoreRepository) as T
                }
            }
        )

        MainScreen(
            authViewModel = authViewModel,
            homeViewModel = homeViewModel,
            favoritesViewModel = favoritesViewModel
        )
    }
}
