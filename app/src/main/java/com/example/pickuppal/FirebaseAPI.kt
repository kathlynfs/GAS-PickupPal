import android.net.Uri
import android.util.Log
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserStatistics
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.storage.storage

class FirebaseAPI {

    private val TAG = "firebaseAPI"
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

    fun uploadImage(photoUri: Uri?) {
        photoUri?.let { uri ->
            val storage = Firebase.storage
            val storageRef = storage.reference
            val photoRef = storageRef.child("photos/${uri.lastPathSegment}")

            photoRef.putFile(uri)
                .addOnSuccessListener { _ ->
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val downloadUrl = downloadUri.toString()
                        Log.d("FirebasePhotoUpload", "Download URL: $downloadUrl")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("UploadPhotoToFirebase", "Upload failed: ${exception.message}")
                }
        } ?: run {
            Log.e("UploadPhotoToFirebase", "Photo URI is null")
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
