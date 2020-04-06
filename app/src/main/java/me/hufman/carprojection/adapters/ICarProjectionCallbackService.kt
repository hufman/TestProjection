package me.hufman.carprojection.adapters

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.util.Log
import me.hufman.carprojection.Gearhead
import me.hufman.testprojection.Data
import me.hufman.testprojection.MainActivity
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import java.lang.reflect.Modifier.isAbstract

class ICarProjectionCallbackService: Service() {
	companion object {
		const val TAG = "CarCallback"
	}

	val callbackResponses = Answer<Any> { invocation ->
		if (invocation.method != null && isAbstract(invocation.method.modifiers)) {
			// expand parameter types for prettier printing
			val stringArguments = invocation.arguments.mapIndexed { index, argument ->
				when (argument) {
					is Bundle -> {
						try {
							argument.classLoader = Gearhead.getClassLoader(this)
							"Bundle(" + argument.keySet().map { "$it -> ${argument.get(it)}" }.joinToString(",") + ")"
						} catch (e: Exception) { argument.toString() }
					}
					else -> argument?.toString()
				}
			}
			val log = "Called ${invocation.method}(${stringArguments.joinToString(",")})"
			Log.d(TAG, log
					.replace("java.lang.", "")
					.replace("java.util.", "")
					.replace("com.google.android.gms.car.", "")
					.replace(" throws android.os.RemoteException", ""))

			if (invocation.method.parameterTypes.getOrNull(0) == Gearhead.getInterface(this, "input.IProxyInputConnection")) {
				Data.carProjectionHost?.inputConnection = ProxyInputConnection.getInstance(this, (invocation.arguments[0] as IInterface).asBinder())
				this.sendBroadcast(Intent(MainActivity.ACTION_KEYBOARD))
				Answers.RETURNS_DEFAULTS.answer(invocation)
//				Log.i(TAG, "Returning text for input")
//				Data.carProjectionHost?.inputConnection?.commitText("Peets")
			} else {
				Answers.RETURNS_DEFAULTS.answer(invocation)
			}
		} else {
			Answers.CALLS_REAL_METHODS.answer(invocation)
		}
	}

	override fun onBind(p0: Intent?): IBinder? {
		val ICarProjectionCallbackStub = Gearhead.getInterface(this, "ICarProjectionCallback\$Stub")
		val iCarProjectionCallback = Mockito.mock(ICarProjectionCallbackStub, Mockito.withSettings().defaultAnswer(callbackResponses).useConstructor()) as IBinder
		return iCarProjectionCallback
	}
}