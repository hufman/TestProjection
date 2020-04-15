package me.hufman.carprojection.adapters.impl

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import me.hufman.carprojection.Gearhead
import me.hufman.carprojection.adapters.CarProjection
import me.hufman.carprojection.parcelables.CarWindowManagerLayoutParams
import me.hufman.carprojection.parcelables.DrawingSpec
import me.hufman.carprojection.parcelables.InputFocusChangedEvent
import java.lang.reflect.Method

class MagicCarProjection(context: Context, transport: IBinder) : CarProjection(context, transport) {
	private val onSetupFunction = proxy::class.java.methods.first {
		it.parameterTypes.size == 2 &&
		it.parameterTypes[0] == Gearhead.getInterface(context, "ICarProjectionCallback") &&
		it.parameterTypes[1] == Gearhead.getInterface(context, "ICar")
	}

	private val onWindowAttributesChanged = proxy::class.java.methods.first {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == Gearhead.getInterface(context, "CarWindowManagerLayoutParams")
	}

	private val setIntent = proxy::class.java.methods.first {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == Intent::class.java
	}

	private val onConfigChanged = proxy::class.java.methods.first {
		it.parameterTypes.size == 3 &&
		it.parameterTypes[1] == Gearhead.getInterface(context, "DrawingSpec") &&
		it.parameterTypes[2] == Configuration::class.java
	}

	val onStart = proxy::class.java.methods.first {
		it.parameterTypes.size == 3 &&
			it.parameterTypes[0] == Gearhead.getInterface(context, "DrawingSpec") &&
			it.parameterTypes[1] == Intent::class.java &&
			it.parameterTypes[2] == Bundle::class.java
	}

	val onProjectionResume = proxy::class.java.methods.first {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == Int::class.javaPrimitiveType
	}

	val onInputFocusChange = proxy::class.java.methods.first {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == Gearhead.getInterface(context, "InputFocusChangedEvent")
	}

	val onKeyEvent = proxy::class.java.methods.first {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == KeyEvent::class.java
	}

	val onProjectionPause = proxy::class.java.methods.filter {
		it.parameterTypes.size == 1 &&
				it.parameterTypes[0] == Integer::class.javaPrimitiveType
	}[1]

	val onProjectionStop = proxy::class.java.methods.filter {
		it.parameterTypes.size == 1 &&
		it.parameterTypes[0] == Integer::class.javaPrimitiveType
	}[2]

	val disconnectSurface = proxy::class.java.methods.filter {
		it.parameterTypes.size == 0
	}[3]
	val onFinish = proxy::class.java.methods.filter {
		it.parameterTypes.size == 0
	}[4]

	fun Method.loggedCall(vararg args: Any?) {
		val log = "Calling ${this}(${args.joinToString(",")})"
		Log.d("CarProjection", log
				.replace("java.lang.", "")
				.replace("java.util.", "")
				.replace("com.google.android.gms.car.", "")
				.replace(" throws android.os.RemoteException", ""))
		this.invoke(proxy, *args)
	}

	override fun onSetup(iCar: IBinder, iCarProjectionCallback: IBinder) {
		onSetupFunction.loggedCall(iCarProjectionCallback, iCar)
//		onSetupFunction.invoke(proxy, iCarProjectionCallback, iCar)
	}

	override fun onNewIntent(intent: Intent) {
		onSetupFunction.loggedCall(intent)
//		setIntent.invoke(proxy, intent)
	}

	override fun onConfigChanged(displayId: Int, drawingSpec: DrawingSpec, config: Configuration) {
		onConfigChanged.loggedCall(displayId, drawingSpec.transport, config)
//		onConfigChanged.invoke(proxy, displayId, drawingSpec.transport, config)
	}

	override fun onProjectionStart(drawingSpec: DrawingSpec, intent: Intent, bundle: Bundle?) {
		onStart.loggedCall(drawingSpec.transport, intent, bundle)
//		onStart.invoke(proxy, drawingSpec.transport, intent, bundle)
	}

	override fun onProjectionResume(displayId: Int) {
		onProjectionResume.loggedCall(displayId)
//		onProjectionResume.invoke(proxy, displayId)
	}

	override fun onInputFocusChange(inputFocusChangedEvent: InputFocusChangedEvent) {
		onInputFocusChange.loggedCall(inputFocusChangedEvent.transport)
//		onInputFocusChange.invoke(proxy, inputFocusChangedEvent.transport)
	}

	override fun onWindowAttributesChanged(windowAttributes: CarWindowManagerLayoutParams) {
		onWindowAttributesChanged.loggedCall(windowAttributes.transport)
	}

	override fun onKeyEvent(event: KeyEvent) {
		onKeyEvent.loggedCall(event)
//		onKeyEvent.invoke(proxy, event)
	}

	override fun onProjectionPause(displayId: Int) {
		onProjectionPause.loggedCall(displayId)
//		onProjectionPause.invoke(proxy, displayId)
	}

	override fun onProjectionStop(displayId: Int) {
		onProjectionStop.loggedCall(displayId)
		disconnectSurface.loggedCall()
		onFinish.loggedCall()
//		onProjectionStop.invoke(proxy, displayId)
	}
}