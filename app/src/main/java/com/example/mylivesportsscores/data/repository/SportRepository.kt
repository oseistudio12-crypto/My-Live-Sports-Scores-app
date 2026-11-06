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

    suspend fun getFixturesToday(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixturesToday()
            response.data.map { mapToSportMatch(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching today's fixtures", e)
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
        val participants = fixture.participants ?: emptyList()
        val homeParticipant = participants.find { it.meta?.location == "home" }
        val awayParticipant = participants.find { it.meta?.location == "away" }
        
        val homeTeam = homeParticipant?.name ?: "Unknown"
        val awayTeam = awayParticipant?.name ?: "Unknown"
        
        val scores = fixture.scores ?: emptyList()
        
        // Find current scores for home and away
        val homeScore = homeParticipant?.let { home ->
            scores.find { it.description == "CURRENT" && it.score?.participant_id == home.id }?.score?.goals
        }
        
        val awayScore = awayParticipant?.let { away ->
            scores.find { it.description == "CURRENT" && it.score?.participant_id == away.id }?.score?.goals
        }

        return SportMatch(
            id = fixture.id.toString(),
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            status = fixture.state?.name ?: fixture.state?.short_name ?: "NS",
            startingAt = fixture.starting_at ?: "",
            league = fixture.league?.name ?: "Unknown",
            sport = "Soccer"
        )
    }
}
