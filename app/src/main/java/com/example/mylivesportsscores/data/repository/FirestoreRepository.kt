package com.example.mylivesportsscores.data.repository

import android.content.Context
import com.example.mylivesportsscores.R
import com.example.mylivesportsscores.data.model.Favorite
import com.example.mylivesportsscores.data.model.UserProfile
import com.example.mylivesportsscores.util.OperationType
import com.example.mylivesportsscores.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(context: Context) {
    private val db = Firebase.firestore(context.getString(R.string.firestore_database_id))
    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in")
    }

    fun getUserProfile(): Flow<UserProfile?> {
        val uid = requireUserId()
        return db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject<UserProfile>() }
    }

    suspend fun createUserProfile() {
        val uid = requireUserId()
        val profile = UserProfile(
            userId = uid,
            favoriteTeams = emptyList(),
            favoriteLeagues = emptyList(),
            notificationsEnabled = true
        )
        try {
            db.collection("users").document(uid).set(profile).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid")
            throw e
        }
    }

    fun getFavorites(): Flow<List<Favorite>> {
        val uid = requireUserId()
        return db.collection("favorites")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects<Favorite>() }
    }

    suspend fun addFavorite(type: String, targetId: String, name: String, imageUrl: String?) {
        val uid = requireUserId()
        val favoriteId = "${uid}_${type}_${targetId}"
        val favorite = Favorite(
            favoriteId = favoriteId,
            userId = uid,
            type = type,
            targetId = targetId,
            name = name,
            imageUrl = imageUrl
        )
        try {
            db.collection("favorites").document(favoriteId).set(favorite).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "favorites/$favoriteId")
            throw e
        }
    }

    suspend fun removeFavorite(favoriteId: String) {
        try {
            db.collection("favorites").document(favoriteId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "favorites/$favoriteId")
            throw e
        }
    }
}
