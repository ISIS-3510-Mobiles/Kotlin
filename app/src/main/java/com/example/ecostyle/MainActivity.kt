package com.example.ecostyle

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.Visibility
import com.example.ecostyle.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
//import androidx.compose.material:material-icons-extended
//import androidx.compose.material.Icon
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Menu
//import androidx.compose.material.icons.filled.ShoppingCart
//import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.ecostyle.ui.theme.EcoStyleTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcoStyleTheme {
                MaterialTheme {
                    val sampleProduct = Product(
                        name = "Uniandes Jacket",
                        price = "$120 000",
                        description = "Uniandes jacket size XL. I changed to the Nacho, I no longer use the jacket",
                        imageUrl = "https://via.placeholder.com/150"
                    )
                    ProductDetailScreen(sampleProduct)
                }
            }
        }
    }
}

data class Product(
    val name: String,
    val price: String,
    val description: String,
    val imageUrl: String
)


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

                    // Heart icon (commented out for now)
                    /*
                    IconButton(onClick = { /* Handle favorite click */ }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                    }
                    */
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
            // Update the TextField to be more visible
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
            // Menu icon (commented for now)
            /*
            IconButton(onClick = { /* Handle menu click */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            */
        },
        actions = {
            // Shopping cart icon (commented for now)
            /*
            IconButton(onClick = { /* Handle cart click */ }) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
            */
        }
    )
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PreviewProductListScreen() {
    EcoStyleTheme {
        MaterialTheme {
            val sampleProduct = Product(
                name = "Uniandes Jacket",
                price = "$120 000",
                description = "Uniandes jacket size XL. I changed to the Nacho, I no longer use the jacket",
                imageUrl = "https://via.placeholder.com/150"
            )
            ProductDetailScreen(sampleProduct)
        }
    }
}