package com.example.pickuppal

import FirebaseAPI
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class PostingFragment : Fragment() {

    private lateinit var title: EditText
    private lateinit var location: EditText
    private lateinit var description: EditText
    private lateinit var postButton: Button
    val TAG = "PostingFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_posting, container, false)
        title = view.findViewById(R.id.itemTitle)
        location = view.findViewById(R.id.itemLocation)
        description = view.findViewById(R.id.itemDescription)
        postButton = view.findViewById(R.id.postButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postButton.setOnClickListener {
            val title = title.text.toString()
            val location = location.text.toString()
            val description = description.text.toString()
            val data = PostingData(title = title, location = location, description = description)

            Log.d(TAG, "PostingData: $data")
            val firebaseAPI = FirebaseAPI()
            firebaseAPI.uploadData(data)

        }
    }

}
