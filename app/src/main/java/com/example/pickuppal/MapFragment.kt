package com.example.pickuppal

import FirebaseAPI
import PostingDataListCallBack
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MapFragment : Fragment() {
    private var currentLocation: Location? = null
    private lateinit var currentLocationDeterminer: CurrentLocationDeterminer
    private lateinit var db: DatabaseReference
    private var postingDataList = mutableListOf<PostingData>()
    private val firebaseAPI = FirebaseAPI()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val args = MapFragmentArgs.fromBundle(requireArguments())
        val profilePicture = args.user.profilePictureUrl
        db = FirebaseAPI().getDB()

        determineCurrentLocation().addOnSuccessListener { location ->
            currentLocation = location
            val userLocation = LatLng(location.latitude, location.longitude)

            db.child("posting_data").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val postID = snapshot.key
                    val data = snapshot.getValue(PostingData::class.java)
                    data?.let {
                        it.postID = postID ?: ""

                        // Used ChatGPT for calculateDistance
                        //val distance = calculateDistance(userLocation, LatLng(it.lat, it.lng))

                        // if longer than 10 km (max supported)
                        //if (distance <= 10.0) {
                        //    Log.d(ContentValues.TAG, "snapshot.value = $it")
                        postingDataList.add(it)
                        //}
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    var data = snapshot.getValue(PostingData::class.java)
                    Log.d(ContentValues.TAG, "snapshot.value = $data ")
                    postingDataList.remove(data)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }


        return ComposeView(requireContext()).apply {
            val navController = NavHostFragment.findNavController(this@MapFragment)

            setContent {
                MapScreen(
                    profilePictureUrl = profilePicture!!,
                    navController = navController) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = MapFragmentArgs.fromBundle(requireArguments())
        val user = args.user
//        Toast.makeText(
//            requireContext(),
//            user.userId,
//            Toast.LENGTH_SHORT
//        ).show()
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
        val isMarkerClickPostingDataOpen = remember{ mutableStateOf(false)}
        val postingData = remember{mutableStateOf<PostingData?>(null)}
        var cameraPositionState = rememberCameraPositionState()
        val filteredPostingDataList = remember { mutableStateOf(postingDataList) }

        LaunchedEffect(Unit)
        {
            coroutineScope.launch {
                val location = determineCurrentLocation().await()
                currentLocation.value = location
            }
        }

        currentLocation.value?.let { currentLocation ->
            val startingLocation =
                LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(startingLocation, 15f)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                content = {
                    filteredPostingDataList.value.forEach{ data ->
                        Marker(
                            state = MarkerState(LatLng(data.lat, data.lng)),
                            onClick = {
                                postingData.value = data
                                isMarkerClickPostingDataOpen.value = true
                                isMarkerClickPostingDataOpen.value
                            }
                        )
                    }
                }
            )

            ExtendedFloatingActionButton(
                onClick = {
                    val action = MapFragmentDirections.actionMapFragmentToPostingFragment(user)
                    navController.navigate(action)
                },
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
                onQueryChange = {  query ->
                    searchQuery.value = query
                    filteredPostingDataList.value = if (query.isNotBlank()) {
                        postingDataList.filter { data ->
                            data.title.contains(query, ignoreCase = true) ||
                            data.description.contains(query, ignoreCase = true) ||
                            data.reverseGeocodedAddress.contains(query, ignoreCase = true)
                        }.toMutableList()
                    } else {
                        postingDataList
                    }
                },
                onSearch = {
                    isSearchActive.value = false
                    filteredPostingDataList.value = postingDataList.filter { data ->
                        data.title.equals(searchQuery.value, ignoreCase = true)
                    }.toMutableList()
                },
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
                                filteredPostingDataList.value = postingDataList
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
                        this.items(filteredPostingDataList.value) { data ->
                            ListItem(
                                headlineContent = { Text(data.title) },
                                supportingContent = { Text(data.description) },
                                modifier = Modifier.clickable {
                                    searchQuery.value = data.title
                                    isSearchActive.value = false
                                    postingData.value = data
                                    filteredPostingDataList.value = postingDataList.filter { item ->
                                        item.title.equals(data.title, ignoreCase = true)
                                    }.toMutableList()
                                }
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
            SearchSettingsMenu(filteredPostingDataList, onDismissRequest = { isSettingsMenuOpen.value = false })
        }

        BackHandler(enabled = isMarkerClickPostingDataOpen.value) {
            isMarkerClickPostingDataOpen.value = false
        }

        if (isMarkerClickPostingDataOpen.value) {
            MarkerClickPostingData(postingData.value!!, user, onDismissRequest = { isMarkerClickPostingDataOpen.value = false })
        }
    }


    @Composable
    fun MarkerClickPostingData(postingData: PostingData, user: UserData, onDismissRequest: () -> Unit) {
        val shouldTrackRoute = remember { mutableStateOf(false) }
        val shouldMakeImageFullScreen = remember { mutableStateOf(false)}
        val isClaimed = remember { mutableStateOf(postingData.claimed) }
        val isOwnItem = postingData.userID == user.userId
        val rating = remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismissRequest() }
        ) {
            Card(
                modifier = Modifier
                    .align(BottomCenter)
                    .height(500.dp)
                    .fillMaxWidth()
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
                        text = postingData.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = postingData.location,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = postingData.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.End),

                        ) {
                        // want to add ability to click on image and have it show up full screen
                        AsyncImage(
                            model = postingData.photoUrl,
                            contentDescription = postingData.description,
                            modifier = Modifier
                                .clickable(onClick = {shouldMakeImageFullScreen.value = true} )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        ExtendedFloatingActionButton(
                            onClick = { shouldTrackRoute.value = true },
                            icon = { Icon(Icons.Filled.Place, "Extended floating action button.") },
                            text = { Text(text = "Directions") },
                            modifier = Modifier
                                .align(Alignment.Bottom)
                        )
                    }

                    Button(
                        onClick = {
                            if (!isOwnItem) {
                                firebaseAPI.claimItem(postingData, user)
                                isClaimed.value = true
                            }
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                        enabled = !isClaimed.value && !isOwnItem,
                        shape = CircleShape

                    ){
                        Text(
                            text = when {
                                isOwnItem -> "You can't claim your own item!"
                                isClaimed.value -> "Already Claimed"
                                else -> "Claim"
                            }
                        )
                    }
                    if (postingData.claimedBy == user.userId) {
                        if (postingData.rating == 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    text = "Rate the item:",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = if (rating.value >= index + 1) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                                rating.value = index + 1
                                            }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    firebaseAPI.submitRating(postingData, rating.value)
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(text = "Submit Rating")
                            }
                        } else {
                            Text(
                                text = "You have already submitted a rating for this item.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        BackHandler(enabled = shouldMakeImageFullScreen.value) {
            shouldMakeImageFullScreen.value = false
        }

        if (shouldMakeImageFullScreen.value) {
            MakeImageFullscreen(postingData, onDismissRequest = { shouldMakeImageFullScreen.value = false })
        }
    }

    // will improve appearance of this in future
    @Composable
    fun MakeImageFullscreen(postingData: PostingData, onDismissRequest: () -> Unit)
    {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ){
            Card(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface)) {
                ExtendedFloatingActionButton(
                    onClick = { onDismissRequest()},
                    modifier = Modifier.align(Alignment.End)
                )
                {
                    Text(text = "Done")
                }
                AsyncImage(
                    model = postingData.photoUrl,
                    contentDescription = postingData.description,
                )
            }
        }
    }

    @Composable
    fun SearchSettingsMenu(filteredPostingDataList: MutableState<MutableList<PostingData>>, onDismissRequest: () -> Unit) {
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

                    SettingsSlider(
                        label = "Distance",
                        value = remember { mutableFloatStateOf(2.5f) },
                        range = 1f..10f,
                        steps = 18,
                        onValueChange = { distanceVal ->
                            filteredPostingDataList.value = postingDataList.filter { data ->
                                Log.d("TAG", currentLocation.toString())
                                calculateDistance(LatLng(data.lat, data.lng), LatLng(currentLocation!!.latitude, currentLocation!!.longitude)) <= distanceVal
                            }.toMutableList()
                        }
                    )

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
            Text(
                text = "${String.format("%.1f", value.value)} miles",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }

//    @Composable
//    fun SettingsTextInput(
//        label: String,
//        value: MutableState<String>,
//        onValueChange: (String) -> Unit
//    ) {
//        Column(modifier = Modifier.padding(vertical = 8.dp)) {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.bodyLarge
//            )
//            OutlinedTextField(
//                value = value.value,
//                onValueChange = {
//                    value.value = it
//                    onValueChange(it)
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 8.dp)
//            )
//        }
//    }

    private fun calculateDistance(location1: LatLng, location2: LatLng): Double {
        val earthRadius = 6371.0 // in kilometers
        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c * 0.621371
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
