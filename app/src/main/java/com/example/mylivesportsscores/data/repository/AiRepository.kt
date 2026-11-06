package com.example.mylivesportsscores.data.repository

import android.util.Log
import com.example.mylivesportsscores.data.model.SportMatch
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"

class AiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun getMatchSummary(match: SportMatch): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze this football match and provide a brief, professional summary.
                Match: ${match.homeTeam} vs ${match.awayTeam}
                Score: ${match.homeScore ?: 0} - ${match.awayScore ?: 0}
                Status: ${match.status}
                League: ${match.league}
                
                Focus on performance, context (e.g., if it's a high-scoring game), and potential implications.
                Do NOT invent events that are not implied by the score.
                Keep it under 3 sentences.
            """.trimIndent()

            val response = withTimeout(30_000L) {
                model.generateContent(prompt)
            }

            response.text ?: "Summary unavailable."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate match summary", e)
            "Could not generate summary at this time."
        }
    }
}
