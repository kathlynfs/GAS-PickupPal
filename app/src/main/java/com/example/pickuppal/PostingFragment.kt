package com.example.pickuppal

import FirebaseAPI
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.util.UUID
import coil.request.ImageRequest
import com.google.firebase.storage.StorageReference


class PostingFragment : Fragment() {
    private val args: PostingFragmentArgs by navArgs()
    private val mutablePhotoUrl: MutableLiveData<String?> = MutableLiveData(null)
    private val photoUrl: LiveData<String?> = mutablePhotoUrl
    private val mutablePhotoUri: MutableLiveData<Uri?> = MutableLiveData(null)
    private var photoUri: LiveData<Uri?> = mutablePhotoUri
    private var photoName: String? = null
    private var prevPhotoName: String? = null
    private var photoFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val user = args.user
        return ComposeView(requireContext()).apply {
            setContent {
                PostingContent(
                    user = user,
                    onBackPressed = {
                        // if the user doesn't post, delete the image
                        if (prevPhotoName != null) {
                            val firebaseAPI = FirebaseAPI()
                            firebaseAPI.deleteImage(prevPhotoName.toString())
                        }
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                )
            }
        }
    }


    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto ->
        if (didTakePhoto && photoName != null) {
            mutablePhotoUri.value = Uri.fromFile(File(requireContext().filesDir, photoName!!))
            //Log.d("TakePhotoCallback", "photoUri: $photoUri")
            uploadImageToFirebase()
        } else {
            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchTakePicture() {
        val timestamp = System.currentTimeMillis()
        val name = "IMG_${timestamp}.jpeg"
        photoName = name
        val pf = File(requireContext().filesDir, photoName!!)
        photoFile = pf
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.example.pickuppal.fileprovider",
            pf
        )
        try {
            takePhoto.launch(photoUri)
            Log.d("launchTakePicture", "Bitmap decoded")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAnnotations(photoName: String) {
        val firebaseAPI = FirebaseAPI()

        firebaseAPI.getLabels(photoName, maxResults = 5)
            .addOnCompleteListener { task ->
                Log.d("Posting", "Task is successful: ${task.isSuccessful}")
                if (task.isSuccessful()) {
                    if (task.result != null) {
                        val labels = mutableListOf<String>()
                        val labelAnnotations =
                            task.result!!.asJsonArray[0].asJsonObject["labelAnnotations"].asJsonArray
                        for (label in labelAnnotations) {
                            val labelObj = label.asJsonObject
                            val description = labelObj["description"].asString
                            val confidence = labelObj["score"].asDouble
                            Log.d("getLabels", "image annotations: $labels ($confidence)")
                            labels.add(description)
                        }
                    }
                } else {
                    Log.e("GetLabels", "Failed to get labels", task.exception)
                }
            }
    }


    @Composable
    private fun PostingContent(
        user: UserData,
        onBackPressed: () -> Unit
    ) {
        val titleState = remember { mutableStateOf(TextFieldValue()) }
        val locationState = remember { mutableStateOf(TextFieldValue()) }
        val descriptionState = remember { mutableStateOf(TextFieldValue()) }
        val navController = findNavController()
        val repository = GooglePlacesRepository(GooglePlacesAPI.create())
        val factory = GooglePlacesViewModelFactory(repository)
        val googlePlacesViewModel: GooglePlacesViewModel = viewModel(factory = factory)
        val predictions by googlePlacesViewModel.predictions.observeAsState()
        val previewImageUri : Uri? by photoUri.observeAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFF5F5F5))
        ) {
            // Top App Bar
            Surface(
                color = Color(0xFF6200EE)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .padding(top = 56.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TitleTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    placeholderText = "Add a title"
                )

                PictureButton(
                    onClick = { launchTakePicture() },
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp, top = 0.dp, end = 16.dp),
                    imageUri = previewImageUri
                )

                LocationTextField(
                    value = locationState.value,
                    onValueChange = {
                        locationState.value = it
                        googlePlacesViewModel.getPredictions(it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholderText = "Add a Location"
                )

                when (val resource = predictions) {
                    is Resource.Success -> {
                        LazyRow(

                        ) {
                            itemsIndexed(resource.data?.predictions ?: emptyList()) { _, prediction ->
                                Surface(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            Color(0xFF6200EE),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            locationState.value =
                                                TextFieldValue(prediction.description)
                                        },
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = prediction.description,
                                        modifier = Modifier
                                            .widthIn(max = 200.dp)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = TextStyle(fontWeight = FontWeight.Bold),
                                        color = Color.DarkGray,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Text(
                            text = "Error fetching location suggestions",
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(8.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Enter a location for suggestions",
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.Start),
                            color = Color.Gray
                        )
                    }
                }

                DescriptionTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    placeholderText = "Add a Description",
                    modifier =  Modifier.verticalScroll(rememberScrollState())
                )

                Button(
                    onClick = {
                        onPostClicked(
                            titleState.value.text,
                            locationState.value.text,
                            user,
                            descriptionState.value.text,
                            navController
                        )
                    },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .wrapContentWidth(align = Alignment.CenterHorizontally)
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Post")
                }

            }
        }
    }

    @Composable
    fun DescriptionTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.Transparent)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), // Make it scrollable
                placeholder = {
                    if (placeholderText != null) {
                        Text(placeholderText, color = Color.Gray)
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }



    @Composable
    fun PostingTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            placeholder = {
                if (placeholderText != null) {
                   Text(placeholderText, color = Color.Gray)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor =  Color.Transparent,
                unfocusedContainerColor =  Color.Transparent,
                disabledContainerColor =  Color.Transparent,
                cursorColor = Color.Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent

            ),
            singleLine = true
        )
    }

    @Composable
    fun TitleTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null,
        size : TextUnit = 24.sp
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp)
                .background(Color.Transparent),
            textStyle = TextStyle(fontSize = size),
            placeholder = {
                if (placeholderText != null) {
                    Text(
                        placeholderText,
                        color = Color.Gray,
                        style = TextStyle(fontSize = size))
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor =  Color.Transparent,
                unfocusedContainerColor =  Color.Transparent,
                disabledContainerColor =  Color.Transparent,
                cursorColor = Color.Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent

            ),
            singleLine = true
        )
    }

    private fun uploadImageToFirebase() {
        val photoUri = mutablePhotoUri.value
        val firebaseAPI = FirebaseAPI()

        // if photoUrl was not null, then an image was added before
        // delete that image before adding a new one
        if (prevPhotoName != null) {
            firebaseAPI.deleteImage(prevPhotoName.toString())
        }

        photoUri?.let {uri ->
            val bitmap = BitmapFactory.decodeStream(context?.contentResolver?.openInputStream(uri))
            val name:String = photoName?: "defaultphotoname"
            firebaseAPI.uploadImage(bitmap, name) { url ->
                if (url != null) {
                    mutablePhotoUrl.value = url
                    prevPhotoName = name
                    getAnnotations(name)
                }  else {
                    Toast.makeText(
                        context,
                        "Error when uploading. Please take the picture again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onPostClicked(title : String, location : String, user : UserData, description : String, navController : NavController) {
        val photoUrl = mutablePhotoUrl.value
        if (hasRequiredInputs(title, location, photoUrl)) {
            val firebaseAPI = FirebaseAPI()
            photoUrl?.let { url ->
                val id = UUID.randomUUID().toString()

                // geocoding address to lat + long
                lifecycleScope.launch {
                    try {
                        val geocodeResp =
                            GeocoderResultsRepository().fetchGeocoderResults(location)
                        Log.d(ContentValues.TAG, "Response received: $geocodeResp")
                        if (geocodeResp.results[0].geometry.location.lat != null) {
                            // once lat and long have been retrieved by geocoding,
                            // reverse geocode to get cleanly formatted address
                            val lat = geocodeResp.results[0].geometry.location.lat.toDouble()
                            val lng = geocodeResp.results[0].geometry.location.lng.toDouble()

                            try {
                                val reverseGeocodeResp =
                                    ReverseGeocoderResultsRepository().fetchReverseGeocoderResults("$lat, $lng")
                                Log.d(ContentValues.TAG, "reverse geocoding: $reverseGeocodeResp")
                                val reverseGeocodedAddress = reverseGeocodeResp.results[0].address
                                Log.d(ContentValues.TAG, "address: $reverseGeocodedAddress")

                                Log.d(ContentValues.TAG, "Lat: $lat")
                                Log.d(ContentValues.TAG, "Lat: $lng")


                                val data = PostingData(
                                    postID = id,
                                    userID = user.userId,
                                    title = title,
                                    location = location,
                                    lat = lat,
                                    lng = lng,
                                    reverseGeocodedAddress = reverseGeocodedAddress,
                                    description = description,
                                    claimed = false,
                                    photoUrl = url,
                                    claimedBy = "",
                                    rating = 0,
                                    )
                                firebaseAPI.uploadPostingData(data, user)
                                navController.popBackStack()

                            }
                            catch(ex: Exception)
                            {
                                Log.e(ContentValues.TAG, "Failed to fetch address", ex)
                            }
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


        } else {
            Toast.makeText(context, "Please fill in title, location, and image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasRequiredInputs(title : String, location : String, url : String?): Boolean {
        return title.isNotBlank() && location.isNotBlank() && url != null
    }

    @Composable
    fun LocationTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.LightGray)
                )
            }
            PostingTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier
                    .fillMaxWidth(),
                placeholderText = placeholderText
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray)
            )
        }
    }

    @Composable
    fun PictureButton(
        onClick: () -> Unit,
        imageUri: Uri?,
        modifier: Modifier = Modifier
    ) {

        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Transparent)
                .clickable { onClick() }
        ) {
            if (imageUri != null) {
                val request = ImageRequest.Builder(requireContext())
                    .data(imageUri)
                    .build()
                Image(
                    painter = rememberAsyncImagePainter(request),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.add_a_photo),
                    contentDescription = "Add Photo",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    tint = Color.Gray
                )
            }
        }
    }

    //https://firebase.google.com/docs/ml/android/label-images
    private fun scaleBitmapDown(bitmap: Bitmap): Bitmap {
        val maxDimension = 640
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = 640
        var resizedHeight = 480
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }


}

