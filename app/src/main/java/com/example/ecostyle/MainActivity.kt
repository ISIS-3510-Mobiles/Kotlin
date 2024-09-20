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
                val sampleProducts = listOf(
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                    Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra")
                )
                MainScreen(sampleProducts)
            }
        }
    }
}

data class Product(
    val name: String,
    val price: String,
    val description: String
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
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.gorra),  // Temporary image from drawable
                contentDescription = product.name,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Texts: Product name and price
            Text(text = product.name, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = product.price, style = MaterialTheme.typography.body1)
            //Text(text = product.description, style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun TopBar() {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.primary,
        title = {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Busca en EcoStyle") },
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
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            */
        },
        actions = {
            // Shopping cart icon (commented for now)
            /*
            IconButton(onClick = {  }) {
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
                    .background(MaterialTheme.colors.background)
                    .fillMaxSize()
            ) {
                ProductGridView(products)
            }
        }
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PreviewProductListScreen() {
    EcoStyleTheme {
        MaterialTheme {
            val sampleProducts = listOf(
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra"),
                Product("Uniandes Hat", "$19.99", "Tengo la cabeza muy grande y me quedo pequeña la gorra")
            )
            MainScreen(sampleProducts)
        }
    }
}