import android.graphics.Bitmap
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserData
import com.example.pickuppal.UserStatistics
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

// Class that connects to our Firebase database
class FirebaseAPI {
    private val db = Firebase.database.reference

    // function that returns reference to the firebase database
    fun getDB(): DatabaseReference {
        return db
    }

    // function that deletes posting data from the database when given the data and id
    fun deletePostingData(userData: UserData, dataId: String) {
        val postingDataRef = db.child("posting_data").child(dataId)

        postingDataRef.removeValue()
            .addOnSuccessListener {
                getUserStatistics(userData, object : UserStatisticsCallback {
                    override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                        val updatedStatistics = userStatistics.copy(
                            numItemsPosted = userStatistics.numItemsPosted - 1
                        )
                        updateUserStatistics(updatedStatistics)
                    }

                    override fun onUserStatisticsError(e: Exception) {
                    }
                })
            }
    }

    // function that uploads posting data to posting_data within our realtime database
    fun uploadPostingData(data: PostingData, userData: UserData) {
        db.child("posting_data").child(data.postID).updateChildren(data.toMap())
            .addOnSuccessListener {
                getUserStatistics(userData, object : UserStatisticsCallback {
                    override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                        val updatedStatistics = userStatistics.copy(
                            numItemsPosted = userStatistics.numItemsPosted + 1
                        )
                        updateUserStatistics(updatedStatistics)
                    }

                    override fun onUserStatisticsError(e: Exception) {
                    }
                })

            }
    }

    // function that is called when a user claims an item
    fun claimItem(claimingUser: UserData) {
        getUserStatistics(claimingUser, object : UserStatisticsCallback {
            override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                val updatedStatistics = userStatistics.copy(
                    numItemsClaimed = userStatistics.numItemsPosted + 1
                )
                updateUserStatistics(updatedStatistics)
            }

            override fun onUserStatisticsError(e: Exception) {
            }
        })

    }

    // function that is called to upload an image of an item to posting_data in the realtime database
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
                callback(imageUrl)
            }.addOnFailureListener { exception ->
                callback(null)
            }
        }.addOnFailureListener { exception ->
            callback(null)
        }
    }

    // function that updates user_statistics in the realtime database
    fun updateUserStatistics(data: UserStatistics) {
        db.child("user_statistics").child(data.userID).updateChildren(data.toMap())
    }

    // function that retrieves user statistics from the database using a user's id
    fun getUserStatistics(userId: String, callback: UserStatisticsCallback) {
        db.child("user_statistics").child(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val userStatistics = dataSnapshot.getValue(UserStatistics::class.java)
                    if (userStatistics != null) {
                        callback.onUserStatisticsReceived(userStatistics)
                    } else {
                        callback.onUserStatisticsError(Exception("User statistics is null"))
                    }
                } else {
                    val defaultStatistics = UserStatistics(
                        userID = userId,
                        numItemsPosted = 0,
                        numItemsClaimed = 0,
                        numRatings = 0,
                        totalRating = 0,
                    )
                    db.child("user_statistics").child(userId).setValue(defaultStatistics)
                        .addOnSuccessListener {
                            callback.onUserStatisticsReceived(defaultStatistics)
                        }
                        .addOnFailureListener { e ->
                            callback.onUserStatisticsError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                callback.onUserStatisticsError(e)
            }
    }
    // function that retrieves user statistics from the database using the user data
    fun getUserStatistics(data: UserData, callback: UserStatisticsCallback) {
        val userId = data.userId
        db.child("user_statistics").child(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val userStatistics = dataSnapshot.getValue(UserStatistics::class.java)
                    if (userStatistics != null) {
                        callback.onUserStatisticsReceived(userStatistics)
                    } else {
                        callback.onUserStatisticsError(Exception("User statistics is null"))
                    }
                } else {
                    val defaultStatistics = UserStatistics(
                        userID = userId,
                        numItemsPosted = 0,
                        numItemsClaimed = 0,
                        numRatings = 0,
                        totalRating = 0,
                    )
                    db.child("user_statistics").child(userId).setValue(defaultStatistics)
                        .addOnSuccessListener {
                            callback.onUserStatisticsReceived(defaultStatistics)
                        }
                        .addOnFailureListener { e ->
                            callback.onUserStatisticsError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                callback.onUserStatisticsError(e)
            }
    }

    // function that is called when a user wants to submit a rating
    fun submitRating(postingData: PostingData, rating: Int) {

        db.child("user_statistics").child(postingData.userID).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val userStats = currentData.getValue(UserStatistics::class.java)
                if (userStats != null) {
                    val updatedTotalRating = userStats.totalRating + rating
                    val updatedNumRatings = userStats.numRatings + 1
                    currentData.child("totalRating").value = updatedTotalRating
                    currentData.child("numRatings").value = updatedNumRatings
                } else {
                    // User statistics don't exist, create new entry
                    val newUserStats = UserStatistics(totalRating = rating, numRatings = 1)
                    currentData.value = newUserStats
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                //doesn't need to do anything
            }
        })
    }

    // function that retrieves all of the data within posting_data and sets postingDataList equivalent to it
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
                                photoUrl = postData.photoUrl,
                                claimedBy = postData.claimedBy,
                                rating = postData.rating
                            )
                            postingDataList.add(postingData)
                        }
                    }

                    callback.onPostingDataListReceived(postingDataList)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    callback.onPostingDataListError(databaseError.toException())
                }
            })
    }
}

// callback for posting data list
interface PostingDataListCallBack {
    fun onPostingDataListReceived(postingDataList: List<PostingData>)
    fun onPostingDataListError(e: Exception)
}

// callback for user statistics
interface UserStatisticsCallback {
    fun onUserStatisticsReceived(userStatistics: UserStatistics)
    fun onUserStatisticsError(e: Exception)
}
