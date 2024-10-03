package com.example.ecostyle.Activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.ProductDetailViewModel
import com.bumptech.glide.Glide


class ProductDetailActivity : AppCompatActivity() {

    private val viewModel: ProductDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        Log.d("ProductDetailActivity", "onCreate called")


        val productImage = findViewById<ImageView>(R.id.product_detail_image)
        val productName = findViewById<TextView>(R.id.product_detail_title)
        val productPrice = findViewById<TextView>(R.id.product_detail_price)
        val productDescription = findViewById<TextView>(R.id.product_detail_description)
        val favoriteButton = findViewById<ImageButton>(R.id.favorite_icon)

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
/*
            Glide.with(this)
                .load(product.imageResource)
                .into(productImage)
*/
            favoriteButton.setImageResource(
                if (product.isFavorite == true) R.drawable.baseline_favorite_24
                else R.drawable.baseline_favorite_border_24
            )
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("ProductDetailActivity", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ProductDetailActivity", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ProductDetailActivity", "onDestroy called")
    }
}
