package me.hufman.testprojection

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import me.hufman.testprojection.MainService.Companion.ACTION_BUTTON
import me.hufman.testprojection.MainService.Companion.ACTION_DOWN
import me.hufman.testprojection.MainService.Companion.ACTION_START
import me.hufman.testprojection.MainService.Companion.ACTION_STOP
import me.hufman.testprojection.MainService.Companion.ACTION_UP
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {
	val TAG = "TestProjection"

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
	}

	override fun onResume() {
		super.onResume()

		// start the projection in the background
		ContextCompat.startForegroundService(this, Intent(this, MainService::class.java).setAction(ACTION_START))
	}

	fun discoverProjectionApps() {

		val filter = Intent("android.intent.action.MAIN")
		filter.addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION")
//		filter.addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION_NAVIGATION")
//		{
//			addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION")
//			addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION_NAVIGATION")
//			addCategory("android.intent.action.MEDIA_BUTTON")
//		}


		val services = packageManager.queryIntentServices(filter, PackageManager.GET_RESOLVED_FILTER)
		services.forEach {
			Log.i(TAG, "Found projection app ${it.serviceInfo?.applicationInfo?.packageName}")
		}
	}

	fun startBackground() {
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
		val smallerBitmap = Bitmap.createBitmap(Data.rect.right/2, Data.rect.bottom/2, Bitmap.Config.RGB_565)
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

	override fun onPause() {
		super.onPause()

		startService(Intent(this, MainService::class.java).setAction(ACTION_STOP))
	}
}
