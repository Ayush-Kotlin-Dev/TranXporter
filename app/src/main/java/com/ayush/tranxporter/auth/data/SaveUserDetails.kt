package com.ayush.tranxporter.auth.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository (
    private val firestore: FirebaseFirestore
)  {

    suspend fun saveUserDetails(userId: String, userData: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun isUserProfileComplete(userId: String): Boolean {
//        return try {
//            val document = firestore.collection("users")
//                .document(userId)
//                .get()
//                .await()
//
//            document.exists()
//        } catch (e: Exception) {
//            false
//        }
//    }
}