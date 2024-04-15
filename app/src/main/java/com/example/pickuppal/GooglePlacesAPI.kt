package com.example.pickuppal

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Credit:
// https://stackoverflow.com/questions/70834787/implementing-google-places-autocomplete-textfield-implementation-in-jetpack-comp/72586090#72586090
interface GooglePlacesAPI {
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPredictions(
        @Query("key") key: String = BuildConfig.MAPS_API_KEY,
    @Query("types") types: String = "address",
    @Query("input") input: String
    ): GooglePredictionsResponse

    companion object{
        const val BASE_URL = "https://maps.googleapis.com/"
        fun create(): GooglePlacesAPI {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(GooglePlacesAPI::class.java)
        }
    }
}

data class GooglePredictionsResponse(
    val predictions: ArrayList<GooglePrediction>
)

data class GooglePrediction(
    val description: String,
    val terms: List<GooglePredictionTerm>
)

data class GooglePredictionTerm(
    val offset: Int,
    val value: String
)

class GooglePlacesRepository constructor(
    private val api: GooglePlacesAPI,
){
    suspend fun getPredictions(input: String): Resource<GooglePredictionsResponse> {
        val response = try {
            api.getPredictions(input = input)
        } catch (e: Exception) {
            Log.d("Rently", "Exception: ${e}")
            return Resource.Error("Failed prediction")
        }

        return Resource.Success(response)
    }
}

sealed class Resource<T>(val data: T? = null, val message: String? = null){
    class Success<T>(data: T): Resource<T>(data)
    class Error<T>(message: String, data:T? = null): Resource<T>(data = data, message = message)
    class Loading<T>(data: T? = null): Resource<T>(data = data)
}

class GooglePlacesViewModel(private val repository: GooglePlacesRepository) : ViewModel() {
    private val _predictions = MutableLiveData<Resource<GooglePredictionsResponse>>()
    val predictions: LiveData<Resource<GooglePredictionsResponse>> = _predictions

    fun getPredictions(input: String) {
        viewModelScope.launch {
            _predictions.postValue(Resource.Loading())
            val response = repository.getPredictions(input)
            _predictions.postValue(response)
        }
    }
}
class GooglePlacesViewModelFactory(private val repository: GooglePlacesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GooglePlacesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GooglePlacesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}