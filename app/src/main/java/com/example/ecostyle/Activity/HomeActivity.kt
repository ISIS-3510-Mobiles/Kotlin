package com.example.ecostyle.Activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ecostyle.R
import com.example.ecostyle.view.ListFragment
import com.example.ecostyle.view.ProfileFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices


enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var firebaseAnalytics: FirebaseAnalytics


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)


        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email == null || provider == null) {
            val authIntent = Intent(this, AuthActivity::class.java)
            startActivity(authIntent)
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navegation_drawer_open,
            R.string.navegation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val btn_logout = findViewById<Button>(R.id.btn_logout)
        btn_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val prefsEditor = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefsEditor.clear()
            prefsEditor.apply()

            val authIntent = Intent(this, AuthActivity::class.java)
            startActivity(authIntent)
            finish()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation() // Permissions granted, proceed to get location
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ListFragment())
                .commit()
        }
        drawer.openDrawer(GravityCompat.START)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        when (item.itemId) {
            R.id.nav_item_1 -> {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ListFragment())
                    .commit()
            }
            R.id.nav_item_2 -> {
                Toast.makeText(this, "Cart", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_item_3 -> {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_item_4 -> {
                Toast.makeText(this, "Sustainability", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_item_5 -> {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()

                val profileFragment = ProfileFragment()
                val bundle = Bundle().apply {
                    putString("email", email)
                    putString("provider", provider)
                }
                profileFragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .commit()
            }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getLastLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, "HomeActivity")
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeActivity")
                    putString("session_start_time", System.currentTimeMillis().toString())
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                })
                Log.d("FirebaseAnalytics", "Logged SCREEN_VIEW event with latitude: $latitude, longitude: $longitude")

                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val bundle = Bundle().apply {
                    putInt("day_of_week", dayOfWeek)
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                }
                firebaseAnalytics.logEvent("user_activity", bundle)
                Log.d("FirebaseAnalytics", "Logged user_activity event with bundle: $bundle")

            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                Log.e("HomeActivity", "Location is null")
            }
        }.addOnFailureListener { exception ->
            Log.e("HomeActivity", "Failed to get location: ${exception.message}")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Please enable it in settings for full functionality.", Toast.LENGTH_LONG).show()            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("HomeActivity", "onStop called")

        firebaseAnalytics.logEvent("session_end", Bundle().apply {
            putString("session_end_time", System.currentTimeMillis().toString())
        })
        Log.d("HomeActivity", "Logging session_end event")
    }

}
