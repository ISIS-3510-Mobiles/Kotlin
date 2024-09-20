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
//import androidx.compose.material:material-icons-extended
//import androidx.compose.material.Icon
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Menu
//import androidx.compose.material.icons.filled.ShoppingCart
//import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.ecostyle.ui.theme.EcoStyleTheme

class MainActivity : ComponentActivity() {  // Changed to ComponentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // MyFirstComposeApp()
            EcoStyleTheme {
                MaterialTheme { // Fallback to ensure proper preview rendering
                    val sampleProduct = Product(
                        name = "Saco uniandes",
                        price = "$120 000",
                        description = "Saco uniandes talla XL. Me cambié a la nacho, ya no uso el saco",
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
        topBar = { TopBar() },  // Reuse the top bar with the search functionality
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colors.background)  // Apply background color
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Product Title
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                )
/*
                // Product Image
                Image(
                    painter = rememberImagePainter(data = product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)  // Adjust the image size
                        .padding(bottom = 16.dp)
                )
*/
                // Price and Heart Icon Row
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
                        color = MaterialTheme.colors.onBackground
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
                    text = "Saco uniandes talla XL. Me cambié a la nacho, ya no uso el saco",  // Update as needed
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Buttons: Comprar and Agregar al Carrito
                Button(
                    onClick = { /* Handle Comprar click */ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = "Comprar", color = MaterialTheme.colors.onPrimary)
                }

                Button(
                    onClick = { /* Handle Agregar al Carrito click */ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "Agregar al Carrito", color = MaterialTheme.colors.onSecondary)
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
                value = "",  // Add a default value if needed
                onValueChange = {},
                placeholder = { Text("Busca en EcoStyle") },  // Ensure the placeholder is visible
                shape = RoundedCornerShape(24.dp),  // Round the edges of the search bar
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,  // Make it stand out against the background
                    focusedBorderColor = MaterialTheme.colors.onSurface,  // Define border colors for better visibility
                    unfocusedBorderColor = MaterialTheme.colors.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth(0.75f)  // Adjust the width for more space
                    .height(50.dp)  // Ensure the height is sufficient
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
    //MaterialTheme {
    EcoStyleTheme {
        MaterialTheme { // Fallback to ensure proper preview rendering
            val sampleProduct = Product(
                name = "Saco uniandes",
                price = "$120 000",
                description = "Saco uniandes talla XL. Me cambié a la nacho, ya no uso el saco",
                imageUrl = "https://via.placeholder.com/150"
            )
            ProductDetailScreen(sampleProduct)
        }
    }
}