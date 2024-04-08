import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.pickuppal.PostingData
import com.example.pickuppal.UserStatistics
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

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

    fun uploadImage(bitmap: Bitmap, imageName: String, postingData: PostingData) {
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
                db.child("posting_data").child(postingData.postID).child("photoUrl").setValue(imageUrl)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Error getting download URL: ${exception.message}")
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error uploading image: ${exception.message}")
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
