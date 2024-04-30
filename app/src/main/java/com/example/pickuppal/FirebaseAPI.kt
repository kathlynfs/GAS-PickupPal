import android.R.attr
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserData
import com.example.pickuppal.UserStatistics
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.functions.functions
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.ByteArrayOutputStream


class FirebaseAPI {

    private val TAG = "firebaseAPI"
    private val db = Firebase.database.reference
    private val functions = Firebase.functions

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

    fun claimItem(claimingUser: UserData) {
        getUserStatistics(claimingUser, object : UserStatisticsCallback {
            override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                val updatedStatistics = userStatistics.copy(
                    numItemsClaimed = userStatistics.numItemsPosted + 1
                )
                updateUserStatistics(updatedStatistics)
            }

            override fun onUserStatisticsError(e: Exception) {
                Log.e(TAG, "Error retrieving user statistics", e)
            }
        })

    }

    fun deleteImage(imageName: String) {

        val storage = Firebase.storage
        val storageRef = storage.reference

        val desertRef = storageRef.child("images/" + imageName)

        desertRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "$imageName deleted")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting image: ${exception.message}")
            }
    }

    //https://firebase.google.com/docs/ml/android/label-images#kotlin+ktx_2
    private fun getLabelDetectionJsonRequest(imageName: String, maxResults: Int) : JsonObject {
        val imageUri = "gs://pickuppal-e450c.appspot.com/images/$imageName"
        val request = JsonObject()

        // Add image to request
        val image = JsonObject()
        val source = JsonObject()
        source.add("gcsImageUri", JsonPrimitive(imageUri))
        image.add("source", source)
        request.add("image", image)

        // Add features to the request
        val feature = JsonObject()
        feature.add("maxResults", JsonPrimitive(maxResults))
        feature.add("type", JsonPrimitive("LABEL_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        val requestsArray = JsonArray()
        requestsArray.add(request)

        val toReturn =JsonObject()
        toReturn.add("requests", requestsArray)

        return toReturn
    }

    fun getLabels(imageUrl: String, maxResults: Int, onComplete: (List<String>) -> Unit) {
        val requestJson = getLabelDetectionJsonRequest(imageUrl, maxResults).toString()

        functions.getHttpsCallable("labelImage")
            .call(requestJson)
            .addOnSuccessListener { task ->
                val labels = mutableListOf<String>()
                val labelAnnotations = task.result?.data
                labelAnnotations?.forEach { label ->
                    val description = label.asJsonObject["description"].asString
                    val confidence = label.asJsonObject["score"].asDouble
                    Log.d("getLabels", "image annotations: $description ($confidence)")
                    labels.add(description)
                }
                onComplete(labels)
            }
            .addOnFailureListener { exception ->
                Log.e("getLabels", "Failed to get labels", exception)
                onComplete(emptyList()) // Handle failure by returning an empty list
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

        uploadTask.addOnSuccessListener { _ ->
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


    fun getUserStatistics(userId: String, callback: UserStatisticsCallback) {
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
                        numItemsPosted = 0,
                        numItemsClaimed = 0,
                        numRatings = 0,
                        totalRating = 0,
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
                        numItemsPosted = 0,
                        numItemsClaimed = 0,
                        numRatings = 0,
                        totalRating = 0,
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
