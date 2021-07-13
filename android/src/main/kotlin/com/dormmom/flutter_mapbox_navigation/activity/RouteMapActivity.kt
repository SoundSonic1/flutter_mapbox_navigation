package com.dormmom.flutter_mapbox_navigation.activity

import android.app.Activity


class RouteMapActivity : Activity() {
    /*
    val CREATED = 1
    val STARTED = 2
    val RESUMED = 3
    val PAUSED = 4
    val STOPPED = 5
    val DESTROYED = 6

    var mapView: MapView? = null
    var startRouteButton: Button? = null

    private var mapboxMap: MapboxMap? = null

    private var locationEngine: LocationEngine? = null
    private var navigation: MapboxNavigation? = null
    lateinit var routes : List<DirectionsRoute>
    private lateinit var route: DirectionsRoute

    var _distanceRemaining: Double? = null
    var _durationRemaining: Double? = null
    
    private var navigationMapRoute: NavigationMapRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var routeRefresh: RouteRefresh? = null
    private var isRefreshing = false
    val TWENTY_FIVE_METERS: Double = 25.0

    private val state: AtomicInteger = AtomicInteger(0)
    private val registrarActivityHashCode = 0

    private class MyBroadcastReceiver internal constructor(navigation: MapboxNavigation?) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<MapboxNavigation?>?
        override fun onReceive(context: Context?, intent: Intent?) {
            val navigation: MapboxNavigation? = weakNavigation?.get()
            navigation!!.stopNavigation()
        }

        init {
            weakNavigation = WeakReference(navigation)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat_NoActionBar);
        setContentView(R.layout.activity_routes_map)
        mapView = findViewById<MapView>(R.id.mapView)
        startRouteButton = findViewById<Button>(R.id.startRouteButton)
        startRouteButton?.setOnClickListener{
            val isValidNavigation = navigation != null
            var isValidRoute = route != null && this.route!!.distance()!! > TWENTY_FIVE_METERS
            if(isValidNavigation && isValidRoute)
            {
                startRouteButton!!.visibility = View.INVISIBLE
                navigation?.addNavigationEventListener(this)
                navigation?.addProgressChangeListener(this)
                navigation?.addMilestoneEventListener(this)
                navigation?.addOffRouteListener(this)
                
                (locationEngine as ReplayRouteLocationEngine).assign(route)
                  navigation?.setLocationEngine(locationEngine!!)
                  mapboxMap?.getLocationComponent()?.setLocationComponentEnabled(true)
                  navigation?.startNavigation(route!!)
                  mapboxMap?.removeOnMapClickListener(this)
            }
        }

        //routeRefresh = RouteRefresh(Mapbox.getAccessToken(), applicationContext)

        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

        val context = applicationContext;
        val customNotification = CustomNavigationNotification(this.applicationContext);
        val options = MapboxNavigationOptions.Builder()
                                              .navigationNotification(customNotification)
                                              .build();

        var accessToken = PluginUtilities.getResourceFromContext(this, "mapbox_access_token")
        navigation = MapboxNavigation(
            this,
            accessToken,
            options
            )

    }


    private fun newOrigin() {
        if (mapboxMap != null) {
            val latLng: LatLng = PluginUtilities.getRandomLatLng(doubleArrayOf(-77.1825, 38.7825, -76.9790, 39.0157))
            (locationEngine as ReplayRouteLocationEngine).assignLastLocation(
                    Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())
            );
            mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0));
        }
    }

    private fun calculateRoute()
    {
        locationEngine?.getLastLocation(object : LocationEngineCallback<LocationEngineResult>{
            override fun onSuccess(result: LocationEngineResult?) {
                findRouteWith(result)
            }

            override fun onFailure(exception: Exception) {

            }

        });
    }
    
    private fun findRouteWith(result: LocationEngineResult?){

        var accessToken = PluginUtilities.getResourceFromContext(this, "mapbox_access_token")
        var userLocation: Location? = result?.lastLocation ?: return
        val origin = Point.fromLngLat(userLocation?.longitude!!, userLocation?.latitude!!)
        if(TurfMeasurement.distance(origin, destination!!, TurfConstants.UNIT_METERS) < 50)
        {
            startRouteButton?.visibility = View.GONE
        }
        
        val navigationRouteBuilder: NavigationRoute.Builder = NavigationRoute.builder(this).accessToken(accessToken)
        navigationRouteBuilder.origin(origin)
        navigationRouteBuilder.destination(destination!!)
        if(waypoint != null)
        {
            navigationRouteBuilder.addWaypoint(waypoint!!)
        }
        navigationRouteBuilder.enableRefresh(true)
        navigationRouteBuilder.build().getRoute(object: Callback<DirectionsResponse>{

            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() != null) {
                    if (!response.body()!!.routes().isEmpty()) {
                        // Route fetched from NavigationRoute
                        routes = response.body()!!.routes()
                        route = routes.get(0)
                        navigationMapRoute?.addRoutes(routes);
                        startRouteButton?.setVisibility(View.VISIBLE);
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {

            }
        })
    }
    
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap?.addOnMapClickListener(this)
        mapboxMap.setStyle(Style.MAPBOX_STREETS){
            val locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(this, it)
            locationComponent.renderMode = RenderMode.GPS
            locationComponent.isLocationComponentEnabled = false
            navigationMapRoute = NavigationMapRoute(navigation, mapView!!, mapboxMap);

            locationEngine = ReplayRouteLocationEngine()
            newOrigin()
        }

    }

    override fun onMapClick(point: LatLng): Boolean {
        if (destination == null) {
          destination = Point.fromLngLat(point.getLongitude(), point.getLatitude())
            mapboxMap?.addMarker(MarkerOptions().position(point))
        } else if (waypoint == null) {
          waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude())
            mapboxMap?.addMarker(MarkerOptions().position(point))
        } else {
          Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
        }

        calculateRoute()
        return false
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        mapboxMap?.locationComponent?.forceLocationUpdate(location)
        if(!isRefreshing)
        {
            isRefreshing = true
            //routeRefresh?.refresh(routeProgress, this)
        }

        var currentState = routeProgress?.currentState()
        _distanceRemaining =  routeProgress?.distanceRemaining();
        _durationRemaining = routeProgress?.durationRemaining();
    }

    override fun onRunning(running: Boolean) {
        if(running)
        {

        }
        else
        {

        }
    }

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
        print("Voice instruction: " + instruction)
    }

    override fun userOffRoute(location: Location) {
        Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show();
    }

    override fun onRefresh(directionsRoute: DirectionsRoute) {
        directionsRoute?.let { navigation?.startNavigation(it) }
        isRefreshing = false;
    }

    override fun onError(error: RefreshError) {
        isRefreshing = false
    }

    /*
    * Activity Life Cycles
    */

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        state.set(CREATED)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        mapView?.onStart()
        state.set(STARTED)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        mapView?.onResume()
        state.set(RESUMED)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        mapView?.onPause()
        state.set(PAUSED)

    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        mapView?.onStop()
        state.set(STOPPED)
    }

    override fun onActivitySaveInstanceState(@NonNull p0: Activity, @NonNull outState: Bundle) {
        mapView?.onSaveInstanceState(outState);
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity.hashCode() != registrarActivityHashCode) {
            return
        }
        activity.application.unregisterActivityLifecycleCallbacks(this)
        navigation?.onDestroy();
        if (mapboxMap != null) {
            mapboxMap?.removeOnMapClickListener(this);
        }
        mapView?.onDestroy();
        state.set(DESTROYED)
    }

     */
}