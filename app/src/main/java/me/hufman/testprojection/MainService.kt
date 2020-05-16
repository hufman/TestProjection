package me.hufman.testprojection

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Display


class MainService: Service() {
	companion object {
		const val TAG = "MainService"
		const val ACTION_START = "me.hufman.testprojection.MainService.start"
		const val ACTION_STOP = "me.hufman.testprojection.MainService.stop"
		const val ACTION_COLOR = "me.hufman.testprojection.MainService.color"

	}

	var projection: MainProjection? = null

	override fun onBind(p0: Intent?): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val action = intent?.action ?: ""
		if (action == ACTION_START) {
			handleActionStart()
		} else if (action == ACTION_STOP) {
			handleActionStop()
		} else if (action == ACTION_COLOR) {
			projection?.changeColor()
		}
		return Service.START_STICKY
	}

	fun handleActionStart() {
		val handler = Handler(Looper.getMainLooper())
		val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
		displayManager.registerDisplayListener(object: DisplayManager.DisplayListener {
			override fun onDisplayChanged(p0: Int) {
				Log.i(TAG, "Display changed: ${displayManager.getDisplay(p0)}")
			}

			override fun onDisplayAdded(p0: Int) {
				Log.i(TAG, "Display added: ${displayManager.getDisplay(p0)}")
			}

			override fun onDisplayRemoved(p0: Int) {
				Log.i(TAG, "Display removed: ${displayManager.getDisplay(p0)}")
			}

		}, handler)
		val virtualDisplayCallback = object: VirtualDisplay.Callback() {
			override fun onResumed() {
				super.onResumed()
				Log.i(TAG, "Virtual display resumed")
			}

			override fun onStopped() {
				super.onStopped()
				Log.i(TAG, "Virtual display stopped")
			}

			override fun onPaused() {
				super.onPaused()
				Log.i(TAG, "Virtual display paused")
			}
		}
		val virtualDisplay = displayManager.createVirtualDisplay("TestMaps", Data.imageCapture.width, Data.imageCapture.height, 100,
				Data.imageCapture.surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY, virtualDisplayCallback, handler)
		Log.i(TAG, "Created virtual display $virtualDisplay")

		val display = virtualDisplay.display
		startPresentation(display)
	}

	fun startPresentation(display: Display) {
		projection = MainProjection(this, display)
		projection?.show()
		val frame = Data.encoder.getFrame()
		Log.i(TAG, "Received frame of ${frame.size} length! ${String(frame)}")
	}

	fun handleActionStop() {

	}
}