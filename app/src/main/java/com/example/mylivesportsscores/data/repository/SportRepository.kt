package com.example.mylivesportsscores.data.repository

import android.util.Log
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.data.remote.SportApi
import com.example.mylivesportsscores.data.remote.FixtureData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SportRepository"

class SportRepository(private val api: SportApi) {

    suspend fun getLiveScores(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLiveScores()
            response.data.map { mapToSportMatch(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live scores", e)
            throw e
        }
    }

    suspend fun getFixtures(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixtures()
            response.data.map { mapToSportMatch(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fixtures", e)
            throw e
        }
    }

    suspend fun getStandings(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        api.getStandings().data
    }

    suspend fun getLeagues(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        api.getLeagues().data
    }

    suspend fun getTeams(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        api.getTeams().data
    }

    private fun mapToSportMatch(fixture: FixtureData): SportMatch {
        val homeTeam = fixture.participants?.find { it.meta?.location == "home" }?.name ?: "Unknown"
        val awayTeam = fixture.participants?.find { it.meta?.location == "away" }?.name ?: "Unknown"
        
        val homeScore = fixture.scores?.find { 
            it.description == "CURRENT" && it.score?.participant_id == fixture.participants?.find { p -> p.meta?.location == "home" }?.id 
        }?.score?.goals?.toString() ?: "0"
        
        val awayScore = fixture.scores?.find { 
            it.description == "CURRENT" && it.score?.participant_id == fixture.participants?.find { p -> p.meta?.location == "away" }?.id 
        }?.score?.goals?.toString() ?: "0"

        return SportMatch(
            id = fixture.id.toString(),
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            status = fixture.result_info ?: "Scheduled",
            timestamp = 0, // Should parse starting_at if needed
            league = fixture.league?.name ?: "Unknown",
            sport = "Soccer"
        )
    }
}
