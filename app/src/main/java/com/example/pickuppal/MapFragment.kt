package com.example.pickuppal

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Slider
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
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
        val args = MapFragmentArgs.fromBundle(requireArguments())

        val profilePicture = args.user.profilePictureUrl

        return ComposeView(requireContext()).apply {
            val navController = NavHostFragment.findNavController(this@MapFragment)

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
                    },
                    profilePictureUrl = profilePicture!!,
                    navController = navController
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MapScreen(
        onAddItemClick: () -> Unit,
        onMapReady: (GoogleMap) -> Unit,
        profilePictureUrl: String,
        navController: NavController
    ) {
        val args = MapFragmentArgs.fromBundle(requireArguments())
        val user = args.user
        val mapView = rememberMapViewWithLifecycle()
        val currentLocation = remember { mutableStateOf<Location?>(null) }
        val coroutineScope = rememberCoroutineScope()
        val searchQuery = remember { mutableStateOf("") }
        val isSearchActive = remember { mutableStateOf(false) }
        val isSettingsMenuOpen = remember { mutableStateOf(false) }

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
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 15f))
                        }
                        onMapReady(googleMap)
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onAddItemClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.Black)
            }

            ExtendedFloatingActionButton(
                onClick = { isSettingsMenuOpen.value = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }

            DockedSearchBar(
                query = searchQuery.value,
                onQueryChange = { searchQuery.value = it },
                onSearch = { /* Handle search */ },
                active = isSearchActive.value,
                onActiveChange = { isSearchActive.value = it },
                placeholder = { Text("Search") },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive.value) {
                                isSearchActive.value = false
                                searchQuery.value = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive.value) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Search,
                            contentDescription = if (isSearchActive.value) "Back" else "Search"
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val action = MapFragmentDirections.actionMapFragmentToProfileFragment(user)

                            navController.navigate(action) // Navigate to ProfileFragment
                        }) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    }
                },
                content = {
                    LazyColumn {
                        // Add search results or suggestions here
                        items(5) { index ->
                            ListItem(
                                headlineContent = { Text("Search Result $index") },
                                modifier = Modifier.clickable { /* Handle item click */ }
                            )
                        }
                    }
                }

            )
        }
        BackHandler(enabled = isSettingsMenuOpen.value) {
            isSettingsMenuOpen.value = false
        }

        if (isSettingsMenuOpen.value) {
            SearchSettingsMenu(onDismissRequest = { isSettingsMenuOpen.value = false })
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchSettingsMenu(onDismissRequest: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismissRequest() }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .clickable(enabled = false) {},
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Search Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Distance filter with a slider
                    SettingsSlider(
                        label = "Distance",
                        value = remember { mutableStateOf(2.5f) },
                        range = 0f..5f,
                        steps = 10,
                        onValueChange = { /* Handle distance value change */ }
                    )

                    // Include and exclude name filters with text inputs
                    SettingsTextInput(
                        label = "Include Name",
                        value = remember { mutableStateOf("") },
                        onValueChange = { /* Handle include name value change */ }
                    )

                    SettingsTextInput(
                        label = "Exclude Name",
                        value = remember { mutableStateOf("") },
                        onValueChange = { /* Handle exclude name value change */ }
                    )

                    // Star search slider with star rating
                    SettingsStarRating(
                        label = "Minimum Star Rating",
                        rating = remember { mutableStateOf(3) },
                        onRatingChange = { /* Handle star search rating change */ }
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsSlider(
        label: String,
        value: MutableState<Float>,
        range: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChange: (Float) -> Unit
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = value.value,
                onValueChange = {
                    value.value = it
                    onValueChange(it)
                },
                valueRange = range,
                steps = steps,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    @Composable
    fun SettingsTextInput(
        label: String,
        value: MutableState<String>,
        onValueChange: (String) -> Unit
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = value.value,
                onValueChange = {
                    value.value = it
                    onValueChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }

    @Composable
    fun SettingsStarRating(
        label: String,
        rating: MutableState<Int>,
        onRatingChange: (Int) -> Unit
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (rating.value > index) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.clickable { rating.value = index + 1 }
                    )
                }
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