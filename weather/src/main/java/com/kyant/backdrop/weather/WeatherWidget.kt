package com.kyant.backdrop.weather

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget placed
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        // Set click intent to open main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

        // Get actual widget size in dp from AppWidgetManager, convert to px
        val widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val density = context.resources.displayMetrics.density
        val widthPx: Int
        val heightPx: Int
        if (widgetInfo != null && widgetInfo.minWidth > 0 && widgetInfo.minHeight > 0) {
            widthPx = (widgetInfo.minWidth * density).toInt()
            heightPx = (widgetInfo.minHeight * density).toInt()
        } else {
            val defW = (250 * density).toInt()
            val defH = (180 * density).toInt()
            widthPx = defW
            heightPx = defH
        }

        // Render the glass widget bitmap asynchronously using goAsync()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = GlassWidgetRenderer.render(context, widthPx, heightPx)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_image, bitmap)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * Trigger widget update from anywhere (e.g., after weather data refresh).
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, WeatherWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, WeatherWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
