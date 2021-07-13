package com.dormmom.flutter_mapbox_navigation.factory

import android.app.Activity
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.annotation.NonNull
import com.dormmom.flutter_mapbox_navigation.FlutterMapboxNavigationPlugin
import com.dormmom.flutter_mapbox_navigation.models.MapBoxEvents
import com.dormmom.flutter_mapbox_navigation.utilities.PluginUtilities
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.NavigationView
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.route.NavigationMapRoute
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import timber.log.Timber
import java.util.*

class FlutterMapViewFactory :
        PlatformView,
        MethodCallHandler,
        Application.ActivityLifecycleCallbacks,
    OnNavigationReadyCallback,
    NavigationListener,
    /*OnNavigationReadyCallback,
    ProgressChangeListener,
    OffRouteListener,
    MilestoneEventListener,
    NavigationEventListener,
    NavigationListener,
    FasterRouteListener,
    SpeechAnnouncementListener,
    RouteListener,
    RefreshCallback, */
    EventChannel.StreamHandler {

    private val activity: Activity
    private val context: Context

    private val methodChannel: MethodChannel
    private val eventChannel: EventChannel
    
    private val options: MapboxMapOptions

    private lateinit var navigationMapBoxMap: NavigationMapboxMap
    private val mapboxNavigation: MapboxNavigation
    private var currentRoute: DirectionsRoute? = null
    private val navigationView: NavigationView
    private val accessToken: String


    private var navigationMapRoute: NavigationMapRoute? = null
    private var mapReady = false

    private var isDisposed = false
    private var isBuildingRoute = false
    private var isNavigationInProgress = false
    private var isNavigationCanceled = false

    //Config
    private var initialLatitude: Double? = null
    private var initialLongitude: Double? = null

    private val wayPoints: MutableList<Point> = mutableListOf()
    private var navigationMode =  DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    private var simulateRoute = false
    private var mapStyleURL: String? = null
    private var navigationLanguage = Locale("en")
    private var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
    private var zoom = 15.0
    private var bearing = 0.0
    private var tilt = 0.0
    private var distanceRemaining: Double? = null
    private var durationRemaining: Double? = null

    private var alternatives = true

    private var allowsUTurnAtWayPoints = false
    private var enableRefresh = false
    private var voiceInstructionsEnabled = true
    private var bannerInstructionsEnabled = true
    private var longPressDestinationEnabled = true
    private var animateBuildRoute = true
    private var isOptimized = false

    private var originPoint: Point? = null
    private var destinationPoint: Point? = null

    private val routeProgressObserver: RouteProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            distanceRemaining = routeProgress.distanceRemaining.toDouble()
            durationRemaining = routeProgress.durationRemaining

            // val progressEvent = MapBoxRouteProgressEvent(routeProgress, location)
            // PluginUtilities.sendEvent(progressEvent)
        }
    }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            // not implemented yet
        }

        override fun onRawLocationChanged(rawLocation: Location) {
            // not implemented yet
        }
    }

    private val offRouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            // not implemented yet
        }
    }

    constructor(cxt: Context, messenger: BinaryMessenger, accessToken: String, viewId: Int, act: Activity, args: Any?)
    {
        context = cxt
        activity = act
        this.accessToken = accessToken
        
        val arguments = args as? Map<*, *>
        if(arguments != null)
            setOptions(arguments)

        methodChannel = MethodChannel(messenger, "flutter_mapbox_navigation/${viewId}")
        eventChannel = EventChannel(messenger, "flutter_mapbox_navigation/${viewId}/events")
        eventChannel.setStreamHandler(this)

        options = MapboxMapOptions.createFromAttributes(context)
                .compassEnabled(false)
                .logoEnabled(true)

        mapboxNavigation = MapboxNavigation(
            MapboxNavigation
                .defaultNavigationOptionsBuilder(context, accessToken)
                .build()
        )

        activity.application.registerActivityLifecycleCallbacks(this)
        methodChannel.setMethodCallHandler(this)
        navigationView = NavigationView(act).apply {
            onCreate(null)
            initialize(this@FlutterMapViewFactory, CameraPosition.Builder().zoom(zoom).build())
            onStart()
            onResume()
        }
    }

    override fun getView(): View = navigationView

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {

            "buildRoute" -> {
                buildRoute(methodCall, result)
            }
            "clearRoute" -> {
                clearRoute(methodCall, result)
            }
            "startNavigation" -> {
                startNavigation(methodCall, result)
            }
            "finishNavigation" -> {
                finishNavigation(methodCall, result)
            }
            "getDistanceRemaining" -> {
                result.success(distanceRemaining)
            }
            "getDurationRemaining" -> {
                result.success(durationRemaining)
            }
            else -> result.notImplemented()
        }
    }

    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        
        isNavigationCanceled = false
        isNavigationInProgress = false
        
        val arguments = methodCall.arguments as? Map<*, *>
        if(arguments != null) {
            setOptions(arguments)
        }

        if (mapReady) {
            wayPoints.clear()
            val points = arguments?.get("wayPoints") as HashMap<*, *>
            for (item in points)
            {
                val point = item.value as HashMap<*, *>
                val latitude = point["Latitude"] as Double
                val longitude = point["Longitude"] as Double
                wayPoints.add(Point.fromLngLat(longitude, latitude))
            } 
            getRoute(context, result)
        } else {
            result.success(false)
        }
    }

    private fun clearRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        if (navigationMapRoute != null)
            navigationMapRoute?.updateRouteArrowVisibilityTo(false)
        
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)
    }

    private fun startNavigation(methodCall: MethodCall, result: MethodChannel.Result) {

        val arguments = methodCall.arguments as? Map<*, *>
        if(arguments != null) {
            setOptions(arguments)
        }

        startNavigation()

        if (currentRoute != null) {
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun finishNavigation(methodCall: MethodCall, result: MethodChannel.Result) {

        finishNavigation()

        if (currentRoute != null) {
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun startNavigation() {
        isNavigationCanceled = false

        currentRoute?.let {
            isNavigationInProgress = true
            val optionsBuilder = NavigationViewOptions.builder(context)
            optionsBuilder.shouldSimulateRoute(simulateRoute)
            optionsBuilder.navigationListener(this)
            optionsBuilder.routeProgressObserver(routeProgressObserver)
            optionsBuilder.locationObserver(locationObserver)
            optionsBuilder.directionsRoute(it)
            navigationView.startNavigation(optionsBuilder.build())
            PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
        }
    }

    private fun finishNavigation(isOffRouted: Boolean = false) {

        zoom = 15.0
        bearing = 0.0
        tilt = 0.0
        isNavigationCanceled = true

        if (!isOffRouted) {
            isNavigationInProgress = false
        }

        if (currentRoute != null) {
            navigationView.stopNavigation()
        }

    }

    private fun setOptions(arguments: Map<*, *>) {
        val navMode = arguments["mode"] as? String
        if(navMode != null)
        {
            if(navMode == "walking")
                navigationMode = DirectionsCriteria.PROFILE_WALKING;
            else if(navMode == "cycling")
                navigationMode = DirectionsCriteria.PROFILE_CYCLING;
            else if(navMode == "driving")
                navigationMode = DirectionsCriteria.PROFILE_DRIVING;
        }

        val simulated = arguments["simulateRoute"] as? Boolean
        if (simulated != null) {
            simulateRoute = simulated
        }

        val language = arguments["language"] as? String
        if(language != null) {
            navigationLanguage = Locale(language)
        }

        val units = arguments["units"] as? String

        if(units != null)
        {
            if(units == "imperial")
                navigationVoiceUnits = DirectionsCriteria.IMPERIAL
            else if(units == "metric")
                navigationVoiceUnits = DirectionsCriteria.METRIC
        }

        mapStyleURL = arguments["mapStyleURL"] as? String

        initialLatitude = arguments["initialLatitude"] as? Double
        initialLongitude = arguments["initialLongitude"] as? Double
        
        val zm = arguments["zoom"] as? Double
        if(zm != null) {
            zoom = zm
        }

        val br = arguments["bearing"] as? Double
        if(br != null) {
            bearing = br
        }

        val tt = arguments["tilt"] as? Double
        if(tt != null) {
            tilt = tt
        }

        val optim = arguments["isOptimized"] as? Boolean
        if(optim != null) {
            isOptimized = optim
        }

        val anim = arguments["animateBuildRoute"] as? Boolean
        if(anim != null) {
            animateBuildRoute = anim
        }

        val altRoute = arguments["alternatives"] as? Boolean
        if(altRoute != null) {
            alternatives = altRoute
        }

        val voiceEnabled = arguments["voiceInstructionsEnabled"] as? Boolean
        if(voiceEnabled != null) {
            voiceInstructionsEnabled = voiceEnabled
        }

        val bannerEnabled = arguments["bannerInstructionsEnabled"] as? Boolean
        if(bannerEnabled != null) {
            bannerInstructionsEnabled = bannerEnabled
        }

        val longPress = arguments["longPressDestinationEnabled"] as? Boolean
        if(longPress != null) {
            longPressDestinationEnabled = longPress
        }
    }


    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning) {
            this.mapReady = true
            navigationMapBoxMap = navigationView.retrieveNavigationMapboxMap()!!
            PluginUtilities.sendEvent(MapBoxEvents.MAP_READY)
        }
    }

    private fun getRoute(context: Context, result: MethodChannel.Result? = null) {

        if (!PluginUtilities.isNetworkAvailable(context)) {
            PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, "No Internet Connection")
            return
        }

        PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILDING)

        originPoint = Point.fromLngLat(wayPoints[0].longitude(), wayPoints[0].latitude())
        destinationPoint = Point.fromLngLat(wayPoints[1].longitude(), wayPoints[1].latitude())

        val routeRequestCallback = object : RoutesRequestCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>) {
                if (routes.isEmpty()) {
                    PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, "No routes found")
                } else {
                    currentRoute = routes[0]
                    PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILT)
                    startNavigation()
                    result?.success(true)
                }
            }

            override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                isBuildingRoute = false
                result?.success(false)
                PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, "Cancelled route request.")
            }

            override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                isBuildingRoute = false
                result?.success(false)
                PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, "${throwable.message}")
            }
        }

        mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultParams()
                .accessToken(accessToken)
                .coordinates(listOf(originPoint, destinationPoint))
                .build(),
            routeRequestCallback
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        navigationView.onCreate(savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity) {

        try {
            navigationView.onStart()
        } catch (e: Exception) {
            Timber.d(String.format("onActivityStarted, %s", "Error: ${e.message}"))
        }
    }

    override fun onActivityResumed(activity: Activity) {
        navigationView.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        navigationView.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        navigationView.onStop()
    }

    override fun onActivitySaveInstanceState(@NonNull p0: Activity, @NonNull outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
    }

    override fun onActivityDestroyed(activity: Activity) {
        navigationView.onDestroy()
    }


    override fun onCancelNavigation() {
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)

        navigationView.stopNavigation()
    }

    override fun onNavigationFinished() {
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_FINISHED)
    }

    override fun onNavigationRunning() {
        if (!isNavigationCanceled) {
            PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
        }
    }

    override fun dispose() {
        isDisposed = true
        mapReady = false
        navigationView.onStop()
        navigationView.onDestroy()
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        FlutterMapboxNavigationPlugin.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        FlutterMapboxNavigationPlugin.eventSink = null
    }
}