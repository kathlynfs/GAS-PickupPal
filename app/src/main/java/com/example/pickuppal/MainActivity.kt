package com.example.pickuppal

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

import com.google.android.gms.location.Priority.PRIORITY_LOW_POWER
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener

class MainActivity : AppCompatActivity(), CurrentLocationDeterminer {
//    private val googleOAuthClient by lazy {
//        GoogleOAuthClient(
//            context = applicationContext,
//            oneTapClient = Identity.getSignInClient(applicationContext)
//        )
//    }

    private val FINE_PERMISSION_CODE = 1
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        viewModel.getNewLocation().observe(this) {location ->
            this.lifecycleScope.launch {
                try {
                    val response =
                        GeocoderResultsRepository().fetchGeocoderResults(location)
                    Log.d(ContentValues.TAG, "Response received: $response")
                    if (response.results[0].geometry.location.lat != null) {
                        var lat = response.results[0].geometry.location.lat.toDouble()
                        var lng = response.results[0].geometry.location.lng.toDouble()
                        Log.d(ContentValues.TAG, "Lat: $lat")
                        Log.d(ContentValues.TAG, "Lat: $lng")
                        var latLng = LatLng(lat, lng)
                        viewModel.setNewLatLng(latLng)
                    } else {
                        Log.e(ContentValues.TAG, "Null lat")
                        // do something
                    }
                } catch (ex: Exception) {
                    Log.e(ContentValues.TAG, "Failed to fetch LatLong", ex)
                    // maybe show a toast
                }
            }
        }
    }

    override fun determineCurrentLocation(): Task<Location> {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        return getLastLocation()
    }

    private fun getLastLocation(): Task<Location>
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(), FINE_PERMISSION_CODE)
        }

        var task = fusedLocationProviderClient.getCurrentLocation(PRIORITY_LOW_POWER, object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token

            override fun isCancellationRequested() = false
        })

        return task
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == FINE_PERMISSION_CODE)
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