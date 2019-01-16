package me.hufman.testprojection

import android.Manifest
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import kotlinx.android.synthetic.main.projection.*

class MainProjection(parentContext: Context, display: Display): Presentation(parentContext, display) {
	val TAG = "TestProjection"
	val colors = ArrayList<Int>()
	var colorIndex = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		colors.add(Color.CYAN)
		colors.add(Color.BLUE)
		colors.add(Color.RED)

		window.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)

		setContentView(R.layout.projection)

	}

	override fun onStart() {
		super.onStart()
		Log.i(TAG, "Projection Start")
	}

	override fun onStop() {
		super.onStop()
		Log.i(TAG, "Projection Stopped")
	}

	fun changeColor() {
		colorIndex = (colorIndex + 1) % colors.size
		textView2?.setBackgroundColor(colors[colorIndex])
	}

}