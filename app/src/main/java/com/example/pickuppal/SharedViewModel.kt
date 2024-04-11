package com.example.pickuppal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng
import java.lang.reflect.Array


class SharedViewModel : ViewModel()
{
    private var postingDataList = MutableLiveData<MutableList<PostingData>>()

    fun addToPostingDataList(postingData: PostingData)
    {
        if(postingDataList.value == null)
        {
            postingDataList.value = mutableListOf()
            postingDataList.value!!.add(postingData)
        }
        else {
            postingDataList.value!!.add(postingData)
        }
    }

    fun deleteFromPostingDataList(postingData: PostingData)
    {

    }

    fun getPostingDataList(): MutableLiveData<MutableList<PostingData>>
    {
        return postingDataList
    }



}