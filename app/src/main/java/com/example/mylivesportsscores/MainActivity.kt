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
import com.example.mylivesportsscores.data.repository.AiRepository
import com.example.mylivesportsscores.util.configureAppCheck
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mylivesportsscores.data.repository.SportRepository
import com.example.mylivesportsscores.data.remote.SportApi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Configure App Check before any UI or Firebase operations
        configureAppCheck(this, intent)
        
        enableEdgeToEdge()
        
        val authManager = AuthManager(applicationContext)
        val firestoreRepository = FirestoreRepository(applicationContext)
        
        // Secure Backend Integration
        val backendUrl = if (BuildConfig.BACKEND_URL.startsWith("https")) {
            BuildConfig.BACKEND_URL
        } else {
            "https://my-live-sports-scores-app.onrender.com/" // User's Render URL
        }
        
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(backendUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        val sportApi = retrofit.create(SportApi::class.java)
        val sportRepository = SportRepository(sportApi)
        val aiRepository = AiRepository()

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
                        sportRepository = sportRepository,
                        aiRepository = aiRepository
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
    sportRepository: SportRepository,
    aiRepository: AiRepository
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
                    return HomeViewModel(sportRepository) as T
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
            favoritesViewModel = favoritesViewModel,
            sportRepository = sportRepository,
            aiRepository = aiRepository
        )
    }
}
