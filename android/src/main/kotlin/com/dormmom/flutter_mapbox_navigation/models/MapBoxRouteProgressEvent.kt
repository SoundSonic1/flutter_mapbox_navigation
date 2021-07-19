package com.dormmom.flutter_mapbox_navigation.models

import android.location.Location
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState

class MapBoxRouteProgressEvent(progress: RouteProgress, location: Location) {
    val arrived: Boolean = progress.currentState == RouteProgressState.ROUTE_COMPLETE
    val distance: Double = progress.distanceRemaining.toDouble()
    val duration: Double = progress.durationRemaining
    val distanceTraveled: Double = progress.distanceTraveled.toDouble()
    val currentLegDistanceTraveled: Double? = progress.currentLegProgress?.distanceTraveled?.toDouble()
    val currentLegDistanceRemaining: Double? = progress.currentLegProgress?.distanceRemaining?.toDouble()
    val currentStepInstruction: String? = progress.bannerInstructions?.primary()?.text()
    val legIndex: Int? = progress.currentLegProgress?.legIndex
    val stepIndex: Int? = null
    val currentLeg: MapBoxRouteLeg? = null
    val priorLeg: MapBoxRouteLeg? = null
    val remainingLegs: List<MapBoxRouteLeg> = listOf()
}
