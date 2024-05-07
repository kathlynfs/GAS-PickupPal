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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
import com.google.android.gms.maps.GoogleMapOptions
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
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
    private var currentLocation: Location? = null // stores the current location of the user
    private lateinit var currentLocationDeterminer: CurrentLocationDeterminer // used for starting map centered on user
    private lateinit var db: DatabaseReference // used for initial population of PostingData (grabs reference from firebaseAPI.kt)
    private var postingDataList = mutableListOf<PostingData>() // initial list of posting data used for filtering
    private var polylineToShow: List<LatLng>? = null // polyline to show on map
    private var polylineDestination: String? = null
    private val firebaseAPI = FirebaseAPI() // instance of firebase database
    private lateinit var profilePic: String // url of profile picture provided on OAuth login
    private var cameraPosition:CameraPosition? = null // sets camera position to currentLocationDeterminer

    val MAX_CLAIM_DISTANCE = 0.1 // miles (can be modified to allow for longer distance claiming)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        // args provided on navigation by SignInFragment of user information
        val args = MapFragmentArgs.fromBundle(requireArguments())
        val profilePicture = args.user.profilePictureUrl
        profilePic = profilePicture!!
        db = FirebaseAPI().getDB()

        // set current location and populate initial dataList
        determineCurrentLocation().addOnSuccessListener { location ->
            currentLocation = location
            db.child("posting_data").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val postID = snapshot.key
                    val data = snapshot.getValue(PostingData::class.java)
                    data?.let {
                        it.postID = postID ?: ""
                        postingDataList.add(it)
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
                    val data = snapshot.getValue(PostingData::class.java)
                    postingDataList.remove(data)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }

        // remnant of fragment architecture we were using before
        val viewModel: SharedViewModel by activityViewModels()
        viewModel.getCameraPosition().observe(viewLifecycleOwner, Observer<LatLng?> { input ->
            cameraPosition = CameraPosition.fromLatLngZoom(input!!, 15f)
        })
        return ComposeView(requireContext()).apply {
            val navController = NavHostFragment.findNavController(this@MapFragment)

            setContent {
                // displaying MapScreen compose object
                MapScreen(
                    profilePictureUrl = profilePicture,
                    navController = navController
                )
            }
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
        profilePictureUrl: String, // profilePictureUrl is passed in as it isn't saved in the database and only
                                   // retrieved on OAuth login
        navController: NavController
    ) {
        val args = MapFragmentArgs.fromBundle(requireArguments())
        val user = args.user
        // all these variables stored in remember as it can update as time goes on
        val currentLocation = remember { mutableStateOf<Location?>(null) }
        val coroutineScope = rememberCoroutineScope()
        val searchQuery = remember { mutableStateOf("") }
        val isSearchActive = remember { mutableStateOf(false) }
        val isSettingsMenuOpen = remember { mutableStateOf(false) }
        val isMarkerClickPostingDataOpen = remember{ mutableStateOf(false)}
        val postingData = remember{mutableStateOf<PostingData?>(null)}
        var cameraPositionState = rememberCameraPositionState()
        val showClaimedPosts = remember { mutableStateOf(true) }
        val filteredPostingDataList = remember { mutableStateOf(if (showClaimedPosts.value) postingDataList else postingDataList.filter { !it.claimed }.toMutableList()) }
        val uiSettings by remember {
            mutableStateOf(
                MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = true
                )
            )
        }


        LaunchedEffect(Unit)
        {
            coroutineScope.launch {
                val location = determineCurrentLocation().await()
                currentLocation.value = location
            }

            while (true) {
                val location = determineCurrentLocation().await()
                currentLocation.value = location
                delay(1000)
            }
        }

        // setting camera to current location
        if(cameraPosition == null) {
            currentLocation.value?.let { currLocation ->
                val startingLocation =
                    LatLng(currLocation.latitude, currLocation.longitude)
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(startingLocation, 15f)
                }
            }
        }
        else{
            cameraPositionState = rememberCameraPositionState {
                position = cameraPosition as CameraPosition
            }
        }

        // my location enabled shows the blue dot
        val properties by remember {
            mutableStateOf(MapProperties(isMyLocationEnabled = true))
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF5F5F5))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = properties,
                cameraPositionState = cameraPositionState,
                googleMapOptionsFactory = { GoogleMapOptions().zoomGesturesEnabled(true) },
                uiSettings = uiSettings,
                content = {
                    // add marker for each value in filteredPostingDataList (which are items after filtering)
                    filteredPostingDataList.value.forEach { data ->
                        Marker(
                            state = MarkerState(LatLng(data.lat, data.lng)),
                            onClick = {
                                postingData.value = data
                                isMarkerClickPostingDataOpen.value = true
                                isMarkerClickPostingDataOpen.value
                            }
                        )
                    }

                    // draw polyline if directions was clicked
                    if (polylineToShow != null) {
                        Polyline(
                            points = polylineToShow!!,
                            color = Color.Blue,
                            startCap = ButtCap(),
                            clickable = true,
                            onClick = {
                                val geoUri = "geo:0,0?q=${Uri.encode(polylineDestination)}"
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                // Set the package to specifically launch Google Maps app
                                mapIntent.setPackage("com.google.android.apps.maps")
                                startActivity(mapIntent)
                            }

                        )
                    }
                },
            )

            // Uses Fragment Navigation as we still use a mixture of Compose and fragments
            // to PostingFragment if new posting button is clicked
            ExtendedFloatingActionButton(
                onClick = {
                    val action = MapFragmentDirections.actionMapFragmentToPostingFragment(user)
                    navController.navigate(action)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = getString(R.string.add_item), tint = Color.Black)
            }

            // Current location button to recenter map
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {currentLocation.value?.let { currLocation ->
                        val startingLocation =
                            LatLng(currLocation.latitude, currLocation.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(startingLocation, 15f)

                    }},
                    modifier = Modifier
                        .padding(0.dp, 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = getString(R.string.current_location),
                        tint = Color.Black
                    )
                }

                // Settings button for filtering data
                ExtendedFloatingActionButton(
                    onClick = { isSettingsMenuOpen.value = true},
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = getString(R.string.settings),
                        tint = Color.Black
                    )
                }
            }

            // Search bar for filtering data by title at top of screen
            // Search icon turns to back button when clicked to exit out of the search
            // or clicking on a query will lead you to the search and close out of the
            // search bar while auto-filling the title at the top
            // Trailing icon turns into x when value is inputted to clear the current query
            DockedSearchBar(
                query = searchQuery.value,
                // markers change as filteredPostingDataList gets modified as onQueryChange gets called
                onQueryChange = { query ->
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
                placeholder = { Text(getString(R.string.search)) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive.value) {
                                isSearchActive.value = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive.value) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Search,
                            contentDescription = if (isSearchActive.value) getString(R.string.back) else getString(R.string.search)
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
                                contentDescription = getString(R.string.clear)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val action =
                                    MapFragmentDirections.actionMapFragmentToProfileFragment(user)
                                navController.navigate(action) // Navigate to ProfileFragment
                            }
                        ) {
                            AsyncImage(
                                model = profilePictureUrl,
                                contentDescription = getString(R.string.profile_picture),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                },
                // List of items that much the current query based on either title or description.
                // when item is clicked, it will autofill the search bar
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

        // if users presses android back button, doesn't exit out of the fragment
        BackHandler(enabled = isSettingsMenuOpen.value) {
            isSettingsMenuOpen.value = false
        }

        // if a marker is clicked, close the settings menu
        LaunchedEffect(isMarkerClickPostingDataOpen.value) {
            if (isMarkerClickPostingDataOpen.value) {
                isSettingsMenuVisible.value = true
                isSettingsMenuAnimationFinished.value = false
            }
        }

        // for animation of sliding settings menu
        LaunchedEffect(isSettingsMenuOpen.value) {
            if (isSettingsMenuOpen.value) {
                isSettingsMenuAnimationFinished.value = false
            }
        }

        // pass in filteredPostingDataList to modify listing in memory
        if (isSettingsMenuOpen.value) {
            SearchSettingsMenu(
                filteredPostingDataList,
                onDismissRequest = { isSettingsMenuVisible.value = false },
                isSettingsMenuVisible,
                onAnimationFinished = {
                    isSettingsMenuAnimationFinished.value = true
                },
                showClaimedPosts
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

        // back handler if user presses back button while posing is open
        BackHandler(enabled = isMarkerClickPostingDataOpen.value) {
            isCardVisible.value = false
        }

        // more animation (used Chat for animation help)
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
        val userLocation = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        val itemLocation = LatLng(postingData.value.lat, postingData.value.lng)
        val distance = calculateDistance(userLocation, itemLocation)

        LaunchedEffect(initialPostingData) {
            isVisible.value = true
        }
        // set up a listener to update the posting data when it changes in the database
        // i.e. when user presses the rating button or claim button
        // to avoid showing stale data or updating data even though no changes in the database
        // was made
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
                        .height(450.dp)
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    // text information of item
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = postingData.value.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Text(
                            text = postingData.value.location,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Text(
                            text = getString(R.string.description),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                        Text(
                            text = postingData.value.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(end = 16.dp)
                                ) {
                                    AsyncImage(
                                        model = postingData.value.photoUrl,
                                        contentDescription = postingData.value.description,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(rotationZ = 90f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(onClick = {
                                                shouldMakeImageFullScreen.value = true
                                            }),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                ExtendedFloatingActionButton(
                                    onClick = { shouldTrackRoute.value = !shouldTrackRoute.value },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Place,
                                            getString(R.string.track_route_text)
                                        )
                                    },
                                    text = { Text(text = getString(R.string.directions)) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }

                        Text(
                            text = getString(R.string.claiming),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // claim item button
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
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                                    .padding(4.dp),
                                enabled = !postingData.value.claimed && !isOwnItem && distance <= MAX_CLAIM_DISTANCE,
                                shape = CircleShape,
                            ) {
                                // handles all 4 scenario (can't claim own item, already claimed,
                                // too far to claim, or available to claim)
                                Text(
                                    text = when {
                                        isOwnItem -> getString(R.string.claim_own_item_err)
                                        postingData.value.claimed -> getString(R.string.already_claimed)
                                        distance > MAX_CLAIM_DISTANCE -> getString(R.string.too_far)
                                        else -> getString(R.string.claim)
                                    }
                                )
                            }

                            // if postingData's claimed by userId value is same as current user's userId
                            // and rating is 0 (means not claimed [can only rate between 1-5 starts]),
                            // user is then finally given option to give rating
                            if (postingData.value.claimedBy == user.userId) {
                                if (postingData.value.rating == 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = getString(R.string.rating),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        repeat(5) { index ->
                                            Icon(
                                                painter = if (postingData.value.rating >= index + 1) painterResource(R.drawable.star_filled) else painterResource(R.drawable.star_outline),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        val newRating = index + 1
                                                        coroutineScope.launch {
                                                            postingRef
                                                                .child("rating")
                                                                .setValue(newRating)
                                                                .await()
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
                                        text = getString(R.string.already_rated),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
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

        // making image full screen (can be exited with back button or x button)
        BackHandler(enabled = shouldMakeImageFullScreen.value) {
            shouldMakeImageFullScreen.value = false
        }

        if (shouldMakeImageFullScreen.value) {
            MakeImageFullscreen(postingData.value, onDismissRequest = { shouldMakeImageFullScreen.value = false })
        }

        // if user presses Directions button
        if (shouldTrackRoute.value) {
            // compose object made drawing a polyline that can be clicked on to open google maps
            // and line should update as user moves
            TrackRoute(postingData.value, currentLocation!!, onDismissRequest = { shouldTrackRoute.value = false })
            Toast.makeText(
                context,
                R.string.track_route,
                Toast.LENGTH_LONG
            ).show()
        }
        else
        {
            polylineToShow = null
        }
    }

    // compose object made drawing a polyline that can be clicked on to open google maps
    // and line should update as user moves
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
                    decodedPolyline = decodePolyLines(polyline)
                    polylineToShow = decodedPolyline
                    polylineDestination = postingData.reverseGeocodedAddress

                }
                catch(ex: Exception){
                    Log.d(ContentValues.TAG, "failed")

                }
            }
        }
    }

    // just a compose object that shows full screen image and have a back button
    @Composable
    fun MakeImageFullscreen(postingData: PostingData, onDismissRequest: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AsyncImage(
                        model = postingData.photoUrl,
                        contentDescription = postingData.description,
                        modifier = Modifier
                            .graphicsLayer(rotationZ = 90f)
                            .fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )
                    ExtendedFloatingActionButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text(text = getString(R.string.done))
                    }
                }
            }
        }
    }

    // Filters available through settings button
    // directly modifies filteredPostingDataList which modifies the MarkerPostings in MapScreen object
    @Composable
    fun SearchSettingsMenu(
        filteredPostingDataList: MutableState<MutableList<PostingData>>,
        onDismissRequest: () -> Unit,
        isVisible: MutableState<Boolean>,
        onAnimationFinished: () -> Unit,
        showClaimedPosts: MutableState<Boolean>
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
                            text = getString(R.string.search_settings),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // All functions below just applies filter through data
                        SettingsSlider(
                            label = getString(R.string.distance),
                            value = remember { mutableFloatStateOf(2.5f) },
                            range = 1f..10f,
                            steps = 18,
                            onValueChange = { distanceVal ->
                                filteredPostingDataList.value = postingDataList.filter { data ->
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
                            label = getString(R.string.min_star_rating),
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
                                        } catch (e: Exception) {
                                            Log.e("TAG", "Error waiting for user statistics: $e")
                                        }

                                        averageRating >= minRating
                                    }
                                    filteredPostingDataList.value = filteredList.toMutableList()
                                }
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = getString(R.string.show_claimed_post),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = showClaimedPosts.value,
                                onCheckedChange = { checked ->
                                    showClaimedPosts.value = checked
                                    filteredPostingDataList.value = if (showClaimedPosts.value) {
                                        postingDataList
                                    } else {
                                        postingDataList.filter { !it.claimed }.toMutableList()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // distance slider called from settings
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
                text = "${String.format("%.1f", value.value)} " + getString(R.string.miles),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }

    // used by distance slider, written by ChatGPT
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
    // star rating filter for showing only items with average rating
    // of user that has higher than selected star rating
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
                        painter = if (rating.value > index) painterResource(R.drawable.star_filled) else painterResource(R.drawable.star_outline),
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
}
