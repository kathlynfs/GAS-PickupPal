package com.example.pickuppal

import FirebaseAPI
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import java.io.File


class PostingFragment : Fragment() {
    private val args: PostingFragmentArgs by navArgs()
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val user = args.user
        takePictureLauncher = takePhoto
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

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto ->
        Log.d("TakePhotoCallback", "didTakePhoto: $didTakePhoto")
        Log.d("TakePhotoCallback", "photoName: $photoName")
        if (didTakePhoto && photoName != null) {
            photoUri = Uri.fromFile(File(requireContext().filesDir, photoName))
            Log.d("TakePhotoCallback", "photoUri: $photoUri")
        } else {
            Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        val timestamp = System.currentTimeMillis()
        val name = "IMG_${timestamp}.jpg"
        photoName = name
        val photoFile = File(requireContext().filesDir, photoName)
        photoFile?.let { file ->
            val photoUri = FileProvider.getUriForFile(requireContext(),
                "com.example.pickuppal.fileprovider", file)
            takePictureLauncher.launch(photoUri)
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
                onClick = { dispatchTakePictureIntent() }
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
                            description = description, claimed = false
                        )

                    if (hasRequiredInputs(data)) {
                        val firebaseAPI = FirebaseAPI()
                        firebaseAPI.uploadPostingData(data)
                        photoUri?.let {
                            uri -> firebaseAPI.uploadImage(uri)
                        } ?: kotlin.run {
                            Toast.makeText(context, "No photo", Toast.LENGTH_SHORT)
                        }
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Please fill in title and location", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Post")
            }
        }
    }


    private fun hasRequiredInputs(data: PostingData): Boolean {
        return data.title.isNotBlank()
                && data.location.isNotBlank()
                && data.userID.isNotBlank()
    }
}

