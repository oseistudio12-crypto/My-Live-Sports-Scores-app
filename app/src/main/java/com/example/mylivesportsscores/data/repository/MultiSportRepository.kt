package com.example.mylivesportsscores.data.repository

import android.util.Log
import com.example.mylivesportsscores.data.model.SportMatch
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MultiSportRepository"

class MultiSportRepository {

    private val matchSchema = Schema.obj(
        properties = mapOf(
            "matches" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "homeTeam" to Schema.string(),
                        "awayTeam" to Schema.string(),
                        "homeScore" to Schema.integer(nullable = true),
                        "awayScore" to Schema.integer(nullable = true),
                        "status" to Schema.string(),
                        "startingAt" to Schema.string(),
                        "league" to Schema.string(),
                        "sport" to Schema.string()
                    )
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = matchSchema
            }
        )
    }

    suspend fun getScoresForSport(sport: String): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Generate 5 realistic live or upcoming sports match results for $sport. 
                Do NOT include any Football (Soccer) data.
                Include team names, current scores (if live), match status (e.g., 'Live', 'Scheduled', 'Finished'), 
                and a formatted starting time.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val jsonString = response.text ?: return@withContext emptyList()
            val json = JSONObject(jsonString)
            val matchesArray = json.getJSONArray("matches")
            
            val result = mutableListOf<SportMatch>()
            for (i in 0 until matchesArray.length()) {
                val obj = matchesArray.getJSONObject(i)
                result.add(
                    SportMatch(
                        id = obj.getString("id"),
                        homeTeam = obj.getString("homeTeam"),
                        awayTeam = obj.getString("awayTeam"),
                        homeScore = if (obj.isNull("homeScore")) null else obj.getInt("homeScore"),
                        awayScore = if (obj.isNull("awayScore")) null else obj.getInt("awayScore"),
                        status = obj.getString("status"),
                        startingAt = obj.getString("startingAt"),
                        league = obj.getString("league"),
                        sport = obj.getString("sport")
                    )
                )
            }
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to generate $sport scores", e)
            emptyList()
        }
    }
}
