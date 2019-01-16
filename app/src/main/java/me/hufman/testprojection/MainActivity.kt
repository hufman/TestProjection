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

class MainActivity : AppCompatActivity() {
	val TAG = "TestProjection"
	var projection: MainProjection? = null
	val rect = Rect(0, 0, 720, 480)
	val imageCapture = ImageReader.newInstance(rect.width(), rect.height(), PixelFormat.RGBA_8888, 2)

	var bitmap = Bitmap.createBitmap(imageCapture.width, imageCapture.height,  Bitmap.Config.ARGB_8888)
	val PERMISSION_CODE = 934

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

		fab.setOnClickListener { _ ->
			projection?.changeColor()
		}

		val handler = Handler(Looper.getMainLooper())
		imageCapture.setOnImageAvailableListener({
			val image = imageCapture.acquireLatestImage()

			Log.i(TAG, "New frame! ${image}")
			showImage(image)
		}, handler)

		// needed for full screen mirroring
//		startActivityForResult(projectionManager.createScreenCaptureIntent(),
//				PERMISSION_CODE)

		// but instead, let's record a background window
		startBackground()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == PERMISSION_CODE) {
			startRecording(data as Intent)
		}
	}

	fun startRecording(data: Intent) {
		val projectionManager = getSystemService(
				Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
		val projection = projectionManager.getMediaProjection(RESULT_OK, data)
		val virtualDisplay = projection.createVirtualDisplay("TestMaps", imageCapture.width, imageCapture.height, DisplayMetrics.DENSITY_MEDIUM,
				VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY, imageCapture.surface, null, null)
		startPresentation(virtualDisplay.display)
	}

	fun startBackground() {
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
		val virtualDisplay = displayManager.createVirtualDisplay("TestMaps", imageCapture.width, imageCapture.height, 100,
				imageCapture.surface, VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY, virtualDisplayCallback, handler)
		// it is possible to directly link the VirtualDisplay to a surfaceView on the main activity, but
		// that makes it hard to screenshot the VirtualDisplay
//		val virtualDisplay = displayManager.createVirtualDisplay("TestMaps", imageCapture.width, imageCapture.height, 100,
//				surfaceView.holder.surface, VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY, virtualDisplayCallback, handler)
		Log.i(TAG, "Created virtual display $virtualDisplay")

		val display = virtualDisplay.display
		startPresentation(display)
	}

	fun startPresentation(display: Display) {
		projection = MainProjection(this, display)
		projection?.show()
	}

	fun showImage(image: Image?) {
		if (image == null) return
		Log.i(TAG, "New frame! ${image.width}x${image.height}")

		// decide what size to make the destination bitmap
		val planes = image.planes
		val buffer = planes[0].buffer
		val padding = planes[0].rowStride - planes[0].pixelStride * imageCapture.width
		val actualWidth = image.width + padding / planes[0].pixelStride
		if (bitmap.width != actualWidth) {
			Log.i(TAG, "Setting capture bitmap to ${actualWidth}x${imageCapture.height} to hold padding $padding")
			bitmap = Bitmap.createBitmap(actualWidth, imageCapture.height, Bitmap.Config.ARGB_8888)
		}

		// copy the image from the VirtualDisplay surface to the SurfaceView
		Log.i(TAG, "Copying rectangle $rect with padding $padding")
		bitmap.copyPixelsFromBuffer(buffer)
		val outputCanvas = surfaceView.holder.lockCanvas()
		if (outputCanvas != null) {
			Log.i(TAG, "Drawing bitmap")
			outputCanvas.drawBitmap(bitmap, rect, rect, null)
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
