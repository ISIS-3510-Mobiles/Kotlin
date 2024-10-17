package com.example.ecostyle.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
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
import com.example.ecostyle.view.SustainabilityFragment
import com.example.ecostyle.view.CheckoutFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.Calendar
import android.location.Location
import android.widget.ImageButton
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
    private var sessionStartTime: Long = 0L
    private var sessionLatitude: Double? = null
    private var sessionLongitude: Double? = null

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val db = FirebaseFirestore.getInstance()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializa 'drawer' primero
        drawer = findViewById(R.id.drawer_layout)

        // Inicializa 'toolbar' después
        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        // Inicializa 'toggle' después de 'drawer' y 'toolbar'
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navegation_drawer_open,
            R.string.navegation_drawer_close
        )

        // Escalar el icono del menú de hamburguesa
        val arrowDrawable = toggle.drawerArrowDrawable
        arrowDrawable.barLength = 80f   // Aumentar la longitud de las barras
        arrowDrawable.barThickness = 8f  // Ajustar el grosor de las barras
        arrowDrawable.gapSize = 10f

        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)

        drawer.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Configura el NavigationView
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        sessionStartTime = savedInstanceState?.getLong("SESSION_START_TIME") ?: System.currentTimeMillis()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)

        // Verifica si el Intent contiene el extra para abrir el CheckoutFragment
        val openFragment = intent.getStringExtra("openFragment")
        if (openFragment == "checkout") {
            // Si debe abrir el CheckoutFragment, lo hacemos aquí
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CheckoutFragment())
                .commit()
        } else {
            // Si no, cargamos el ListFragment por defecto
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ListFragment())
                    .commit()
            }
        }




        FirebaseApp.initializeApp(this)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        db.firestoreSettings = settings
        Log.d("FirestoreInit", "Firestore inicializado correctamente")

        FirebaseFirestore.setLoggingEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email == null || provider == null) {
            val authIntent = Intent(this, AuthActivity::class.java)
            startActivity(authIntent)
            finish()
            return
        }

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

        val button2 = findViewById<ImageButton>(R.id.button2)
        button2.setOnClickListener {
            Toast.makeText(this, "Cart", Toast.LENGTH_SHORT).show()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CheckoutFragment())
                .commit()

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
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        when (item.itemId) {
            R.id.nav_item_1 -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ListFragment())
                    .commit()
            }
            R.id.nav_item_2 -> {

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CheckoutFragment())
                    .commit()
            }
            R.id.nav_item_3 -> {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_item_4 -> {

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SustainabilityFragment())
                    .commit()
            }
            R.id.nav_item_5 -> {
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
                sessionLatitude = location.latitude
                sessionLongitude = location.longitude

                Log.d("Location", "Latitude: $sessionLatitude, Longitude: $sessionLongitude")

                // Logging SCREEN_VIEW event with location data
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, "HomeActivity")
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeActivity")
                    putString("session_start_time", System.currentTimeMillis().toString())
                    putDouble("latitude", sessionLatitude ?: 0.0)
                    putDouble("longitude", sessionLongitude ?: 0.0)
                })

                // Logging user activity with context-aware data
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val bundle = Bundle().apply {
                    putInt("day_of_week", dayOfWeek)
                    putDouble("latitude", sessionLatitude ?: 0.0)
                    putDouble("longitude", sessionLongitude ?: 0.0)
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
                Toast.makeText(this, "Location permission denied. Please enable it in settings for full functionality.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val sessionEndTime = System.currentTimeMillis()
        val sessionDuration = sessionEndTime - sessionStartTime

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val durationBundle = Bundle().apply {
            putLong("user_session_duration", sessionDuration)
        }
        firebaseAnalytics.logEvent("session_duration", durationBundle)
        Log.d("FirebaseAnalytics", "Logging session_duration event: $sessionDuration ms")

        val timeBundle = Bundle().apply {
            putLong("session_start_time", sessionStartTime)
            putLong("session_end_time", sessionEndTime)
        }
        firebaseAnalytics.logEvent("session_time", timeBundle)
        Log.d("FirebaseAnalytics", "Logging session_time event: start=$sessionStartTime, end=$sessionEndTime")

        val dayBundle = Bundle().apply {
            putInt("day_of_week", dayOfWeek)
        }
        firebaseAnalytics.logEvent("session_day", dayBundle)
        Log.d("FirebaseAnalytics", "Logging session_day event: day of week=$dayOfWeek")

        sessionLatitude?.let { latitude ->
            sessionLongitude?.let { longitude ->
                val locationBundle = Bundle().apply {
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                }
                firebaseAnalytics.logEvent("session_location", locationBundle)
                Log.d("FirebaseAnalytics", "Logging session_location event: lat=$latitude, long=$longitude")
            }
        }

        val sessionBundle = Bundle().apply {
            putLong("user_session_duration", sessionDuration)
            putLong("session_start_time", sessionStartTime)
            putLong("session_end_time", sessionEndTime)
            putInt("day_of_week", dayOfWeek)
            sessionLatitude?.let { putDouble("latitude", it) }
            sessionLongitude?.let { putDouble("longitude", it) }
        }
        firebaseAnalytics.logEvent("session_info", sessionBundle)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("SESSION_START_TIME", sessionStartTime)
    }
}
