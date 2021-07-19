package com.dormmom.flutter_mapbox_navigation.models

import android.location.Location
import com.mapbox.navigation.core.trip.session.LocationObserver

class RouteLocationObserver: LocationObserver {

    var location: Location? = null

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        location = enhancedLocation
    }

    override fun onRawLocationChanged(rawLocation: Location) = Unit
}