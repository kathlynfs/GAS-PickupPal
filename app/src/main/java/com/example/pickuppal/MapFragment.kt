package com.example.pickuppal

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory

class MapFragment : Fragment(), OnMapReadyCallback
{
    private lateinit var currentLocation: Location
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        // create boolean to determine whether or not map should be shown at a given time
        // for initial testing, use true if you want to view map, false if not
        if (true) {
            if (mapFragment == null) {
                val newMapFragment = SupportMapFragment.newInstance()
                childFragmentManager.beginTransaction()
                    .add(R.id.map, newMapFragment)
                    .commit()
            }
        } else {
            if (mapFragment != null) {
                childFragmentManager.beginTransaction()
                    .remove(mapFragment)
                    .commit()
            }
        }

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        currentLocation = Location("mockedLocationProvider").apply {
            latitude = 42.350876
            longitude = -71.106918
        }
        // Add a marker at your current location and move the camera
        val startingLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
        googleMap.addMarker(MarkerOptions().position(startingLocation).title("Marker in Starting Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 15f))
    }

}