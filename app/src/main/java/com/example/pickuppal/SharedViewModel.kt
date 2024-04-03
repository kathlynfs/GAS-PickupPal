package com.example.pickuppal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel()
{
    private var currentFragment = MutableLiveData<String>()
    fun getCurrentFragment(): MutableLiveData<String>
    {
        return currentFragment
    }
    fun setCurrentFragment(frag: String)
    {
        currentFragment.value = frag
    }
}