package com.example.ecostyle.view

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

class ProductDetailActivity : AppCompatActivity() {

    private val viewModel: ProductDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val productImage = findViewById<ImageView>(R.id.product_detail_image)
        val productName = findViewById<TextView>(R.id.product_detail_title)
        val productPrice = findViewById<TextView>(R.id.product_detail_price)
        val productDescription = findViewById<TextView>(R.id.product_detail_description)
        val favoriteButton = findViewById<ImageButton>(R.id.favorite_icon)
        val buyButton = findViewById<Button>(R.id.btn_buy)
        val addToCartButton = findViewById<Button>(R.id.btn_add_to_cart)

        val productId = intent.getIntExtra("PRODUCT_ID",-1)
        Log.d("ProductDetailActivity", "Received productId: $productId")

        viewModel.product.observe(this) { product ->
            productName.text = product.name
            productPrice.text = product.price
            productDescription.text = product.description
            productImage.setImageResource(product.imageResource)

            favoriteButton.setImageResource(
                if (product.isFavorite) R.drawable.baseline_favorite_24
                else R.drawable.baseline_favorite_border_24
            )
        }

        viewModel.loadProduct(productId)

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        buyButton.setOnClickListener {
        }

        addToCartButton.setOnClickListener {
        }
    }
}
