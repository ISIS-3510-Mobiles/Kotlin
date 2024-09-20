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
            //    MyFirstComposeApp()
                val sampleProducts = listOf(
                    Product("Uniandes Sweater", "$19.99", "https://example.com/tshirt.jpg"),
                    Product("Reusable Water Bottle", "$9.99", "https://example.com/bottle.jpg"),
                    Product("Organic Cotton Bag", "$14.99", "https://example.com/bag.jpg")
                )
                MainScreen(sampleProducts)
            }
        }
    }
}
/*
@Composable
fun MyFirstComposeApp() {

}
// Preview function to see how the composable looks in Android Studio Preview window
@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PreviewMyFirstComposeApp() {
    MaterialTheme {
        MyFirstComposeApp()
    }
}
*/

data class Product(
    val name: String,
    val price: String,
    val imageUrl: String
)

//Display List of Products
@Composable
fun ProductListView(products: List<Product>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ProductCard(product)
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductGridView(products: List<Product>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ProductCard(product)
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,  // Use surface color for card background
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            // Placeholder for the product image (later we’ll load it from Firebase)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Texts: Product name and price
            Text(text = product.name, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.price, style = MaterialTheme.typography.body1)
        }
    }
}


@Composable
fun SampleProductListScreen() {
    val sampleProducts = listOf(
        Product("Eco-friendly T-shirt", "$19.99", "https://example.com/tshirt.jpg"),
        Product("Reusable Water Bottle", "$9.99", "https://example.com/bottle.jpg"),
        Product("Organic Cotton Bag", "$14.99", "https://example.com/bag.jpg")
    )

    ProductListView(products = sampleProducts)
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

@Composable
fun MainScreen(products: List<Product>) {
    Scaffold(
        topBar = { TopBar() },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .background(MaterialTheme.colors.background)  // Apply the background color here
                    .fillMaxSize()  // Make sure the background covers the whole screen
            ) {
                ProductGridView(products)
            }
        }
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PreviewProductListScreen() {
    //MaterialTheme {
    EcoStyleTheme {
        MaterialTheme { // Fallback to ensure proper preview rendering
            val sampleProducts = listOf(
                Product("Uniandes Sweater", "$19.99", "https://example.com/tshirt.jpg"),
                Product("Reusable Water Bottle", "$9.99", "https://example.com/bottle.jpg"),
                Product("Organic Cotton Bag", "$14.99", "https://example.com/bag.jpg"),
                Product("Eco-friendly T-shirt", "$19.99", "https://example.com/tshirt.jpg"),
                Product("Reusable Water Bottle", "$9.99", "https://example.com/bottle.jpg"),
                Product("Organic Cotton Bag", "$14.99", "https://example.com/bag.jpg")
            )
            MainScreen(sampleProducts)
        }
    }
}