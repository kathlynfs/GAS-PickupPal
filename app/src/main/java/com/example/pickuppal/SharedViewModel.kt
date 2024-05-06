package com.example.pickuppal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class SharedViewModel: ViewModel() {
    private val cameraPosition = MutableLiveData<LatLng?>()

    fun setCameraPosition(input: LatLng?)
    {
        cameraPosition.value = input
    }

    fun getCameraPosition(): MutableLiveData<LatLng?>
    {
        return cameraPosition
    }
}