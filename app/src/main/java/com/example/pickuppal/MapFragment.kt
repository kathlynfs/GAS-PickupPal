package com.example.pickuppal

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapFragment : Fragment() {
    private var currentLocation: Location? = null
    private lateinit var currentLocationDeterminer: CurrentLocationDeterminer
    private var mapPins = mutableListOf<LatLng>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedViewModel: SharedViewModel by activityViewModels()


        return ComposeView(requireContext()).apply {
            setContent {
                MapScreen(
                    onAddItemClick = { sharedViewModel.setCurrentFragment("posting") },
                    onMapReady = { googleMap ->
                        currentLocation?.let{ location ->
                            val startingLocation = LatLng(location.latitude, location.longitude)

                            for (pin in mapPins) {
                                googleMap.addMarker(
                                    MarkerOptions().position(pin).title("New Pin")
                                )
                            }

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 15f))
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = MapFragmentArgs.fromBundle(requireArguments())
        val user = args.user
        Toast.makeText(
            requireContext(),
            user.userId,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        currentLocationDeterminer = context as CurrentLocationDeterminer
    }

    private fun determineCurrentLocation(): Task<Location> {
        return currentLocationDeterminer.determineCurrentLocation()
    }

    @Composable
    fun MapScreen(
        onAddItemClick: () -> Unit,
        onMapReady: (GoogleMap) -> Unit
    ) {
        val mapView = rememberMapViewWithLifecycle()
        val currentLocation = remember { mutableStateOf<Location?>(null) }
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            ) { mapView ->
                mapView.getMapAsync { googleMap ->
                    coroutineScope.launch {
                        val location = determineCurrentLocation().await()
                        currentLocation.value = location
                        currentLocation.value?.let { currentLocation ->
                            val startingLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
                            Log.d("TAG", startingLocation.toString())
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 15f))
                        }
                        onMapReady(googleMap)
                    }
                }
            }

            Button(
                onClick = onAddItemClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("Add Item")
            }
        }
    }

    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current
        val mapView = remember { MapView(context) }

        // Makes MapView follow the lifecycle of this composable
        val lifecycleObserver = rememberMapLifecycleObserver(mapView)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            lifecycle.addObserver(lifecycleObserver)
            onDispose {
                lifecycle.removeObserver(lifecycleObserver)
            }
        }

        return mapView
    }

    @Composable
    fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
        remember(mapView) {
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> throw IllegalStateException()
                }
            }
        }
}