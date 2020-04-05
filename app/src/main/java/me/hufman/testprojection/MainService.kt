package me.hufman.testprojection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.*
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.view.KeyEvent
import me.hufman.carprojection.AppDiscovery
import me.hufman.carprojection.CarProjectionHost
import me.hufman.carprojection.ProjectionAppInfo
import me.hufman.carprojection.adapters.ICarService
import me.hufman.carprojection.adapters.ICarProjectionCallbackService
import java.lang.Exception


class MainService: Service() {
	companion object {
		const val TAG = "MainService"
		const val CHANNEL_ID = "ForegroundService"
		const val ACTION_START = "me.hufman.testprojection.MainService.start"
		const val ACTION_STOP = "me.hufman.testprojection.MainService.stop"
		const val ACTION_BUTTON = "me.hufman.testprojection.MainService.color"
		const val ACTION_UP = "me.hufman.testprojection.MainService.up"
		const val ACTION_DOWN = "me.hufman.testprojection.MainService.down"

	}

	var iCar: IBinder? = null
	var iCarProjectionCallback: IBinder? = null

	override fun onBind(p0: Intent?): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startNotification()
		System.setProperty("org.mockito.android.target", this.getDir("mockito", Context.MODE_PRIVATE).absolutePath)
		val action = intent?.action ?: ""
		when (action) {
			ACTION_START -> handleActionStart()
			ACTION_STOP -> handleActionStop()
			ACTION_BUTTON -> {
				sendInput(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
				sendInput(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
			}
			ACTION_UP -> {
				val time = SystemClock.uptimeMillis()
				sendInput(KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON))
				sendInput(KeyEvent(time, time+10, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0, KeyEvent.META_SHIFT_ON))
			}
			ACTION_DOWN -> {
				sendInput(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
				sendInput(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
			}
		}
		return START_STICKY
	}

	fun startNotification() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val serviceChannel = NotificationChannel(
					CHANNEL_ID,
					"Foreground Service Channel",
					NotificationManager.IMPORTANCE_DEFAULT
			)
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(serviceChannel)
		}

		val intent = Intent(this, MainActivity::class.java)
		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("TestProjection")
				.setContentText("TestProjection")
				.setSmallIcon(R.drawable.ic_launcher_foreground)
				.build()
		startForeground(1, notification)
	}

	fun handleActionStart() {
		startCarService()
		startCarCallbackService()
	}

	fun startVirtualDisplay() {
		val handler = Handler(Looper.getMainLooper())
		val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
		displayManager.registerDisplayListener(object : DisplayManager.DisplayListener {
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
		val virtualDisplayCallback = object : VirtualDisplay.Callback() {
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
	}

	fun startCarService() {
		val intent = Intent(this, ICarService::class.java)
		bindService(intent, object: ServiceConnection {
			override fun onServiceDisconnected(p0: ComponentName?) {
				Log.i(TAG, "Disconnected from fake car")
				iCar = null
			}

			override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
				Log.i(TAG, "Connected to fake car $p1")
				iCar = p1
				tryProjectionApp()
			}

		}, Context.BIND_AUTO_CREATE)
	}

	fun startCarCallbackService() {
		val intent = Intent(this, ICarProjectionCallbackService::class.java)
		bindService(intent, object: ServiceConnection {
			override fun onServiceDisconnected(p0: ComponentName?) {
				Log.i(TAG, "Disconnected from fake car callback")
				iCarProjectionCallback = null
			}

			override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
				Log.i(TAG, "Connected to fake car callback $p1")
				iCarProjectionCallback = p1
				tryProjectionApp()
			}
		}, Context.BIND_AUTO_CREATE)
	}

	fun tryProjectionApp() {
		val selectedApp = Data.selectedApp
		val iCar = iCar
		val iCarProjectionCallback = iCarProjectionCallback
		if (selectedApp != null && iCar != null && iCarProjectionCallback != null) {
			connectToProjectionApp(selectedApp, Data.imageCapture, iCar, iCarProjectionCallback)
		}
	}

	fun connectToProjectionApp(appInfo: ProjectionAppInfo, imageCapture: ImageReader, car: IBinder, callbacks: IBinder) {
		val discovery = AppDiscovery(this)
		if (appInfo != Data.carProjectionHost?.appInfo ) {
			if (Data.carProjectionHost != null) {
				handleActionStop()
				Handler().postDelayed({connectToProjectionApp(appInfo, imageCapture, car, callbacks)}, 1000)
			} else {
				val host = CarProjectionHost(this, appInfo, imageCapture, car, callbacks)
				Data.carProjectionHost = host
				discovery.connectApp(appInfo, host)
			}
		}
	}

	fun sendInput(event: KeyEvent) {
		val carProjection = Data.carProjectionHost?.projection ?: return
		carProjection.onKeyEvent(event)
	}

	fun handleActionStop() {
		val carProjection = Data.carProjectionHost?.projection ?: return
		try {
			carProjection.onProjectionStop(0)
		} catch (e: Exception) {}
		try {
			val carProjectionHost = Data.carProjectionHost
			carProjectionHost?.apply { unbindService(this) }
		} catch (e: Exception) {}
		Data.carProjectionHost = null
	}

	override fun onDestroy() {
		handleActionStop()
		super.onDestroy()
	}
}