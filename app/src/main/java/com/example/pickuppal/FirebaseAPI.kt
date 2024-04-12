import android.graphics.Bitmap
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserData
import com.example.pickuppal.UserStatistics
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

class FirebaseAPI {

    private val TAG = "firebaseAPI"
    private val db = Firebase.database.reference

    fun getDB(): DatabaseReference {
        return db
    }

    fun deletePostingData(userData: UserData, dataId: String) {
        val postingDataRef = db.child("posting_data").child(dataId)

        postingDataRef.removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Posting data deleted successfully")
                getUserStatistics(userData, object : UserStatisticsCallback {
                    override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                        val updatedStatistics = userStatistics.copy(
                            numItemsPosted = userStatistics.numItemsPosted - 1
                        )
                        updateUserStatistics(updatedStatistics)
                    }

                    override fun onUserStatisticsError(e: Exception) {
                        Log.e(TAG, "Error retrieving user statistics", e)
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting posting data", e)

            }
    }

    fun uploadPostingData(data: PostingData, userData: UserData) {
        db.child("posting_data").child(data.postID).updateChildren(data.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Added posting data")
                getUserStatistics(userData, object : UserStatisticsCallback {
                    override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                        val updatedStatistics = userStatistics.copy(
                            numItemsPosted = userStatistics.numItemsPosted + 1
                        )
                        updateUserStatistics(updatedStatistics)
                    }

                    override fun onUserStatisticsError(e: Exception) {
                        Log.e(TAG, "Error retrieving user statistics", e)
                    }
                })

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding posting data", e)
            }
    }

    fun uploadImage(bitmap: Bitmap, imageName: String, callback: (String?) -> Unit) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imagesRef = storageRef.child("images")
        val imageRef = imagesRef.child(imageName)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                Log.d(TAG, "Image uploaded successfully. URL: $imageUrl")
                callback(imageUrl)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Error getting download URL: ${exception.message}")
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error uploading image: ${exception.message}")
            callback(null)
        }
    }

    fun updateUserStatistics(data: UserStatistics) {
        db.child("user_statistics").child(data.userID).updateChildren(data.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Update user statistics")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user statistics", e)
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

    fun getPostingDataList(data: UserData, callback: PostingDataListCallBack) {
        val userId = data.userId
        val postingDataRef = db.child("posting_data")

        postingDataRef.orderByChild("userID").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val postingDataList = mutableListOf<PostingData>()

                    for (postSnapshot in dataSnapshot.children) {
                        val postId = postSnapshot.key
                        val postData = postSnapshot.getValue(PostingData::class.java)

                        if (postId != null && postData != null) {
                            val postingData = PostingData(
                                postID = postId,
                                userID = postData.userID,
                                title = postData.title,
                                location = postData.location,
                                lat = postData.lat,
                                lng = postData.lng,
                                reverseGeocodedAddress = postData.reverseGeocodedAddress,
                                description = postData.description,
                                claimed = postData.claimed,
                                photoUrl = postData.photoUrl
                            )
                            postingDataList.add(postingData)
                        }
                    }

                    callback.onPostingDataListReceived(postingDataList)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error retrieving posting data list", databaseError.toException())
                    callback.onPostingDataListError(databaseError.toException())
                }
            })
    }
}

    interface PostingDataListCallBack {
        fun onPostingDataListReceived(postingDataList: List<PostingData>)
        fun onPostingDataListError(e: Exception)
    }

    interface UserStatisticsCallback {
        fun onUserStatisticsReceived(userStatistics: UserStatistics)
        fun onUserStatisticsError(e: Exception)
    }
