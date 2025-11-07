package com.example.face_recognition

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(private val auth: FirebaseAuth,
                                         private val firestore: FirebaseFirestore
) {




    //ACCOUNT CREATION
    suspend fun signUp(
        email: String,
        password: String,

        name: String
    ): Result<Boolean> =
        try {
            Log.d("UserRepository", "Starting sign-up with email: $email")

            auth.createUserWithEmailAndPassword(email, password).await()

            Log.d("UserRepository", "User created in Firebase Auth")

            val uid = auth.currentUser?.uid
            Log.d("UserRepository", "Fetched UID: $uid")

            if (uid == null) throw Exception("User UID is null after sign-up")

            val user = User( name, email)
            Log.d("UserRepository", "Creating user object: $user")

            saveUserToFirestore(uid, user )  // if it reaches here, you’ll see the saveUser log
            Log.d("UserRepository", "Completed saveUserToFirestore")

            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }

    suspend fun login(email: String, password: String): Result<Boolean> =
        try {
            auth.signInWithEmailAndPassword(email, password).await()


            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }


    private suspend fun saveUserToFirestore(uid:String,user: User) {
        val docRef = firestore.collection("users").document(uid)
        docRef.set(user).await()

        Log.d("UserRepository", "Saving user to Firestore: $user with UID: $uid")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCurrentUser(): Result<User?> = try {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val snapshot = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            var user: User? = snapshot.toObject(User::class.java)

            if (user == null) {
                // Missing document → create default user
                user = User(name = "", email = firebaseUser.email ?: "")
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
                Log.d("UserRepository", "Created missing Firestore user: $user")
            }

            Result.Success(user)
        } else {
            Result.Success(null)
        }
    } catch (e: Exception) {
        Log.e("UserRepository", "Error in getCurrentUser", e)
        Result.Error(e)
    }


    suspend fun logOut(){
        auth.signOut()
    }

}


