package com.example.pickuppal

import FirebaseAPI
import PostingDataListCallBack
import UserStatisticsCallback
import android.os.Bundle
import android.provider.ContactsContract.Profile
import android.util.Log
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
import androidx.compose.material.icons.filled.Delete
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
    private val firebaseAPI = FirebaseAPI()
    private val listItems = mutableListOf<ListingItem>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val user = args.user
        val composeView = ComposeView(requireContext())

        firebaseAPI.getUserStatistics(user, object : UserStatisticsCallback {
            override fun onUserStatisticsReceived(userStatistics: UserStatistics) {
                firebaseAPI.getPostingDataList(user, object : PostingDataListCallBack {
                    override fun onPostingDataListReceived(postingDataList: List<PostingData>) {
                        listItems.clear()
                        postingDataList.forEach { postingData ->
                            val listingItem = ListingItem(
                                dataId = postingData.postID,
                                title = postingData.title,
                                description = postingData.description,
                                location = postingData.location
                            )
                            listItems.add(listingItem)
                        }
                        composeView.setContent {
                            ProfileScreen(
                                user = user,
                                userStatistics = userStatistics,
                                listItems = listItems,
                                onDeleteClick = { dataId ->
                                    firebaseAPI.deletePostingData(user, dataId)
                                },
                                onBackPressed = {
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                }
                            )
                        }
                    }

                    override fun onPostingDataListError(e: Exception) {
                        Log.e("TAG", "Error retrieving posting data list", e)
                    }
                })
            }

            override fun onUserStatisticsError(e: Exception) {
                Log.e("TAG", "Error retrieving user statistics", e)
                composeView.setContent {
                    // Show an error message or handle the error scenario
                    Text("Error retrieving user statistics")
                }
            }
        })
        composeView.setContent {
            // Show a loading indicator or placeholder UI
            Text("Loading user statistics...")
        }

        return composeView
    }
    @Composable
    fun ProfileScreen(
        user: UserData,
        userStatistics: UserStatistics?,
        listItems: List<ListingItem>,
        onDeleteClick: (String) -> Unit,

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
                            text = userStatistics?.averageRating?.toString() ?: "0.0",
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
                            text = userStatistics?.numItemsPosted?.toString() ?: "0",
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
                            text = userStatistics?.numItemsClaimed?.toString() ?: "0",
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
                        ListingCard(
                            item = item,
                            onDeleteClick = onDeleteClick
                        )
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
    fun ListingCard(item: ListingItem, onDeleteClick: (String) -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(
                        onClick = {
                            Log.d("TAG", item.dataId)
                            onDeleteClick(item.dataId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    data class ListingItem(
        val dataId: String,
        val title: String,
        val location: String,
        val description: String
    )
}