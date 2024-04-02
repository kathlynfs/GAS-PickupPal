package com.example.pickuppal

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
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


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.example.pickuppal.PostingFragment

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
//    private val googleOAuthClient by lazy {
//        GoogleOAuthClient(
//            context = applicationContext,
//            oneTapClient = Identity.getSignInClient(applicationContext)
//        )
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        // create boolean to determine whether or not map should be shown at a given time
        // for initial testing, use true if you want to view map, false if not
        if (true) {
            if (mapFragment == null) {
                val newMapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .add(R.id.map, newMapFragment)
                    .commit()
            }
        } else {
            if (mapFragment != null) {
                supportFragmentManager.beginTransaction()
                    .remove(mapFragment)
                    .commit()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .title("Marker")
        )
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