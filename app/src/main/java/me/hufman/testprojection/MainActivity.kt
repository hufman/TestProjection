package me.hufman.testprojection

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import me.hufman.testprojection.MainService.Companion.ACTION_COLOR
import me.hufman.testprojection.MainService.Companion.ACTION_START

class MainActivity : AppCompatActivity() {
	val TAG = "TestProjection"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

		fab.setOnClickListener { _ ->
			startService(Intent(this, MainService::class.java).setAction(ACTION_COLOR))
		}

		// set up the callback for new images
		val handler = Handler(Looper.getMainLooper())
		Data.imageCapture.setOnImageAvailableListener({
			val image = it.acquireLatestImage()

			Log.i(TAG, "New frame! $image")
			showImage(image)
		}, handler)

		// start the projection in the background
		startBackground()
	}

	fun startBackground() {
		startService(Intent(this, MainService::class.java).setAction(ACTION_START))
	}

	fun showImage(image: Image?) {
		if (image == null) return
		Log.i(TAG, "New frame! ${image.width}x${image.height}")

		// decide what size to make the destination bitmap
		val planes = image.planes
		val buffer = planes[0].buffer
		val padding = planes[0].rowStride - planes[0].pixelStride * Data.imageCapture.width
		val actualWidth = image.width + padding / planes[0].pixelStride
		if (Data.bitmap.width != actualWidth) {
			Log.i(TAG, "Setting capture bitmap to ${actualWidth}x${Data.imageCapture.height} to hold padding $padding")
			Data.bitmap = Bitmap.createBitmap(actualWidth, Data.imageCapture.height, Bitmap.Config.ARGB_8888)
		}

		// copy the image from the VirtualDisplay surface to the SurfaceView
		Log.i(TAG, "Copying rectangle ${Data.rect} with padding $padding")
		Data.bitmap.copyPixelsFromBuffer(buffer)
		val outputCanvas = surfaceView.holder.lockCanvas()
		if (outputCanvas != null) {
			Log.i(TAG, "Drawing bitmap")
			outputCanvas.drawBitmap(Data.bitmap, Data.rect, Data.rect, null)
			Log.i(TAG, "Posting canvas")
			surfaceView.holder.unlockCanvasAndPost(outputCanvas)
			Log.i(TAG, "Finished frame")
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
}
