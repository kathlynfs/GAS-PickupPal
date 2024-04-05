package com.example.pickuppal

import android.os.Bundle
import android.provider.ContactsContract.Profile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import coil.compose.rememberAsyncImagePainter
import kotlin.math.ceil
import kotlin.math.floor

class ProfileFragment : Fragment() {
    private val args: ProfileFragmentArgs by navArgs()

    val listItems = listOf(
        ListingItem(
            title = "Cozy Apartment",
            description = "A cozy apartment with modern amenities, perfect for a couple or solo traveler."
        ),
        ListingItem(
            title = "Spacious House",
            description = "A spacious house with a beautiful backyard, ideal for families or groups."
        ),
        ListingItem(
            title = "Luxury Villa",
            description = "Experience luxury living in this stunning villa with a private pool and breathtaking views."
        ),
        ListingItem(
            title = "Downtown Studio",
            description = "A stylish studio apartment in the heart of the city, close to restaurants and attractions."
        ),
        ListingItem(
            title = "Beachfront Condo",
            description = "Enjoy a relaxing getaway in this beachfront condo with direct access to the sand and sea."
        ),
        ListingItem(
            title = "Mountain Retreat",
            description = "Escape to the mountains in this cozy retreat, surrounded by nature and hiking trails."
        ),
        ListingItem(
            title = "Historic Townhouse",
            description = "Step back in time in this charming historic townhouse, updated with modern comforts."
        ),
        ListingItem(
            title = "Rustic Cabin",
            description = "Unplug and unwind in this rustic cabin, nestled in the woods with a fireplace and hot tub."
        ),
        ListingItem(
            title = "Urban Loft",
            description = "Live like a local in this trendy urban loft, with high ceilings and industrial chic decor."
        ),
        ListingItem(
            title = "Lakefront Cottage",
            description = "Relax and recharge in this quaint lakefront cottage, with a private dock and stunning views."
        )
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val user = args.user
        return ComposeView(requireContext()).apply {
            setContent {
                ProfileScreen(
                    user = user,
                    onBackPressed = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                )
            }
        }
    }
    @Composable
    fun ProfileScreen(
        user: UserData,
        onBackPressed: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackPressed
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ProfilePicture(
                            modifier = Modifier.size(48.dp),
                            imageUrl = user.profilePictureUrl!!
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = user.username ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.5",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "10",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.Black
                        )
                        Text(
                            text = "Items Posted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.Black
                        )
                        Text(
                            text = "Items Claimed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "Listings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listItems) { item ->
                        ListingCard(item = item)
                    }
                }
            }
        }
    }

    @Composable
    fun ProfilePicture(
        modifier: Modifier = Modifier,
        imageUrl: String
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )
    }

    @Composable
    fun ListingCard(item: ListingItem) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    data class ListingItem(
        val title: String,
        val description: String
    )
}