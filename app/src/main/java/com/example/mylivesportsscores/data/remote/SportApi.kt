package com.example.mylivesportsscores.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SportmonksResponse<T>(
    @Json(name = "data") val data: T
)

@JsonClass(generateAdapter = true)
data class FixtureData(
    val id: Long,
    val name: String,
    val starting_at: String?,
    val result_info: String?,
    val state: StateData?,
    val league: LeagueData?,
    val participants: List<ParticipantData>?,
    val scores: List<ScoreData>?,
    val events: List<EventData>?
)

@JsonClass(generateAdapter = true)
data class StateData(
    val id: Int?,
    val name: String?,
    val short_name: String?
)

@JsonClass(generateAdapter = true)
data class LeagueData(
    val id: Long,
    val name: String,
    val image_path: String?
)

@JsonClass(generateAdapter = true)
data class ParticipantData(
    val id: Long,
    val name: String,
    val image_path: String?,
    val meta: ParticipantMeta?
)

@JsonClass(generateAdapter = true)
data class ParticipantMeta(
    val location: String? // home or away
)

@JsonClass(generateAdapter = true)
data class ScoreData(
    val id: Long,
    val score: ScoreValue?,
    val description: String? // CURRENT, FINAL, etc.
)

@JsonClass(generateAdapter = true)
data class ScoreValue(
    val goals: Int?,
    val participant_id: Long?
)

@JsonClass(generateAdapter = true)
data class EventData(
    val id: Long,
    val type_id: Long?,
    val minute: Int?,
    val extra_minute: Int?,
    val participant_id: Long?
)

interface SportApi {
    @GET("api/sports/livescores")
    suspend fun getLiveScores(): SportmonksResponse<List<FixtureData>>

    @GET("api/sports/fixtures/today")
    suspend fun getFixturesToday(): SportmonksResponse<List<FixtureData>>

    @GET("api/sports/fixtures")
    suspend fun getFixtures(): SportmonksResponse<List<FixtureData>>

    @GET("api/sports/standings")
    suspend fun getStandings(): SportmonksResponse<List<Map<String, Any>>>

    @GET("api/sports/leagues")
    suspend fun getLeagues(): SportmonksResponse<List<Map<String, Any>>>

    @GET("api/sports/teams")
    suspend fun getTeams(): SportmonksResponse<List<Map<String, Any>>>

    @GET("api/sports/players")
    suspend fun getPlayers(): SportmonksResponse<List<Map<String, Any>>>

    @GET("api/sports/top-scorers")
    suspend fun getTopScorers(): SportmonksResponse<List<Map<String, Any>>>

    @GET("api/sports/health")
    suspend fun checkHealth(): Map<String, String>
}
