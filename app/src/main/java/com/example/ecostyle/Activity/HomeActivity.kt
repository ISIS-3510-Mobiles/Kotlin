package com.example.ecostyle.Activity

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
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.example.ecostyle.R
import com.example.ecostyle.view.ListFragment
import com.example.ecostyle.view.ProfileFragment
import com.example.ecostyle.view.SustainabilityFragment
import com.example.ecostyle.view.CheckoutFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

enum class ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        // Verifica si el Intent contiene el extra para abrir el CheckoutFragment
        val openFragment = intent.getStringExtra("openFragment")
        if (openFragment == "checkout") {
            // Si debe abrir el CheckoutFragment, lo hacemos aquí
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CheckoutFragment())
                .commit()
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

        // Cargar datos de sesión de SharedPreferences
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
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CheckoutFragment())
                    .commit()
            }
            R.id.nav_item_3 -> {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_item_4 -> {
                Toast.makeText(this, "Sustainability", Toast.LENGTH_SHORT).show()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SustainabilityFragment())
                    .commit()
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

}

