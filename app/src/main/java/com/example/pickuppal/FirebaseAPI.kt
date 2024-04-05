import android.util.Log
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserData
import com.example.pickuppal.UserStatistics
import com.google.firebase.Firebase
import com.google.firebase.database.database

class FirebaseAPI {

    val TAG = "FirebaseAPI"
    private val db = Firebase.database.reference

    fun uploadPostingData(data : PostingData) {
        db.child("posting_data").child(data.postID).updateChildren(data.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Added posting data")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding posting data", e)
            }
    }

    fun getUserStatistics(data: UserData, callback: UserStatisticsCallback) {
        Log.d(TAG, "get user statistics called")
        val userId = data.userId
        db.child("user_statistics").child(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                Log.d(TAG, "get user statistics called!!")
                if (dataSnapshot.exists()) {
                    val userStatistics = dataSnapshot.getValue(UserStatistics::class.java)
                    if (userStatistics != null) {
                        callback.onUserStatisticsReceived(userStatistics)
                    } else {
                        Log.e(TAG, "User statistics is null")
                        callback.onUserStatisticsError(Exception("User statistics is null"))
                    }
                } else {
                    val defaultStatistics = UserStatistics(
                        userID = userId,
                        averageRating = 5.0f,
                        numItemsPosted = 0,
                        numItemsClaimed = 0
                    )
                    db.child("user_statistics").child(userId).setValue(defaultStatistics)
                        .addOnSuccessListener {
                            Log.d(TAG, "Default user statistics created")
                            callback.onUserStatisticsReceived(defaultStatistics)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating default user statistics", e)
                            callback.onUserStatisticsError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "get user statistics called :(")
                Log.e(TAG, "Error retrieving user statistics", e)
                callback.onUserStatisticsError(e)
            }
    }

    fun updateUserStatistics(data : UserStatistics) {
        db.child("user_statistics").child(data.userID).updateChildren(data.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Update user statistics")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user statistics", e)
            }
    }


}

interface UserStatisticsCallback {
    fun onUserStatisticsReceived(userStatistics: UserStatistics)
    fun onUserStatisticsError(e: Exception)
}
