package com.example.mylivesportsscores.data.repository

import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.data.remote.SportApi
import com.example.mylivesportsscores.data.remote.SportmonksFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SportRepository(private val api: SportApi) {

    suspend fun getLiveScores(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLiveScores()
            response.data.map { it.toSportMatch() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTodayFixtures(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixtures()
            response.data.map { it.toSportMatch() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMatchDetails(matchId: String): SportmonksFixture? = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixtureDetail(matchId)
            response.data
        } catch (e: Exception) {
            null
        }
    }

    private fun SportmonksFixture.toSportMatch(): SportMatch {
        val homeParticipant = participants.find { it.meta?.location == "home" }
        val awayParticipant = participants.find { it.meta?.location == "away" }
        
        // Find current scores
        val homeScore = scores.find { it.description == "CURRENT" && it.id == homeParticipant?.id }?.score?.goals
            ?: scores.find { it.id == homeParticipant?.id }?.score?.goals
        val awayScore = scores.find { it.description == "CURRENT" && it.id == awayParticipant?.id }?.score?.goals
            ?: scores.find { it.id == awayParticipant?.id }?.score?.goals

        return SportMatch(
            id = id.toString(),
            homeTeam = homeParticipant?.name ?: "Home Team",
            awayTeam = awayParticipant?.name ?: "Away Team",
            homeScore = homeScore,
            awayScore = awayScore,
            status = if (starting_at.contains("LIVE")) "Live" else "Scheduled", // Rough mapping
            startingAt = starting_at,
            league = league?.name ?: "Unknown League",
            sport = "Football"
        )
    }
}
