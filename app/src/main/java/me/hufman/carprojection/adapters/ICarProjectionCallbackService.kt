package me.hufman.carprojection.adapters

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import me.hufman.carprojection.Gearhead
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import java.lang.reflect.Modifier.isAbstract

class ICarProjectionCallbackService: Service() {
	companion object {
		const val TAG = "CarCallback"
	}

	val callbackResponses = Answer<Any> { invocation ->
		if (isAbstract(invocation.method.modifiers)) {
			Log.d(TAG, "Called ${invocation?.method}:(${invocation?.arguments?.joinToString(",")})")
			Answers.RETURNS_DEFAULTS.answer(invocation)
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