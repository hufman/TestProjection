package me.hufman.testprojection

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.ImageReader
import me.hufman.carprojection.CarProjectionHost
import me.hufman.carprojection.ProjectionAppInfo

object Data {
	val projectionApps = ArrayList<ProjectionAppInfo>()
	val rect = Rect(0, 0, 720, 480)
	val imageCapture = ImageReader.newInstance(rect.width(), rect.height(), PixelFormat.RGBA_8888, 2)!!
	var bitmap = Bitmap.createBitmap(Data.imageCapture.width, Data.imageCapture.height,  Bitmap.Config.ARGB_8888)!!
	var selectedApp: ProjectionAppInfo? = null
	var carProjectionHost: CarProjectionHost? = null
}