package com.example.pickuppal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private val googleOAuthClient by lazy {
        GoogleOAuthClient(
            context = requireContext(),
            oneTapClient = Identity.getSignInClient(requireContext())
        )
    }

    private val viewModel: SignInViewModel by activityViewModels()

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            lifecycleScope.launch {
                val signInResult = googleOAuthClient.signInWithIntent(
                    intent = result.data ?: return@launch
                )
                viewModel.onSignInResult(signInResult)
            }
        }
    }

    private lateinit var signInButton: Button
    private lateinit var logoutButton: Button
    private lateinit var userNameTextView: TextView
    private lateinit var profilePictureImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        signInButton = view.findViewById(R.id.signInButton)
        userNameTextView = view.findViewById(R.id.usernameTextView)
        logoutButton = view.findViewById(R.id.logoutButton)
        profilePictureImageView = view.findViewById(R.id.profilePictureImageView)

        signInButton.setOnClickListener {
            lifecycleScope.launch {
                val signInIntent = googleOAuthClient.signIn()
                launcher.launch(
                    IntentSenderRequest.Builder(signInIntent ?: return@launch).build()
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.isSignInSuccessful) {
                        val userData : UserData? = googleOAuthClient.getSignedInUser()
                        val user: UserData = userData!!
                        val action = SignInFragmentDirections.signInToMap(user)
                        findNavController().navigate(action)
                    } else if (state.isSignInAttempted) {
                        viewModel.resetState()
                        Toast.makeText(
                            requireContext(),
                            "Sign in unsuccessful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


        return view
    }
}