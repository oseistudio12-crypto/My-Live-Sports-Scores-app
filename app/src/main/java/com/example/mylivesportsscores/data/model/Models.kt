package com.example.mylivesportsscores.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val favoriteTeams: List<String> = emptyList(),
    val favoriteLeagues: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Favorite(
    @DocumentId val favoriteId: String = "",
    val userId: String = "",
    val type: String = "", // team, player, competition
    val targetId: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class SportMatch(
    val id: String = "",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val homeScore: String = "",
    val awayScore: String = "",
    val status: String = "", // Live, Finished, Scheduled
    val timestamp: Long = 0,
    val league: String = "",
    val sport: String = ""
)

data class NewsArticle(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val imageUrl: String? = null,
    val source: String = "",
    val timestamp: Long = 0
)
