package com.example.pickuppal

import FirebaseAPI
import UserStatisticsCallback
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
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
    private var polylineToShow: List<LatLng>? = null
    private var polylineDestination: String? = null
    private val firebaseAPI = FirebaseAPI()
    private lateinit var profilePic: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val args = MapFragmentArgs.fromBundle(requireArguments())
        val profilePicture = args.user.profilePictureUrl
        profilePic = profilePicture!!
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

                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
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
                    navController = navController)}
        }
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
                    if(polylineToShow != null) {
                        Polyline(
                            points = polylineToShow!!,
                            color = Color.Blue,
                            startCap = ButtCap(),
                            clickable = true,
                            onClick = {val geoUri = "geo:0,0?q=${Uri.encode(polylineDestination)}"
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                // Set the package to specifically launch Google Maps app
                                mapIntent.setPackage("com.google.android.apps.maps")
                                startActivity(mapIntent)
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
                    if (searchQuery.value.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery.value = ""
                                filteredPostingDataList.value = postingDataList
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val action = MapFragmentDirections.actionMapFragmentToProfileFragment(user)
                                navController.navigate(action) // Navigate to ProfileFragment
                            }
                        ) {
                            AsyncImage(
                                model = profilePictureUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        }
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
        // SETTINGS VISUALIZER
        val isSettingsMenuVisible = remember { mutableStateOf(false) }
        val isSettingsMenuAnimationFinished = remember { mutableStateOf(false) }

        BackHandler(enabled = isSettingsMenuOpen.value) {
            isSettingsMenuOpen.value = false
        }

        LaunchedEffect(isMarkerClickPostingDataOpen.value) {
            if (isMarkerClickPostingDataOpen.value) {
                isSettingsMenuVisible.value = true
                isSettingsMenuAnimationFinished.value = false
            }
        }

        LaunchedEffect(isSettingsMenuOpen.value) {
            if (isSettingsMenuOpen.value) {
                isSettingsMenuAnimationFinished.value = false
            }
        }

        if (isSettingsMenuOpen.value) {
            SearchSettingsMenu(
                filteredPostingDataList,
                onDismissRequest = { isSettingsMenuVisible.value = false },
                isSettingsMenuVisible,
                onAnimationFinished = {
                    isSettingsMenuAnimationFinished.value = true
                }
            )
            LaunchedEffect(isSettingsMenuAnimationFinished.value) {
                if (isSettingsMenuAnimationFinished.value) {
                    isSettingsMenuOpen.value = false
                }
            }
        }

        // CARD VISUALIZER
        val isCardVisible = remember { mutableStateOf(false) }
        val isAnimationFinished = remember { mutableStateOf(false) }

        BackHandler(enabled = isMarkerClickPostingDataOpen.value) {
            isCardVisible.value = false
        }

        LaunchedEffect(isMarkerClickPostingDataOpen.value) {
            if (isMarkerClickPostingDataOpen.value) {
                isCardVisible.value = true
                isAnimationFinished.value = false
            }
        }

        if (isMarkerClickPostingDataOpen.value) {
            MarkerClickPostingData(
                postingData.value!!,
                user,
                onDismissRequest = {
                    isCardVisible.value = false
                },
                isCardVisible,
                onAnimationFinished = {
                    isAnimationFinished.value = true
                }
            )

            LaunchedEffect(isAnimationFinished.value) {
                if (isAnimationFinished.value) {
                    isMarkerClickPostingDataOpen.value = false
                }
            }
        }
    }

    @Composable
    fun MarkerClickPostingData(
        initialPostingData: PostingData,
        user: UserData,
        onDismissRequest: () -> Unit,
        isVisible: MutableState<Boolean>,
        onAnimationFinished: () -> Unit)
    {
        val shouldTrackRoute = remember { mutableStateOf(false) }
        val shouldMakeImageFullScreen = remember { mutableStateOf(false)}
        val postingRef = db.child("posting_data").child(initialPostingData.postID)
        val postingData = remember { mutableStateOf(initialPostingData) }
        val isOwnItem = postingData.value.userID == user.userId
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(initialPostingData) {
            isVisible.value = true
        }

        DisposableEffect(initialPostingData.postID) {
            val postingListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedPostingData = snapshot.getValue(PostingData::class.java)
                    updatedPostingData?.let {
                        postingData.value = it
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
                }
            }
            postingRef.addValueEventListener(postingListener)

            onDispose {
                postingRef.removeEventListener(postingListener)
            }
        }

        AnimatedVisibility(
            visible = isVisible.value,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 300)
            ),

        ) {
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
                            text = postingData.value.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = postingData.value.location,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = postingData.value.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                        ) {
                            // want to add ability to click on image and have it show up full screen
                            AsyncImage(
                                model = postingData.value.photoUrl,
                                contentDescription = postingData.value.description,
                                modifier = Modifier
                                    .size(200.dp)
                                    .graphicsLayer(
                                        rotationZ = 90f
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = { shouldMakeImageFullScreen.value = true })
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            ExtendedFloatingActionButton(
                                onClick = { shouldTrackRoute.value = true },
                                icon = {
                                    Icon(
                                        Icons.Filled.Place,
                                        "Extended floating action button."
                                    )
                                },
                                text = { Text(text = "Directions") },
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                            )
                        }

                        Button(
                            onClick = {
                                if (!isOwnItem) {
                                    coroutineScope.launch {
                                        postingRef.child("claimed").setValue(true).await()
                                        postingRef.child("claimedBy").setValue(user.userId).await()
                                    }
                                    firebaseAPI.claimItem(user)
                                }
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                            enabled = !postingData.value.claimed && !isOwnItem,
                            shape = CircleShape

                        ) {
                            Text(
                                text = when {
                                    isOwnItem -> "You can't claim your own item!"
                                    postingData.value.claimed -> "Already Claimed"
                                    else -> "Claim"
                                }
                            )
                        }

                        if (postingData.value.claimedBy == user.userId) {
                            if (postingData.value.rating == 0) {
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
                                            imageVector = if (postingData.value.rating >= index + 1) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable {
                                                    val newRating = index + 1
                                                    coroutineScope.launch {
                                                        postingRef.child("rating")
                                                            .setValue(newRating).await()
                                                        postingData.value.rating = newRating
                                                    }
                                                    firebaseAPI.submitRating(
                                                        postingData.value,
                                                        newRating
                                                    )
                                                }
                                        )
                                    }
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
        }

        LaunchedEffect(isVisible.value) {
            if (!isVisible.value) {
                delay(300)
                onAnimationFinished()
            }
        }

        BackHandler(enabled = shouldMakeImageFullScreen.value) {
            shouldMakeImageFullScreen.value = false
        }

        if (shouldMakeImageFullScreen.value) {
            MakeImageFullscreen(postingData.value, onDismissRequest = { shouldMakeImageFullScreen.value = false })
        }

        if (shouldTrackRoute.value) {
            TrackRoute(postingData.value, currentLocation!!, onDismissRequest = { shouldTrackRoute.value = false })
        }
    }

    @Composable
    fun TrackRoute(postingData: PostingData, currentLocation: Location, onDismissRequest: () -> Unit)
    {
        val destination = postingData.lat.toString() + ", " + postingData.lng.toString()
        val origin = currentLocation.latitude.toString() + ", " + currentLocation.longitude.toString()
        var decodedPolyline: List<LatLng>? = null

        LaunchedEffect(Unit)
        {
            lifecycleScope.launch {
                try {
                    val directions =
                        DirectionsFinderResultsRepository().fetchDirections(destination, origin)
                    val polyline = directions.routes[0].overviewPolyline.points
                    Log.d(ContentValues.TAG, "directions: $directions")
                    Log.d(ContentValues.TAG, "polyline: $polyline")
                    decodedPolyline = decodePolyLines(polyline)
                    polylineToShow = decodedPolyline
                    polylineDestination = postingData.reverseGeocodedAddress
                    Log.d(ContentValues.TAG, "decoded polyline: $decodedPolyline")

                }
                catch(ex: Exception){
                    Log.d(ContentValues.TAG, "failed")

                }


            }
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
    fun SearchSettingsMenu(
        filteredPostingDataList: MutableState<MutableList<PostingData>>,
        onDismissRequest: () -> Unit,
        isVisible: MutableState<Boolean>,
        onAnimationFinished: () -> Unit
    ) {
        LaunchedEffect(filteredPostingDataList) {
            isVisible.value = true
        }

        LaunchedEffect(isVisible.value) {
            if (!isVisible.value) {
                delay(300)
                onAnimationFinished()
            }
        }

        val args = MapFragmentArgs.fromBundle(requireArguments())
        val user = args.user
        AnimatedVisibility(
            visible = isVisible.value,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(durationMillis = 300)
            )
        ) {
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
                                    calculateDistance(
                                        LatLng(data.lat, data.lng),
                                        LatLng(
                                            currentLocation!!.latitude,
                                            currentLocation!!.longitude
                                        )
                                    ) <= distanceVal
                                }.toMutableList()
                            }
                        )

                        SettingsStarRating(
                            label = "Minimum Star Rating",
                            rating = remember { mutableIntStateOf(3) },
                            onRatingChange = { minRating ->
                                // used lifecycle scope from chat
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val filteredList = postingDataList.filter { data ->
                                        val userId = data.userID
                                        var averageRating = 0f

                                        val deferredRating = CompletableDeferred<Float>()
                                        firebaseAPI.getUserStatistics(
                                            userId,
                                            object : UserStatisticsCallback {
                                                override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                                                    val totalRating = userStatistics.totalRating
                                                    val numRatings = userStatistics.numRatings
                                                    val calculatedRating = if (numRatings > 0) {
                                                        totalRating.toFloat() / numRatings
                                                    } else {
                                                        0f
                                                    }
                                                    deferredRating.complete(calculatedRating)
                                                }

                                                override fun onUserStatisticsError(e: Exception) {
                                                    Log.e(
                                                        "TAG",
                                                        "Error retrieving user statistics for user $userId",
                                                        e
                                                    )
                                                    deferredRating.completeExceptionally(e)
                                                }
                                            })

                                        try {
                                            averageRating = deferredRating.await()
                                            Log.d("TAG", "$userId: $averageRating")
                                        } catch (e: Exception) {
                                            Log.e("TAG", "Error waiting for user statistics: $e")
                                        }

                                        Log.d(
                                            "TAG",
                                            "$userId: $averageRating, MinRating: $minRating"
                                        )
                                        averageRating >= minRating
                                    }
                                    filteredPostingDataList.value = filteredList.toMutableList()
                                }
                            }
                        )
                    }
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
                        modifier = Modifier.clickable {
                            val newRating = index + 1
                            rating.value = newRating
                            onRatingChange(newRating)
                        }
                    )
                }
            }
        }
    }

    // code from https://stackoverflow.com/questions/39851243/android-ios-decode-polyline-string
    fun decodePolyLines(poly: String): List<LatLng>? {
        val len = poly.length
        var index = 0
        val decoded: MutableList<LatLng> = ArrayList()
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = poly[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = poly[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            decoded.add(
                LatLng(
                    lat / 100000.0,
                    lng / 100000.0
                )
            )
        }
        return decoded
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
