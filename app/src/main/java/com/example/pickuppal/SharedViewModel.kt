package com.example.pickuppal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


class SharedViewModel : ViewModel()
{
    private var currentFragment = MutableLiveData<Int>()
    fun getCurrentFragment(): MutableLiveData<Int>
    {
        return currentFragment
    }
    fun setCurrentFragment(activity: FragmentActivity, fragment: Fragment) {
        val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()

    }
}