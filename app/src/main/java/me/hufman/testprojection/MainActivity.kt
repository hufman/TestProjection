package me.hufman.testprojection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import me.hufman.carprojection.AppDiscovery
import me.hufman.carprojection.ProjectionAppInfo
import me.hufman.testprojection.MainService.Companion.ACTION_BUTTON
import me.hufman.testprojection.MainService.Companion.ACTION_DOWN
import me.hufman.testprojection.MainService.Companion.ACTION_START
import me.hufman.testprojection.MainService.Companion.ACTION_STOP
import me.hufman.testprojection.MainService.Companion.ACTION_UP
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {
	companion object {
		val TAG = "TestProjection"

		const val ACTION_KEYBOARD = "me.hufman.testprojection.MainActivity.keyboard"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

		fab.setOnClickListener { _ ->
			startService(Intent(this, MainService::class.java).setAction(ACTION_BUTTON))
		}
		fabUp.setOnClickListener {
			startService(Intent(this, MainService::class.java).setAction(ACTION_UP))
		}
		fabDown.setOnClickListener {
			startService(Intent(this, MainService::class.java).setAction(ACTION_DOWN))
		}

		// set up the callback for new images
		val handler = Handler(Looper.getMainLooper())
		Data.imageCapture.setOnImageAvailableListener({
			val image = it.acquireLatestImage()

//			Log.i(TAG, "New frame! $image")
			showImage(image)
		}, handler)

		discoverProjectionApps()
		Data.projectionApps.addAll(AppDiscovery(this).discoverApps().sortedBy { it.name })
		Log.i(TAG, Data.projectionApps.toString())
		lstApps.adapter = ProjectionAppListItem(this, Data.projectionApps)
		lstApps.setOnItemClickListener { adapterView, view, i, l ->
			val appInfo = adapterView.adapter.getItem(i) as? ProjectionAppInfo
			if (appInfo != null) {
				Data.selectedApp = appInfo
				startService()
			}
		}
	}

	override fun onResume() {
		super.onResume()

		// start the projection in the background
		startService()

		registerReceiver(broadcastReceiver, IntentFilter(ACTION_KEYBOARD))
	}

	fun discoverProjectionApps() {
		AppDiscovery(this).discoverApps().forEach {
			Log.i(TAG, "Found projection app ${it.packageName}")
		}
	}

	fun startService() {
		try {
			ContextCompat.startForegroundService(this, Intent(this, MainService::class.java).setAction(ACTION_START))
		} catch (e: RuntimeException) {
			Log.w(TAG, "Couldn't start service: $e")
		}
	}

	fun showImage(image: Image?) {
		if (image == null) return
//		Log.i(TAG, "New frame! ${image.width}x${image.height}")

		// decide what size to make the destination bitmap
		val planes = image.planes
		val buffer = planes[0].buffer
		val padding = planes[0].rowStride - planes[0].pixelStride * Data.imageCapture.width
		val actualWidth = image.width + padding / planes[0].pixelStride
		if (Data.bitmap.width != actualWidth) {
//			Log.i(TAG, "Setting capture bitmap to ${actualWidth}x${Data.imageCapture.height} to hold padding $padding")
			Data.bitmap = Bitmap.createBitmap(actualWidth, Data.imageCapture.height, Bitmap.Config.ARGB_8888)
		}

		// copy the image from the VirtualDisplay surface to the SurfaceView
//		Log.i(TAG, "Copying rectangle ${Data.rect} with padding $padding")
		Data.bitmap.copyPixelsFromBuffer(buffer)
		// resize to a smaller
		val smallerRect = Rect(0, 0, Data.rect.right/2, Data.rect.bottom/2)
		val smallerRectF = RectF(0f, 0f, Data.rect.right/2.0f, Data.rect.bottom/2.0f)
		val smallerBitmap = Bitmap.createBitmap(Data.rect.right/2, Data.rect.bottom/2, Bitmap.Config.ARGB_8888)
//		val smallerBitmap = Bitmap.createBitmap(Data.rect.right/2, Data.rect.bottom/2, Bitmap.Config.RGB_565)
		val smallCanvas = Canvas(smallerBitmap)
		val smallMatrix = Matrix()
		smallMatrix.reset()
		smallMatrix.postScale(0.5f, 0.5f)
		smallCanvas.drawBitmap(Data.bitmap, Data.rect, smallerRectF, null)
		// output to surface
		val outputCanvas = surfaceView.holder.lockCanvas()
		if (outputCanvas != null) {
//			Log.i(TAG, "Drawing bitmap")
			outputCanvas.drawBitmap(smallerBitmap, smallerRect, Data.rect, null)
//			Log.i(TAG, "Posting canvas")
			surfaceView.holder.unlockCanvasAndPost(outputCanvas)
//			Log.i(TAG, "Finished frame")
		}
		image.close()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}

	val broadcastReceiver = object: BroadcastReceiver() {
		override fun onReceive(p0: Context?, p1: Intent?) {
			if (p1?.action == ACTION_KEYBOARD) {
				getSystemService(InputMethodManager::class.java)?.showSoftInput(this@MainActivity.findViewById(R.id.activityMain), InputMethodManager.SHOW_FORCED)
			}
		}

	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		event ?: return false
//		return Data.carProjectionHost?.inputConnection?.sendKeyEvent(event) ?: false
		return if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
			true
		} else if (event.keyCode == KeyEvent.KEYCODE_DEL) {
			Data.carProjectionHost?.inputConnection?.deleteSurroundingText(1, 0)
		} else {
			val data = event.unicodeChar.toChar()
			Data.carProjectionHost?.inputConnection?.commitText(data.toString())
		} ?: false
	}

	override fun onPause() {
		super.onPause()

		startService(Intent(this, MainService::class.java).setAction(ACTION_STOP))
		unregisterReceiver(broadcastReceiver)
	}
}
