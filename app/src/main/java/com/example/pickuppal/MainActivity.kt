package com.example.pickuppal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.example.pickuppal.PostingFragment
import com.google.android.gms.maps.CameraUpdateFactory


class MainActivity : AppCompatActivity() {
//    private val googleOAuthClient by lazy {
//        GoogleOAuthClient(
//            context = applicationContext,
//            oneTapClient = Identity.getSignInClient(applicationContext)
//        )
//    }

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        // create boolean to determine whether or not map should be shown at a given time
        // for initial testing, use true if you want to view map, false if not
        if (true) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            getLastLocation()

            if (mapFragment == null) {
                val newMapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .add(R.id.map, newMapFragment)
                    .commit()
            }
        } else {
            if (mapFragment != null) {
                supportFragmentManager.beginTransaction()
                    .remove(mapFragment)
                    .commit()
            }
        }

         */
    }

    /*
    override fun onMapReady(googleMap: GoogleMap) {
        // Add a marker at your current location and move the camera
        val startingLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
        googleMap.addMarker(MarkerOptions().position(startingLocation).title("Marker in Starting Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 15f))
    }

    private fun getLastLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(), FINE_PERMISSION_CODE)
            return
        }

        var task = fusedLocationProviderClient.getLastLocation()
        task.addOnSuccessListener{ location ->
            if(location != null)
            {
                currentLocation = location

                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
            else {
                currentLocation = Location("mockedLocationProvider").apply {
                    latitude = 42.350876
                    longitude = -71.106918
                }
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
                    "Location permission is denied, please allow permission",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

     */

//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            val viewModel = viewModel<SignInViewModel>()
//            val state by viewModel.state.collectAsStateWithLifecycle()
//            val launcher = rememberLauncherForActivityResult(
//                contract = ActivityResultContracts.StartIntentSenderForResult(),
//                onResult = { result ->
//                    if (result.resultCode == RESULT_OK) {
//                        lifecycleScope.launch {
//                            val signInResult = googleOAuthClient.signInWithIntent(
//                                intent = result.data ?: return@launch
//                            )
//                            viewModel.onSignInResult(signInResult)
//                        }
//                    }
//                }
//            )
//
//            LaunchedEffect(key1 = state.isSignInSuccessful) {
//                if (state.isSignInSuccessful) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Sign in successful",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//            SignInScreen(state = state,
//                onSignInClick = {
//                    lifecycleScope.launch {
//                        val signInIntent = googleOAuthClient.signIn()
//                        launcher.launch(
//                            IntentSenderRequest.Builder(
//                                signInIntent ?: return@launch
//                            ).build()
//                        )
//                    }
//                }
//            )
//        }
//    }
}