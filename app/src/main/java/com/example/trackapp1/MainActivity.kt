package com.example.trackapp1

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.trackapp1.ui.theme.TrackApp1Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackApp1Theme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomCard()
                        GetCurrentLocation() //Check above code for this
                    }

                }
            }
        }
    }


    @Composable
    fun BottomCard() {
        // Get screen height
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val offsetY = with(LocalDensity.current) {
            (screenHeight / 2.8f).toPx().toInt()
        } // Convert Dp to pixels

        Box(modifier = Modifier.fillMaxSize()) {
            // Card positioned half from the bottom
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(x = 0, y = offsetY) } // Offset using calculated pixel value
                    .fillMaxSize(), // Adjust card width as needed
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                // Card content
                Text(
                    text = "This is a half-screen rounded card!",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Black
                )
            }
        }
    }


    @Composable
    fun GetCurrentLocation() {
        val context = LocalContext.current
        var address by remember { mutableStateOf("Fetching address...") }
        var coordinates by remember { mutableStateOf("Fetching coordinates...") }

        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                fetchLocationAndAddress(context, fusedLocationClient) { addr, coord ->
                    address = addr ?: "Unable to fetch address"
                    coordinates = coord ?: "Unavailable"
                }
            } else {
                address = "Permission denied"
                coordinates = "Permission denied"
            }
        }

        LaunchedEffect(Unit) {
            when (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            )) {
                PermissionChecker.PERMISSION_GRANTED -> {
                    fetchLocationAndAddress(context, fusedLocationClient) { addr, coord ->
                        address = addr ?: "Unable to fetch address"
                        coordinates = coord ?: "Unavailable"
                    }
                }
                else -> locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Stylish card container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
                modifier = Modifier.padding(8.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Coordinates:\n$coordinates", color = Color.Black)
                        Text("Address:\n$address", color = Color.Black, modifier = Modifier.padding(top = 8.dp))

                        Button(
                            onClick = {
                                fetchLocationAndAddress(context, fusedLocationClient) { addr, coord ->
                                    address = addr ?: "Unable to fetch address"
                                    coordinates = coord ?: "Unavailable"
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Refresh Location")
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun fetchLocationAndAddress(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        onAddressFetched: (String?, String?) -> Unit
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val coord = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                getAddressFromLocation(context, location) { address ->
                    onAddressFetched(address, coord)
                }
            } ?: onAddressFetched("Location not found", null)
        }.addOnFailureListener {
            onAddressFetched("Error fetching location: ${it.message}", null)
        }
    }


    // Function to convert location to address using Geocoder
    private fun getAddressFromLocation(
        context: Context,
        location: Location,
        onAddressFetched: (String?) -> Unit
    ) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val latitude = location.latitude // دوائر العرض
        val longitude = location.longitude // خطوط الطول

        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    onAddressFetched(address)
                } else {
                    onAddressFetched("No address found")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onAddressFetched("Error fetching address: ${e.message}")
        }
    }
}

