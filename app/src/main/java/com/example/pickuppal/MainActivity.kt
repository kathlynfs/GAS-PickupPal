package com.example.pickuppal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

import com.example.pickuppal.PostingFragment
import com.google.android.gms.maps.SupportMapFragment
import kotlin.math.sign

class MainActivity : AppCompatActivity(), CurrentLocationDeterminer {
//    private val googleOAuthClient by lazy {
//        GoogleOAuthClient(
//            context = applicationContext,
//            oneTapClient = Identity.getSignInClient(applicationContext)
//        )
//    }

    private val FINE_PERMISSION_CODE = 1
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        sharedViewModel.setCurrentFragment("signin")

        val fragmentManager = supportFragmentManager
        fragmentManager.commit{
            setReorderingAllowed(true)
        }

        // create instances of new fragments here
        val signInFragment = fragmentManager.findFragmentById(R.id.sign_in_fragment_container) as SignInFragment
        val mapFragment = fragmentManager.findFragmentById(R.id.map_fragment_container) as MapFragment
        val postingFragment = fragmentManager.findFragmentById(R.id.posting_fragment_container) as PostingFragment

        // while not all transitions between fragments are set up,
        // feel free to manually change what fragment should be shown
        // by updating setCurrentFragment()
        // ex: sharedViewModel.setCurrentFragment("posting")
        // to have app open on posting screen
        sharedViewModel.getCurrentFragment().observe(this) { frag ->
            if (frag == "signin")
            {
                fragmentManager.beginTransaction()
                    .attach(signInFragment)
                    .detach(mapFragment)
                    .detach(postingFragment)
                    .commit()
            }
            else if(frag == "map")
            {
                fragmentManager.beginTransaction()
                    .detach(signInFragment)
                    .attach(mapFragment)
                    .detach(postingFragment)
                    .commit()
            }
            else if (frag == "posting")
            {
                fragmentManager.beginTransaction()
                    .detach(signInFragment)
                    .detach(mapFragment)
                    .attach(postingFragment)
                    .commit()
            }
        }
    }


    override fun determineCurrentLocation(): Task<Location> {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        return getLastLocation()
    }

    private fun getLastLocation(): Task<Location>
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(), FINE_PERMISSION_CODE)
        }

        return fusedLocationProviderClient.getLastLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == FINE_PERMISSION_CODE)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getLastLocation()
            }
            else
            {
                Toast.makeText(
                    this,
                    R.string.location_permission_denied,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }



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