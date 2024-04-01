import android.util.Log
import com.example.pickuppal.PostingData
import com.google.firebase.Firebase
import com.google.firebase.database.database

class FirebaseAPI {

    val TAG = "FirebaseAPI"
    private val db = Firebase.database.reference

    fun uploadData(data : PostingData) {
        db.child("posting_data").child(data.id).updateChildren(data.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "User updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user", e)
            }
    }


}
