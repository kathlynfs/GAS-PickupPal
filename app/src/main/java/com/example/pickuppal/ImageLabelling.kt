package com.example.pickuppal

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.functions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

class ImageLabelling {
    private val functions: FirebaseFunctions = Firebase.functions


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

        val toReturn = JsonObject()
        toReturn.add("requests", requestsArray)

        return toReturn
    }

    fun getLabels(imageName: String, maxResults: Int): Task<JsonElement> {
        val requestJson = getLabelDetectionJsonRequest(imageName, maxResults).toString()

        return functions
            .getHttpsCallable("labelImage")
            .call(requestJson)
            .continueWith { task ->
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }
}