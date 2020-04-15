package me.hufman.carprojection.parcelables

import android.content.Context
import android.os.Parcelable
import me.hufman.carprojection.Gearhead

class CarWindowManagerLayoutParams(val transport: Parcelable) {
	companion object {
		/**
		 * Flags are a bitmask of which ProjectedPresentation trampolines are focusable
		 * 1,2,4,8
		 */
		fun build(context: Context, flags: Int): CarWindowManagerLayoutParams {
			return CarWindowManagerLayoutParams(Gearhead.createParcelable(context, "CarWindowManagerLayoutParams", flags))
		}
	}
}