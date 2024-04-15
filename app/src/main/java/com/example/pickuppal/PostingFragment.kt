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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.min
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController
import java.util.UUID


class PostingFragment : Fragment() {
    private val args: PostingFragmentArgs by navArgs()
    private var photoUri: Uri? = null
    private var photoName: String? = null
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
            photoUri = Uri.fromFile(File(requireContext().filesDir, photoName!!))
            Log.d("TakePhotoCallback", "photoUri: $photoUri")
        } else {
            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchTakePicture(updatePreviewImage: (Bitmap?) -> Unit) {
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
            val bitmap = BitmapFactory.decodeStream(context?.contentResolver?.openInputStream(photoUri))
            Log.d("launchTakePicture", "Bitmap decoded")
            updatePreviewImage(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
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
        val previewImage = remember { mutableStateOf<Bitmap?>(null) }
        val navController = findNavController()
        val repository = GooglePlacesRepository(GooglePlacesAPI.create())
        val factory = GooglePlacesViewModelFactory(repository)
        val googlePlacesViewModel: GooglePlacesViewModel = viewModel(factory = factory)
        val predictions by googlePlacesViewModel.predictions.observeAsState()

        val updatePreviewImage: (Bitmap?) -> Unit = { newImage ->
            previewImage.value = newImage
        }

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

            Row(
                modifier = Modifier
                    .padding(top = 56.dp, bottom = 16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .padding(top = 56.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PostingTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    placeholderText = "Add a title"
                )

                PictureButton(
                    onClick = { launchTakePicture(updatePreviewImage) },
                    modifier = Modifier.padding(16.dp),
                    previewImage = previewImage.value
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
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            itemsIndexed(resource.data?.predictions ?: emptyList()) { _, prediction ->
                                Text(
                                    text = prediction.description,
                                    modifier = Modifier
                                        .clickable {
                                            locationState.value = TextFieldValue(prediction.description)
                                        }
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
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
                            text = "No location suggestions available",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                PostingTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    placeholderText = "Add a Description"
                )
            }
        }
    }



    @Composable
    fun PostingTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null,
        visualTransformation: VisualTransformation = VisualTransformation.None
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
                // underline
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent

            ),
            visualTransformation = visualTransformation
        )
    }

    private fun onPostClicked(title : String, location : String, user : UserData, description : String, navController : NavController) {
        if (hasRequiredInputs(title, location, photoUri)) {
            val firebaseAPI = FirebaseAPI()
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(context?.contentResolver?.openInputStream(uri))
                val id = UUID.randomUUID().toString()
                val name:String = photoName?: "defaultphotoname"

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

                                firebaseAPI.uploadImage(bitmap, name) { imageUrl ->
                                    if (imageUrl != null) {
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
                                            photoUrl = imageUrl
                                        )
                                        firebaseAPI.uploadPostingData(data, user)
                                        navController.popBackStack()

                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error. Please try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
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



    private fun hasRequiredInputs(title : String, location : String, uri : Uri?): Boolean {
        return title.isNotBlank() && location.isNotBlank() && uri != null
    }

    fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        val sampleSize = if (srcHeight <= destHeight && srcWidth <= destWidth) {
            1
        } else {
            val heightScale = srcHeight / destHeight
            val widthScale = srcWidth / destWidth

            min(heightScale, widthScale).toInt()
        }

        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })
    }

    @Composable
    fun LocationTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        modifier: Modifier = Modifier,
        placeholderText: String? = null,
        visualTransformation: VisualTransformation = VisualTransformation.None
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
                placeholderText = placeholderText,
                visualTransformation = visualTransformation
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
        previewImage: Bitmap?,
        modifier: Modifier = Modifier
    ) {
        val currentPreviewImage = remember { mutableStateOf(previewImage) }

        Box(
            modifier = modifier
                .size(200.dp)
                .background(Color.Transparent)
                .clickable { onClick() }
        ) {
            if (currentPreviewImage.value != null) {
                Image(
                    bitmap = currentPreviewImage.value!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.add_a_photo),
                    contentDescription = "Add Photo",
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                    tint = Color.Gray
                )
            }
        }
    }




}

