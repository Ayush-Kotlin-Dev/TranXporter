package com.ayush.tranxporter.user.presentation.location

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ayush.tranxporter.R
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline

@Composable
fun MapContent(
    cameraPositionState: CameraPositionState,
    pickupLocation: LatLng?,
    dropOffLocation: LatLng?,
    routePoints: List<LatLng>,
    onMapClick: (LatLng) -> Unit,
    onPickupMarkerClick: () -> Unit,
    onDropoffMarkerClick: () -> Unit
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(myLocationButtonEnabled = false),
        onMapClick = onMapClick
    ) {
        pickupLocation?.let {
            Marker(
                state = MarkerState(position = it),
                title = "Pickup Location",
                snippet = "Tap to change",
                onInfoWindowClick = { onPickupMarkerClick() }
            )
        }
        dropOffLocation?.let {
            Marker(
                state = MarkerState(position = it),
                title = "Drop-off Location",
                snippet = "Tap to change",
                onInfoWindowClick = { onDropoffMarkerClick() }
            )
        }
        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 4f,
                pattern = listOf(Dot(), Gap(10f))
            )
        }
    }
}
@Composable
fun LocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.location),
            contentDescription = "My Location",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}