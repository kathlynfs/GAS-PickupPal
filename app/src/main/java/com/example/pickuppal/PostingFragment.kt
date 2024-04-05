package com.example.pickuppal

import FirebaseAPI
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class PostingFragment : Fragment() {
    private val args: PostingFragmentArgs by navArgs()
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

    @Composable
    private fun PostingContent(
        user: UserData,
        onBackPressed: () -> Unit
    ) {
        val titleState = remember { mutableStateOf(TextFieldValue()) }
        val locationState = remember { mutableStateOf(TextFieldValue()) }
        val descriptionState = remember { mutableStateOf(TextFieldValue()) }
        val navController = findNavController()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = titleState.value,
                onValueChange = { titleState.value = it },
                label = { Text("Title") }
            )
            OutlinedTextField(
                value = locationState.value,
                onValueChange = { locationState.value = it },
                label = { Text("Location") }
            )
            OutlinedTextField(
                value = descriptionState.value,
                onValueChange = { descriptionState.value = it },
                label = { Text("Description") }
            )
            Button(
                onClick = {
                    val userID = user.userId
                    val title = titleState.value.text
                    val location = locationState.value.text
                    val description = descriptionState.value.text
                    val data =
                        PostingData(userID = userID, title = title, location = location,
                            description = description, claimed = false)

                    if (hasRequiredInputs(data)) {
                        val firebaseAPI = FirebaseAPI()
                        firebaseAPI.uploadPostingData(data)
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

