package com.bs.inavitest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.bs.inavitest.Adapter.FragmentAdapter
import com.bs.inavitest.Fragment.MapFragment
import com.bs.inavitest.Fragment.ModeFragment
import com.bs.inavitest.Fragment.ProfileFragment
import com.bs.inavitest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var accessToken: String
    }

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    val fragmentList by lazy { listOf(MapFragment(), ModeFragment(), ProfileFragment()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        accessToken = "Bearer " + intent.getStringExtra("accessToken").toString()

        val adapter = FragmentAdapter(this)
        adapter.fragmentList = fragmentList
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.bottomNavigationView.setOnItemSelectedListener { navigationSelected(it) }
    }

    private fun navigationSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.map -> {
                binding.viewPager.setCurrentItem(0, false)
            }
            R.id.mode -> {
                binding.viewPager.setCurrentItem(1, false)
            }
            R.id.profile -> {
                binding.viewPager.setCurrentItem(2, false)
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.bottomNavigationView.selectedItemId = R.id.map
        }
    }
}