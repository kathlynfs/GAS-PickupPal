package com.example.pickuppal

import FirebaseAPI
import android.content.ContentValues.TAG
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
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.File
import kotlin.math.min


class PostingFragment : Fragment() {
    private val args: PostingFragmentArgs by navArgs()
    private var photoUri: Uri? = null

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

    private var photoName: String? = null
    private var photoFile: File? = null

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto ->
        if (didTakePhoto && photoName != null) {
            photoUri = Uri.fromFile(File(requireContext().filesDir, photoName))
            Log.d("TakePhotoCallback", "photoUri: $photoUri")
        } else {
            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchTakePicture() {
        val timestamp = System.currentTimeMillis()
        val name = "IMG_${timestamp}.jpeg"
        photoName = name
        val pf = File(requireContext().filesDir, photoName)
        photoFile = pf
        pf.let { file ->
            val photoUri = FileProvider.getUriForFile(requireContext(),
                "com.example.pickuppal.fileprovider", file)
            takePhoto.launch(photoUri)

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
        val imageBitmapState = remember { mutableStateOf<ImageBitmap?>(null)}
        val navController = findNavController()
        val context = LocalContext.current
        val viewModel: SharedViewModel by activityViewModels()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFF5F5F5))
        ){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a Title", color = Color.Gray) }
                )
                Button(
                    onClick = { launchTakePicture() }
                ) {
                    Text("Take a Photo")
                }
                OutlinedTextField(
                    value = locationState.value,
                    onValueChange = { locationState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a Location", color = Color.Gray) }
                )
                OutlinedTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a Description", color = Color.Gray) }
                )

                Button(
                    onClick = {
                        if (hasRequiredInputs(titleState.value.text, locationState.value.text, photoUri)) {
                            val firebaseAPI = FirebaseAPI()
                            photoUri?.let { uri ->
                                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                val name:String = photoName?: "defaultphotoname"
                                firebaseAPI.uploadImage(bitmap, name) { imageUrl ->
                                    if (imageUrl != null) {
                                        val data = PostingData(
                                            userID = user.userId,
                                            title = titleState.value.text,
                                            location = locationState.value.text,
                                            description = descriptionState.value.text,
                                            claimed = false,
                                            photoUrl = imageUrl
                                        )
                                        firebaseAPI.uploadPostingData(data, user)
                                        navController.popBackStack()

                                        val location = locationState.value.text
                                        viewModel.setNewLocation(location)

                                    }else {
                                        Toast.makeText(context, "Error. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }


                        } else {
                            Toast.makeText(context, "Please fill in title, location, and image", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Post")
                }
            }

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
}

