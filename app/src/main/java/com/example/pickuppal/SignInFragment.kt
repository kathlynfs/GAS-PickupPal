package com.example.pickuppal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SignInScreen(
                    onSignInClick = {
                        lifecycleScope.launch {
                            val signInIntent = googleOAuthClient.signIn()
                            launcher.launch(
                                IntentSenderRequest.Builder(signInIntent ?: return@launch).build()
                            )
                        }
                    },
                    onSignInSuccess = { userData ->
                        val action = SignInFragmentDirections.signInToMap(userData)
                        findNavController().navigate(action)
                    },
                    onSignInFailure = {
                        Toast.makeText(
                            requireContext(),
                            "Sign in unsuccessful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

            }
        }
    }

    @Composable
    fun SignInScreen(
        onSignInClick: () -> Unit,
        onSignInSuccess: (UserData) -> Unit,
        onSignInFailure: () -> Unit
    ) {
        val context = LocalContext.current
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.isSignInSuccessful) {
            if (state.isSignInSuccessful) {
                val userData: UserData? = googleOAuthClient.getSignedInUser()
                if (userData != null) {
                    onSignInSuccess(userData)
                } else {
                    onSignInFailure()
                }
            } else if (state.isSignInAttempted) {
                viewModel.resetState()
                onSignInFailure()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PickUp Pal",
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            )
            ExtendedFloatingActionButton(
                onClick = onSignInClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Sign In")
            }

        }
    }
}