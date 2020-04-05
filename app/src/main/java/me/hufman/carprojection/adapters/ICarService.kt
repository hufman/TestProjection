package me.hufman.carprojection.adapters

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.nhaarman.mockito_kotlin.withSettings
import me.hufman.carprojection.Gearhead
import me.hufman.carprojection.parcelables.CarInfo
import me.hufman.carprojection.parcelables.CarUiInfo
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import java.lang.reflect.Modifier.isAbstract

class ICarService: Service() {
	companion object {
		const val TAG = "FakeCar"
	}

	val carResponses = Answer<Any> { invocation ->
		if (isAbstract(invocation.method.modifiers)) {
			Log.d(TAG, "Called ${invocation?.method}:(${invocation?.arguments?.joinToString(",")})")

			// Figure out what sort of response to give
			val C = {name: String -> Gearhead.getParcelableClass(this, name) }
			when(invocation.method.returnType) {
				C("CarInfo") -> CarInfo.build(this, hideClock = true, hidePhoneSignal = true).transport
				C("CarUiInfo") -> CarUiInfo.build(this, hasRotaryController = true, hasTouchScreen = false).transport
				else -> Answers.RETURNS_DEFAULTS.answer(invocation)
			}
		} else {
			Answers.CALLS_REAL_METHODS.answer(invocation)
		}
	}
	override fun onBind(p0: Intent?): IBinder? {
		val ICarStub = Gearhead.getInterface(this, "ICar\$Stub")
		Log.i(TAG, "Found ICarStub $ICarStub")

		val iCar = Mockito.mock(ICarStub, withSettings().defaultAnswer(carResponses).useConstructor()) as IBinder
		return iCar
	}

}