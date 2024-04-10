package com.example.pickuppal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng


class SharedViewModel : ViewModel()
{
    private var newLocation = MutableLiveData<String>()
    private var newLatLng = MutableLiveData<LatLng>()

    fun setNewLocation(location: String)
    {
        newLocation.value = location
    }

    fun getNewLocation(): MutableLiveData<String>
    {
        return newLocation
    }

    fun setNewLatLng(latLng: LatLng)
    {
        newLatLng.value = latLng
    }

    fun getNewLatLng(): MutableLiveData<LatLng>
    {
        return newLatLng
    }

}