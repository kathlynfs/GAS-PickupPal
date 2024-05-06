package com.example.pickuppal

import FirebaseAPI
import PostingDataListCallBack
import UserStatisticsCallback
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

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
                                location = postingData.location,
                                imageUrl =  postingData.photoUrl
                            )
                            listItems.add(listingItem)
                        }
                        composeView.setContent {
                            ProfileScreen(
                                user = user,
                                userStatistics = userStatistics,
                                initialListItems = listItems,
                                onDeleteClick = { dataId ->
                                    firebaseAPI.deletePostingData(user, dataId)
                                },
                                onBackPressed = {
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                },
                                onLogoutClick = {
                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    requireActivity().finish()
                                    Runtime.getRuntime().exit(0)
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
        initialListItems: List<ListingItem>,
        onDeleteClick: (String) -> Unit,
        onBackPressed: () -> Unit,
        onLogoutClick: () -> Unit

    ) {
        val listItems = remember { mutableStateListOf<ListingItem>().apply { addAll(initialListItems) } }
        val numItemsPosted = remember { mutableStateOf(userStatistics?.numItemsPosted ?: 0) }

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
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                onLogoutClick()
                            }
                        },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("Log Out")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if ((userStatistics?.numRatings ?: 0)> 0) {
                                String.format("%.2f", userStatistics?.totalRating?.toDouble()?.div(userStatistics?.numRatings ?: 1) ?: 0.0)
                            } else {
                                "0.00"
                            },
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
                            text = numItemsPosted.value.toString(),
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
                            onDeleteClick = { dataId ->
                                onDeleteClick(dataId)
                                listItems.removeIf { it.dataId == dataId }
                                numItemsPosted.value--
                            }
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

                if (!item.imageUrl.isNullOrEmpty()) {
                    // Display the image if imageUrl is not null or empty
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Listing Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(top = 16.dp),
                        contentScale = ContentScale.Crop
                    )
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
        val description: String,
        val imageUrl: String,
    )
}