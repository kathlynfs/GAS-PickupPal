package com.example.pickuppal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

import com.example.pickuppal.PostingFragment

class MainActivity : AppCompatActivity() {
//    private val googleOAuthClient by lazy {
//        GoogleOAuthClient(
//            context = applicationContext,
//            oneTapClient = Identity.getSignInClient(applicationContext)
//        )
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            val viewModel = viewModel<SignInViewModel>()
//            val state by viewModel.state.collectAsStateWithLifecycle()
//            val launcher = rememberLauncherForActivityResult(
//                contract = ActivityResultContracts.StartIntentSenderForResult(),
//                onResult = { result ->
//                    if (result.resultCode == RESULT_OK) {
//                        lifecycleScope.launch {
//                            val signInResult = googleOAuthClient.signInWithIntent(
//                                intent = result.data ?: return@launch
//                            )
//                            viewModel.onSignInResult(signInResult)
//                        }
//                    }
//                }
//            )
//
//            LaunchedEffect(key1 = state.isSignInSuccessful) {
//                if (state.isSignInSuccessful) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Sign in successful",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//            SignInScreen(state = state,
//                onSignInClick = {
//                    lifecycleScope.launch {
//                        val signInIntent = googleOAuthClient.signIn()
//                        launcher.launch(
//                            IntentSenderRequest.Builder(
//                                signInIntent ?: return@launch
//                            ).build()
//                        )
//                    }
//                }
//            )
//        }
//    }
}