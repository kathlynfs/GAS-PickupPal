package com.example.pickuppal

import FirebaseAPI
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
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
                    val userID = user.userId
                    val title = titleState.value.text
                    val location = locationState.value.text
                    val description = descriptionState.value.text
                    val data =
                        PostingData(
                            userID = userID, title = title, location = location,
                            description = description, claimed = false, photoUrl = "no picture yet"
                        )

                    if (hasRequiredInputs(data)) {
                        val firebaseAPI = FirebaseAPI()
                        firebaseAPI.uploadPostingData(data)
                        photoUri?.let { uri ->
                            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                            val name:String = photoName?: "defaultphotoname"
                            firebaseAPI.uploadImage(bitmap, name, data)
                        }
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Please fill in title and location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun hasRequiredInputs(data: PostingData): Boolean {
        return data.title.isNotBlank()
                && data.location.isNotBlank()
                && data.userID.isNotBlank()
                && photoName != null
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

