package com.dormmom.flutter_mapbox_navigation.activity

/*class NavigationActivity : AppCompatActivity(),
        OnNavigationReadyCallback,
        ProgressChangeListener,
        OffRouteListener,
        MilestoneEventListener,
        NavigationEventListener,
        NavigationListener,
        FasterRouteListener,
        SpeechAnnouncementListener,
        BannerInstructionsListener,
        RouteListener {
    var receiver: BroadcastReceiver? = null
    private var navigationView: NavigationView? = null
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private var dropoffDialogShown = false
    private var lastKnownLocation: Location? = null
    
    private val route by lazy { intent.getSerializableExtra("route") as? DirectionsRoute }
    private var points: MutableList<Point> = mutableListOf()

    private var currentDestination: Point? = null;

    override fun onCreate(savedInstanceState: Bundle?) {

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
                NavigationLauncher.cleanUpPreferences(applicationContext)
            }
        }
        registerReceiver(receiver, IntentFilter(NavigationLauncher.KEY_STOP_NAVIGATION))
        
        super.onCreate(savedInstanceState)
        
        setTheme(R.style.Theme_AppCompat_NoActionBar)

        var accessToken = PluginUtilities.getResourceFromContext(this.applicationContext, "mapbox_access_token")
        Mapbox.getInstance(this.applicationContext, accessToken)

        setContentView(R.layout.activity_navigation)
        
        var p = intent.getSerializableExtra("waypoints") as? MutableList<Point>
        if(p != null)
        {
            points = p
        }

        navigationView = findViewById(R.id.navigationView)

        navigationView?.onCreate(savedInstanceState)
        navigationView?.initialize(
                this,
                getInitialCameraPosition()
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView?.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        navigationView?.onPause()
    }

    override fun onDestroy() {
        navigationView?.onDestroy()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView?.onBackPressed()!!) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navigationView?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {

        if (isRunning && ::navigationMapboxMap.isInitialized) {
            return
        }

        if(points.count() > 0)
        {
            fetchRoute(points.removeAt(0), points.removeAt(0))
        }

    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        lastKnownLocation = location
        val progressEvent = MapBoxRouteProgressEvent(routeProgress, location)
        FlutterMapboxNavigationPlugin.distanceRemaining = routeProgress.distanceRemaining()
        FlutterMapboxNavigationPlugin.durationRemaining = routeProgress.durationRemaining()
        sendEvent(progressEvent)
    }

    override fun userOffRoute(location: Location) {

        sendEvent(MapBoxEvents.USER_OFF_ROUTE,
                MapBoxLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                ).toString())
    }

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {

        sendEvent(MapBoxEvents.MILESTONE_EVENT,
                MapBoxMileStone(
                        identifier = milestone.identifier,
                        distanceTraveled = routeProgress.distanceTraveled(),
                        legIndex = routeProgress.legIndex,
                        stepIndex = routeProgress.stepIndex
                ).toString())
    }

    override fun onRunning(running: Boolean) {

        sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
    }

    override fun onCancelNavigation() {
        sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)
        navigationView?.stopNavigation()
        FlutterMapboxNavigationPlugin.eventSink = null
        NavigationLauncher.stopNavigation(this)
        
    }

    override fun onNavigationFinished() {
        sendEvent(MapBoxEvents.NAVIGATION_FINISHED)
        navigationView?.stopNavigation()
        FlutterMapboxNavigationPlugin.eventSink = null
        NavigationLauncher.stopNavigation(this)

    }

    override fun onNavigationRunning() {
        sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
    }

    override fun fasterRouteFound(directionsRoute: DirectionsRoute) {
        sendEvent(MapBoxEvents.FASTER_ROUTE_FOUND, directionsRoute.toJson())
    }

    override fun willVoice(announcement: SpeechAnnouncement?): SpeechAnnouncement? {
        sendEvent(MapBoxEvents.SPEECH_ANNOUNCEMENT,
                "${announcement?.announcement()}")
        return announcement
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        sendEvent(MapBoxEvents.BANNER_INSTRUCTION,
                "${instructions?.primary()?.text()}")
        return instructions
    }

    override fun onArrival() {
        PluginUtilities.sendEvent(MapBoxEvents.ON_ARRIVAL)
        if (points.isNotEmpty()) {
            fetchRoute(getLastKnownLocation(), points.removeAt(0))
            dropoffDialogShown = true // Accounts for multiple arrival events
            // Toast.makeText(this, "You have arrived!", Toast.LENGTH_SHORT).show()

        }
        else
        {
            FlutterMapboxNavigationPlugin.eventSink = null
        }
       
     
        // onNavigationFinished()

    }

    override fun onFailedReroute(errorMessage: String?) {
        sendEvent(MapBoxEvents.FAILED_TO_REROUTE,"${errorMessage}")
    }

    override fun onOffRoute(offRoutePoint: Point?) {
        sendEvent(MapBoxEvents.USER_OFF_ROUTE,
                MapBoxLocation(
                        latitude = offRoutePoint?.latitude(),
                        longitude = offRoutePoint?.longitude()
                ).toString())
        if(offRoutePoint != null)
            fetchRoute(offRoutePoint, getCurrentDestination());
        else
            fetchRoute(getLastKnownLocation(), getCurrentDestination());
    }

    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
        sendEvent(MapBoxEvents.REROUTE_ALONG, "${directionsRoute?.toJson()}")
    }

    private fun buildAndStartNavigation(directionsRoute: DirectionsRoute) {

        dropoffDialogShown = false

        navigationView?.retrieveNavigationMapboxMap()?.let {navigationMap ->

            if(FlutterMapboxNavigationPlugin.mapStyleUrlDay != null)
                navigationMap.retrieveMap().setStyle(Style.Builder().fromUri(FlutterMapboxNavigationPlugin.mapStyleUrlDay as String))

            if(FlutterMapboxNavigationPlugin.mapStyleUrlNight != null)
                navigationMap.retrieveMap().setStyle(Style.Builder().fromUri(FlutterMapboxNavigationPlugin.mapStyleUrlNight as String))


            this.navigationMapboxMap = navigationMap
            this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
            navigationView?.retrieveMapboxNavigation()?.let {
                this.mapboxNavigation = it

                mapboxNavigation.addOffRouteListener(this)
                mapboxNavigation.addFasterRouteListener(this)
                mapboxNavigation.addNavigationEventListener(this)
            }
            
            // Custom map style has been loaded and map is now ready
            val options =
                    NavigationViewOptions.builder()
                            .progressChangeListener(this)
                            .milestoneEventListener(this)
                            .navigationListener(this)
                            .speechAnnouncementListener(this)
                            .bannerInstructionsListener(this)
                            .routeListener(this)
                            .directionsRoute(directionsRoute)
                            .shouldSimulateRoute(FlutterMapboxNavigationPlugin.simulateRoute)
                            .build()


            navigationView?.startNavigation(options)

        }
        //navigationView!!.startNavigation(navigationViewOptions)
    }
    
    private fun showDropoffDialog() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setMessage(getString(R.string.dropoff_dialog_text))
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dropoff_dialog_positive_text)
        ) { dialogInterface: DialogInterface?, `in`: Int -> fetchRoute(getLastKnownLocation(), points.removeAt(0)) }
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dropoff_dialog_negative_text)
        ) { dialogInterface: DialogInterface?, `in`: Int -> }
        alertDialog.show()
    }

    private fun fetchRoute(origin: Point, destination: Point) {

        val accessToken = Mapbox.getAccessToken()
        if (accessToken == null) {
            Toast.makeText(this, "Access Token is Required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentDestination = destination
        NavigationRoute.builder(this)
                .accessToken(accessToken)
                .origin(origin)
                .destination(destination)
                .alternatives(true)
                .profile(FlutterMapboxNavigationPlugin.navigationMode)
                .language(FlutterMapboxNavigationPlugin.navigationLanguage)
                .voiceUnits(FlutterMapboxNavigationPlugin.navigationVoiceUnits)
                .continueStraight(!FlutterMapboxNavigationPlugin.allowsUTurnsAtWayPoints)
                .annotations(DirectionsCriteria.ANNOTATION_DISTANCE, DirectionsCriteria.ANNOTATION_DURATION, DirectionsCriteria.ANNOTATION_DURATION, DirectionsCriteria.ANNOTATION_CONGESTION)
                .build()
                .getRoute(object : SimplifiedCallback() {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        val directionsResponse = response.body()
                        if (directionsResponse != null) {
                            if (!directionsResponse.routes().isEmpty()) buildAndStartNavigation(directionsResponse.routes()[0]) else {
                                val message = directionsResponse.message()
                                sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, message!!)
                                finish()
                            }
                        }
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                        sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, throwable.localizedMessage)
                        finish()
                    }
                })
    }

    private fun getLastKnownLocation(): Point {
        return Point.fromLngLat(lastKnownLocation?.longitude!!, lastKnownLocation?.latitude!!)
    }
    private fun getCurrentDestination(): Point {
        return Point.fromLngLat(currentDestination?.longitude()!!, currentDestination?.latitude()!!)
    }

    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {
        return true
    }

    private fun getInitialCameraPosition(): CameraPosition {
        if(route == null)
            return CameraPosition.DEFAULT;

        val originCoordinate = route?.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
                .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
                .zoom(FlutterMapboxNavigationPlugin.zoom)
                .bearing(FlutterMapboxNavigationPlugin.bearing)
                .tilt(FlutterMapboxNavigationPlugin.tilt)
                .build()
    }

}
*/