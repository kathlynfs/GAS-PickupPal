package com.example.pickuppal

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.location.Priority.PRIORITY_LOW_POWER
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener

class MainActivity : AppCompatActivity(), CurrentLocationDeterminer {

    private val finePermissionCode = 1
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // lock orientation to portrait because it is best for typing
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // overrides function created in CurrentLocationDeterminer
    override fun determineCurrentLocation(): Task<Location> {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        return getLastLocation()
    }

    // requests permission to fine location if not already accessible and then returns device's current location
    // using fusedLocationProviderClient
    private fun getLastLocation(): Task<Location> {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(),
                finePermissionCode
            )
        }

        return fusedLocationProviderClient.getCurrentLocation(
            PRIORITY_LOW_POWER,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
    }

    // checks to see if fine location access was granted and then behaves appropriately
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == finePermissionCode)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getLastLocation()
            }
            else
            {
                Toast.makeText(
                    this,
                    R.string.location_permission_denied,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}