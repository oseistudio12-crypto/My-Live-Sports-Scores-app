package com.example.mylivesportsscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SportApi {
    @GET("api/sports/livescores")
    suspend fun getLiveScores(): SportResponse

    @GET("api/sports/fixtures")
    suspend fun getFixtures(): SportResponse

    @GET("api/sports/fixtures/{id}")
    suspend fun getFixtureDetail(@Path("id") id: String): FixtureDetailResponse

    @GET("api/sports/leagues")
    suspend fun getLeagues(): LeagueResponse

    @GET("api/sports/standings")
    suspend fun getStandings(): StandingResponse

    @GET("api/sports/teams")
    suspend fun getTeams(): TeamResponse
}

data class SportResponse(
    val data: List<SportmonksFixture> = emptyList()
)

data class SportmonksFixture(
    val id: Long,
    val name: String,
    val starting_at: String,
    val result_info: String?,
    val league: SportmonksLeague?,
    val participants: List<SportmonksParticipant> = emptyList(),
    val scores: List<SportmonksScore> = emptyList(),
    val events: List<SportmonksEvent> = emptyList()
)

data class SportmonksLeague(
    val id: Long,
    val name: String,
    val image_path: String?
)

data class SportmonksParticipant(
    val id: Long,
    val name: String,
    val image_path: String?,
    val meta: ParticipantMeta?
)

data class ParticipantMeta(
    val location: String? // home or away
)

data class SportmonksScore(
    val id: Long,
    val score: ScoreValue?,
    val description: String?
)

data class ScoreValue(
    val goals: Int?
)

data class SportmonksEvent(
    val id: Long,
    val type_id: Int?,
    val player_name: String?,
    val minute: Int?
)

data class LeagueResponse(val data: List<SportmonksLeague>)
data class FixtureDetailResponse(val data: SportmonksFixture)
data class StandingResponse(val data: List<Any>) // Generic for now
data class TeamResponse(val data: List<Any>) // Generic for now
