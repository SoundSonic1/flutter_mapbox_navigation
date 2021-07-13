package com.dormmom.flutter_mapbox_navigation

/*class CustomNavigationNotification(applicationContext: Context) : NavigationNotification
{
    private val CUSTOM_NOTIFICATION_ID = 845745152
    private val STOP_NAVIGATION_ACTION = "stop_navigation_action"
    private val CUSTOM_CHANNEL_ID = "custom_channel_id"
    private val CUSTOM_CHANNEL_NAME = "custom_channel_name"

    private var customNotification: Notification
    private var customNotificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private val stopNavigationReceiver: BroadcastReceiver? = null
    private var numberOfUpdates = 0

    init {
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                    CUSTOM_CHANNEL_ID, CUSTOM_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
        customNotificationBuilder = NotificationCompat.Builder(applicationContext, CUSTOM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_navigation)
                .setContentTitle("Custom Navigation Notification")
                .setContentText("Display your own content here!")
                .setContentIntent(createPendingStopIntent(applicationContext))
        customNotification = customNotificationBuilder?.build()!!
    }

    override fun onNavigationStopped(context: Context): kotlin.Unit {
        context?.unregisterReceiver(stopNavigationReceiver);
        notificationManager?.cancel(CUSTOM_NOTIFICATION_ID);
    }

    override fun getNotificationId(): Int {
        return CUSTOM_NOTIFICATION_ID;
    }

    override fun getNotification(): Notification {
        return customNotification;
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        customNotificationBuilder?.setContentText("Number of updates: " + numberOfUpdates++);
        notificationManager?.notify(CUSTOM_NOTIFICATION_ID, customNotificationBuilder?.build());
    }

    private fun createPendingStopIntent(context: Context): PendingIntent? {
        val stopNavigationIntent = Intent(STOP_NAVIGATION_ACTION)
        return PendingIntent.getBroadcast(context, 0, stopNavigationIntent, 0)
    }

}

 */