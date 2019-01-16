package me.hufman.testprojection

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.google.android.gms.maps.GoogleMap
import kotlinx.android.synthetic.main.projection.*

class MainProjection(parentContext: Context, display: Display): Presentation(parentContext, display) {
	val TAG = "TestProjection"
	var map: GoogleMap? = null
	val colors = ArrayList<Int>()
	var colorIndex = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val r = context.resources
		colors.add(Color.CYAN)
		colors.add(Color.BLUE)
		colors.add(Color.RED)

		window.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
//
		setContentView(R.layout.projection)

		mapViewProjection.onCreate(savedInstanceState)
		mapViewProjection.getMapAsync {
			Log.i(TAG, "Got map!")
			map = it
			it.isIndoorEnabled = false
			it.isTrafficEnabled = true

		}

	}

	override fun onStart() {
		super.onStart()
		Log.i(TAG, "Projection Start")
		mapViewProjection.onStart()
		mapViewProjection.onResume()
	}

	override fun onStop() {
		super.onStop()
		Log.i(TAG, "Projection Stopped")
		mapViewProjection.onPause()
		mapViewProjection.onStop()
		mapViewProjection.onDestroy()
	}

	override fun onSaveInstanceState(): Bundle {
		val output = super.onSaveInstanceState()
		mapViewProjection.onSaveInstanceState(output)
		return output
	}

	fun changeColor() {
		colorIndex = (colorIndex + 1) % colors.size
		textView2?.setBackgroundColor(colors[colorIndex])
	}

}