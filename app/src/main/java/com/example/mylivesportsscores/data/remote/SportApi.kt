package com.example.mylivesportsscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Node.js backend proxying Sportmonks v3 Football API.
 */
interface SportApi {

    /**
     * Fetches all current in-play fixtures.
     * Maps to Sportmonks v3 /livescores
     */
    @GET("api/sports/livescores")
    suspend fun getLiveScores(): SportResponse

    /**
     * Fetches today's fixtures (scheduled and completed).
     * Maps to Sportmonks v3 /fixtures
     */
    @GET("api/sports/fixtures")
    suspend fun getFixtures(@Query("date") date: String? = null): SportResponse

    /**
     * Fetches detailed information for a specific fixture.
     * Maps to Sportmonks v3 /fixtures/{id}
     */
    @GET("api/sports/fixtures/{id}")
    suspend fun getFixtureDetail(@Path("id") id: String): FixtureDetailResponse

    /**
     * Fetches accessible leagues.
     */
    @GET("api/sports/leagues")
    suspend fun getLeagues(): LeagueResponse

    /**
     * Fetches league standings.
     */
    @GET("api/sports/standings")
    suspend fun getStandings(): StandingResponse

    /**
     * Fetches standings for a specific league.
     */
    @GET("api/sports/standings/leagues/{id}")
    suspend fun getStandingsByLeague(@Path("id") id: String): StandingResponse

    /**
     * Fetches teams.
     */
    @GET("api/sports/teams")
    suspend fun getTeams(): TeamResponse
}

/**
 * Top-level response for multiple fixtures.
 */
data class SportResponse(
    val data: List<SportmonksFixture> = emptyList()
)

/**
 * Top-level response for a single fixture detail.
 */
data class FixtureDetailResponse(
    val data: SportmonksFixture
)

/**
 * Core model for a football match in Sportmonks v3.
 */
data class SportmonksFixture(
    val id: Long,
    val name: String,
    val starting_at: String,
    val result_info: String?,
    val league: SportmonksLeague?,
    val participants: List<SportmonksParticipant> = emptyList(),
    val scores: List<SportmonksScore> = emptyList(),
    val events: List<SportmonksEvent> = emptyList(),
    val statistics: List<SportmonksStatistic> = emptyList(),
    val lineups: List<SportmonksLineup> = emptyList(),
    val formations: List<SportmonksFormation> = emptyList(),
    val metadata: Map<String, Any>? = null
)

data class SportmonksFormation(
    val id: Long,
    val participant_id: Long,
    val formation: String?
)

data class SportmonksStatistic(
    val id: Long,
    val participant_id: Long,
    val type_id: Int,
    val data: StatisticData?
)

data class StatisticData(
    val value: Any?,
    val type: StatisticType?
)

data class StatisticType(
    val id: Int,
    val code: String,
    val name: String
)

data class SportmonksLineup(
    val id: Long,
    val participant_id: Long,
    val player_id: Long?,
    val player_name: String?,
    val jersey_number: Int?,
    val position_id: Int?,
    val formation_position: Int?,
    val type_id: Int? // 11: starter, 12: substitute
)

/**
 * Model for a league/competition.
 */
data class SportmonksLeague(
    val id: Long,
    val name: String,
    val image_path: String? = null,
    val type: String? = null,
    val country: SportmonksCountry? = null
)

data class SportmonksCountry(
    val id: Long,
    val name: String,
    val image_path: String? = null
)

/**
 * Model for a team/participant.
 */
data class SportmonksParticipant(
    val id: Long,
    val name: String,
    val image_path: String? = null,
    val meta: ParticipantMeta? = null
)

/**
 * Metadata for a participant, defining if they are home or away.
 */
data class ParticipantMeta(
    val location: String? = null // "home" or "away"
)

/**
 * Model for a score entry (Current, Half-time, etc.).
 */
data class SportmonksScore(
    val id: Long,
    val score: ScoreValue? = null,
    val description: String? = null, // e.g., "CURRENT", "1ST_HALF"
    val participant_id: Long? = null
)

/**
 * The actual score value.
 */
data class ScoreValue(
    val goals: Int? = null
)

/**
 * Model for a match event (Goal, Card, Substitution).
 */
data class SportmonksEvent(
    val id: Long,
    val type_id: Int? = null,
    val player_name: String? = null,
    val minute: Int? = null,
    val participant_id: Long? = null
)

/**
 * Response wrappers for other entities.
 */
data class LeagueResponse(val data: List<SportmonksLeague>)
data class StandingResponse(val data: List<SportmonksStanding>)

data class SportmonksStanding(
    val position: Int,
    val points: Int,
    val participant_id: Long,
    val participant: StandingParticipant?,
    val details: List<StandingDetail> = emptyList()
)

data class StandingParticipant(
    val id: Long,
    val name: String,
    val image_path: String?
)

data class StandingDetail(
    val type_id: Int,
    val value: Int,
    val type: StandingDetailType?
)

data class StandingDetailType(
    val id: Int,
    val code: String,
    val name: String
)

data class TeamResponse(val data: List<Any>)
