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

    suspend fun getFixturesByDate(date: String? = null): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixtures(date)
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

    suspend fun getStandingsByLeague(leagueId: String): List<com.example.mylivesportsscores.data.remote.SportmonksStanding> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStandingsByLeague(leagueId)
            response.data
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun SportmonksFixture.toSportMatch(): SportMatch {
        val homeParticipant = participants.find { it.meta?.location == "home" }
        val awayParticipant = participants.find { it.meta?.location == "away" }
        
        // Find current scores
        val homeScore = scores.find { it.description == "CURRENT" && it.participant_id == homeParticipant?.id }?.score?.goals
            ?: scores.find { it.participant_id == homeParticipant?.id }?.score?.goals
        val awayScore = scores.find { it.description == "CURRENT" && it.participant_id == awayParticipant?.id }?.score?.goals
            ?: scores.find { it.participant_id == awayParticipant?.id }?.score?.goals

        return SportMatch(
            id = id.toString(),
            homeTeam = homeParticipant?.name ?: "Home Team",
            awayTeam = awayParticipant?.name ?: "Away Team",
            homeScore = homeScore,
            awayScore = awayScore,
            status = mapStatus(starting_at),
            startingAt = starting_at,
            league = league?.name ?: "Unknown League",
            sport = "Football",
            homeLogoUrl = homeParticipant?.image_path,
            awayLogoUrl = awayParticipant?.image_path,
            leagueLogoUrl = league?.image_path,
            countryLogoUrl = league?.country?.image_path
        )
    }

    private fun mapStatus(startingAt: String): String {
        return if (startingAt.contains("LIVE")) "Live" else "Scheduled"
    }
}
