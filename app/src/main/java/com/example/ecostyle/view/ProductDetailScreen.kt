package com.example.ecostyle.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ecostyle.R
import com.example.ecostyle.viewmodel.ProductViewModel
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Alignment
import com.example.ecostyle.model.Product
import com.example.ecostyle.ui.theme.EcoStyleTheme

@Composable
fun ProductDetailScreen(product: Product) {
    Scaffold(
        topBar = { TopBar() },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Product Title
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                )

                // Product Image
                Image(
                    painter = painterResource(id = R.drawable.buzouniandes),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(bottom = 16.dp)
                )

                // Price and Heart Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Price
                    Text(
                        text = product.price,
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary
                    )
                    //Heart icon
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Product Description
                Text(
                    text = "Uniandes jacket size XL. I changed to the Nacho, I no longer use the jacket",  // Update as needed
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Buttons: Comprar and Agregar al Carrito
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = "Buy", color = MaterialTheme.colors.onPrimary)
                }

                Button(
                    onClick = {  },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "Add to Cart", color = MaterialTheme.colors.onSecondary)
                }
            }
        }
    )
}


@Composable
fun TopBar() {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.primary,  // Dark green background
        title = {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search in EcoStyle") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    focusedBorderColor = MaterialTheme.colors.onSurface,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(50.dp)
            )
        },
        navigationIcon = {
            // Menu icon
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            // Shopping cart icon
            IconButton(onClick = {}) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewProductDetailScreen() {
    EcoStyleTheme {
        // Mock product data for preview
        val sampleProduct = Product(
            name = "Uniandes Jacket",
            price = "$120 000",
            description = "Uniandes jacket size XL. I changed to the Nacho, I no longer use the jacket",
            imageUrl = "https://via.placeholder.com/150"
        )
        ProductDetailScreen(product = sampleProduct)
    }
}