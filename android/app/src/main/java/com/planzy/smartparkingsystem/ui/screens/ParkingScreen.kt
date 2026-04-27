package com.planzy.smartparkingsystem.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.planzy.smartparkingsystem.ui.components.*
import com.planzy.smartparkingsystem.viewmodel.ParkingViewModel
import android.os.Looper

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }

    DisposableEffect(locationPermission.status.isGranted) {
        if (!locationPermission.status.isGranted) return@DisposableEffect onDispose {}

        val client   = LocationServices.getFusedLocationProviderClient(context)
        val request  = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { viewModel.onLocationUpdate(it) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {}

        onDispose { client.removeLocationUpdates(callback) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Parking",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    DistanceChip(
                        distanceMeters = uiState.distanceMeters,
                        inRadius = uiState.inRadius,
                    )
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            ParkingMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                free = uiState.parking.free,
                total = uiState.parking.total,
                hasLocation = locationPermission.status.isGranted,
            )

            Surface(
                modifier= Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {

                        SpotsRow(parking = uiState.parking)

                        ParkingStatusRow(parking = uiState.parking)

                        LastUpdateText(timestamp = uiState.parking.lastUpdate)

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        CaptureButton(
                            status   = uiState.captureStatus,
                            onClick  = { viewModel.triggerCapture() },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (!locationPermission.status.isGranted) {
                            PermissionWarningCard(
                                onRequest = { locationPermission.launchPermissionRequest() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkingMap(
    modifier: Modifier,
    free: Int,
    total: Int,
    hasLocation: Boolean,
) {
    val parkingLatLng = LatLng(
        ParkingViewModel.PARKING_LAT,
        ParkingViewModel.PARKING_LON,
    )
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(parkingLatLng, 16f)
    }

    val markerColor = if (free > 0)
        BitmapDescriptorFactory.HUE_GREEN
    else
        BitmapDescriptorFactory.HUE_RED

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = hasLocation),
        uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocation),
    ) {
        Marker(
            state = MarkerState(position = parkingLatLng),
            title = "Parking — $free/$total free",
            icon = BitmapDescriptorFactory.defaultMarker(markerColor),
        )
        Circle(
            center = parkingLatLng,
            radius = ParkingViewModel.TRIGGER_RADIUS_M.toDouble(),
            fillColor = androidx.compose.ui.graphics.Color(0x220000FF),
            strokeColor = androidx.compose.ui.graphics.Color(0x440000FF),
            strokeWidth = 2f,
        )
    }
}


@Composable
private fun PermissionWarningCard(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This app needs to access your location",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRequest) {
                Text("Allow")
            }
        }
    }
}