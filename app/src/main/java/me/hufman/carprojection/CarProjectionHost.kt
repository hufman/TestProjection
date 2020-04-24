package me.hufman.carprojection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.content.res.Configuration.*
import android.graphics.Rect
import android.media.ImageReader
import android.os.IBinder
import android.util.Log
import me.hufman.carprojection.adapters.CarProjection
import me.hufman.carprojection.adapters.ProxyInputConnection
import me.hufman.carprojection.parcelables.CarWindowManagerLayoutParams
import me.hufman.carprojection.parcelables.DrawingSpec
import me.hufman.carprojection.parcelables.InputFocusChangedEvent
import me.hufman.testprojection.Data

class CarProjectionHost(val context: Context, val appInfo: ProjectionAppInfo,
                        val imageCapture: ImageReader, val car: IBinder, val callbacks: IBinder): ServiceConnection {
	companion object {
		val TAG = "CarProjection"
	}

	var projection: CarProjection? = null
	var inputConnection: ProxyInputConnection? = null

	override fun onServiceDisconnected(p0: ComponentName?) {
		Log.i(TAG, "Disconnected from projection app ${appInfo.className}")
		projection = null
	}

	override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
		p1 ?: return
		try {
			val projection = CarProjection.getInstance(context, p1)
			this.projection = projection
			projection.onSetup(car, callbacks)

			val intent = Intent(Intent.ACTION_MAIN)
					.setComponent(ComponentName("com.waze", "com.waze.android_auto.a1"))
					.putExtra("assistant_activity", true)
			val configuration = Configuration()
			configuration.setToDefaults()
			configuration.orientation = ORIENTATION_LANDSCAPE
			configuration.uiMode = UI_MODE_TYPE_CAR
			configuration.touchscreen = TOUCHSCREEN_NOTOUCH
			configuration.screenWidthDp = imageCapture.width
			configuration.screenHeightDp = imageCapture.height
			val displayRect = Rect(100, 60, Data.imageCapture.width, Data.imageCapture.height)
			val drawingSpec = DrawingSpec.build(context, imageCapture.width, imageCapture.height, 125, imageCapture.surface, displayRect)

			projection.onConfigChanged(0, drawingSpec, configuration)
			projection.onProjectionStart(drawingSpec, intent, null)
			projection.onProjectionResume(0)
			projection.onWindowAttributesChanged(CarWindowManagerLayoutParams.build(context, 15))

			val inputFocus = InputFocusChangedEvent.build(context, true, false, 0, displayRect)
			projection.onInputFocusChange(inputFocus)
		} catch (e: Exception) {
			Log.e(TAG, "Error starting projection", e)
		}
	}

}