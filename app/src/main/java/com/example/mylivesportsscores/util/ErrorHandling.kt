package com.example.mylivesportsscores.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(e: Exception, operation: OperationType, path: String) {
    val firestoreError = generateSequence(e as Throwable) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()

    val logMessage = if (firestoreError != null) {
        "Firestore Error [${operation.name}] at $path: ${firestoreError.code} - ${firestoreError.message}"
    } else {
        "Unknown Error [${operation.name}] at $path: ${e.message}"
    }

    Log.e("FirestoreError", logMessage, e)
}
