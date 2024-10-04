package com.example.ecostyle.Activity

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.ProductDetailViewModel
import com.bumptech.glide.Glide
import com.example.ecostyle.view.ListFragment
import com.example.ecostyle.view.ProfileFragment
import com.google.android.material.navigation.NavigationView
import android.content.res.Configuration
import android.content.Context


class ProductDetailActivity : AppCompatActivity() {

    private val viewModel: ProductDetailViewModel by viewModels()
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

/*
        // Find toolbar and set it
        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        // Set up drawer
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

        // Set up navigation view
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

*/
        val productImage = findViewById<ImageView>(R.id.product_detail_image)
        val productName = findViewById<TextView>(R.id.product_detail_title)
        val productPrice = findViewById<TextView>(R.id.product_detail_price)
        val productDescription = findViewById<TextView>(R.id.product_detail_description)
        val favoriteButton = findViewById<ImageButton>(R.id.favorite_icon)
        val addToCartButton = findViewById<Button>(R.id.btn_add_to_cart)

        val productId = intent.getIntExtra("PRODUCT_ID",-1)

        Log.d("ProductDetailActivity", "Received productId: $productId")
        if (productId == -1) {
            Log.e("ProductDetailActivity", "Invalid productId received!")
        }

        viewModel.product.observe(this) { product ->
            Log.d("ProductDetailActivity", "Observed product: $product")
            productName.text = product.name
            productPrice.text = product.price
            productDescription.text = product.description

            Glide.with(this)
                .load(product.imageResource)
                .into(productImage)

            favoriteButton.setImageResource(
                if (product.isFavorite == true) R.drawable.baseline_favorite_24
                else R.drawable.baseline_favorite_border_24
            )
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        addToCartButton.setOnClickListener {
        }
    }
/*
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
    */
}
