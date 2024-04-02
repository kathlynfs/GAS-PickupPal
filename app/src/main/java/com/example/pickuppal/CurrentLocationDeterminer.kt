package com.example.pickuppal

import android.location.Location
import com.google.android.gms.tasks.Task

interface CurrentLocationDeterminer
{
    fun determineCurrentLocation(): Task<Location>
}