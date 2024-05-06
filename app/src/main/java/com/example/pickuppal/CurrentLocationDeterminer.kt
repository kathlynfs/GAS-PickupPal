package com.example.pickuppal

import android.location.Location
import com.google.android.gms.tasks.Task

// Interface that is called by MapFragment and overriden in MainActivity to determine
// current location of device after making appropriate requests and returns Task<Location>
interface CurrentLocationDeterminer
{
    fun determineCurrentLocation(): Task<Location>
}